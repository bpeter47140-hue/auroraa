package main

import (
	"archive/tar"
	"bytes"
	"context"
	"crypto/subtle"
	"encoding/binary"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

type config struct{ token, dataPath, image string }
type buildRequest struct {
	WorkspaceID int64 `json:"workspaceId"`
	ProjectID   int64 `json:"projectId"`
}
type buildResponse struct {
	Status  string    `json:"status"`
	Logs    string    `json:"logs"`
	BuiltAt time.Time `json:"builtAt"`
}
type dockerClient struct{ client *http.Client }

func newDocker() *dockerClient {
	tr := &http.Transport{DialContext: func(ctx context.Context, _, _ string) (net.Conn, error) {
		return (&net.Dialer{Timeout: 5 * time.Second}).DialContext(ctx, "unix", "/var/run/docker.sock")
	}}
	return &dockerClient{&http.Client{Transport: tr, Timeout: 8 * time.Minute}}
}
func (d *dockerClient) request(method, path string, body io.Reader, contentType string) (*http.Response, error) {
	req, e := http.NewRequest(method, "http://docker"+path, body)
	if e != nil {
		return nil, e
	}
	if contentType != "" {
		req.Header.Set("Content-Type", contentType)
	}
	return d.client.Do(req)
}
func (d *dockerClient) json(method, path string, v any) ([]byte, int, error) {
	var body io.Reader
	if v != nil {
		b, e := json.Marshal(v)
		if e != nil {
			return nil, 0, e
		}
		body = bytes.NewReader(b)
	}
	ct := ""
	if body != nil {
		ct = "application/json"
	}
	r, e := d.request(method, path, body, ct)
	if e != nil {
		return nil, 0, e
	}
	defer r.Body.Close()
	b, _ := io.ReadAll(io.LimitReader(r.Body, 2<<20))
	return b, r.StatusCode, nil
}
func (d *dockerClient) ok(method, path string, v any) error {
	b, status, e := d.json(method, path, v)
	if e != nil {
		return e
	}
	if status < 200 || status >= 300 {
		return fmt.Errorf("Docker API %s %s returned %d: %s", method, path, status, string(b))
	}
	return nil
}
func (d *dockerClient) archive(container string, body []byte) error {
	r, e := d.request("PUT", "/containers/"+container+"/archive?path=%2Fworkspace", bytes.NewReader(body), "application/x-tar")
	if e != nil {
		return e
	}
	defer r.Body.Close()
	if r.StatusCode < 200 || r.StatusCode >= 300 {
		b, _ := io.ReadAll(r.Body)
		return fmt.Errorf("upload source archive returned %d: %s", r.StatusCode, string(b))
	}
	return nil
}
func (d *dockerClient) exec(container, command string) (string, error) {
	payload := map[string]any{"AttachStdout": true, "AttachStderr": true, "Tty": false, "User": "10001:10001", "Cmd": []string{"/bin/sh", "-lc", command}}
	b, status, e := d.json("POST", "/containers/"+container+"/exec", payload)
	if e != nil {
		return "", e
	}
	if status != 201 {
		return "", fmt.Errorf("create exec returned %d: %s", status, string(b))
	}
	var result struct {
		ID string `json:"Id"`
	}
	if e = json.Unmarshal(b, &result); e != nil {
		return "", e
	}
	r, e := d.request("POST", "/exec/"+result.ID+"/start", bytes.NewReader([]byte(`{"Detach":false,"Tty":false}`)), "application/json")
	if e != nil {
		return "", e
	}
	stream, e := io.ReadAll(io.LimitReader(r.Body, 2<<20))
	r.Body.Close()
	if e != nil {
		return "", e
	}
	if r.StatusCode < 200 || r.StatusCode >= 300 {
		return string(stream), fmt.Errorf("exec start returned %d", r.StatusCode)
	}
	logs := decodeDockerStream(stream)
	var inspect struct {
		ExitCode int  `json:"ExitCode"`
		Running  bool `json:"Running"`
	}
	for i := 0; i < 300; i++ {
		raw, st, er := d.json("GET", "/exec/"+result.ID+"/json", nil)
		if er != nil {
			return logs, er
		}
		if st != 200 {
			return logs, fmt.Errorf("inspect exec returned %d", st)
		}
		if er = json.Unmarshal(raw, &inspect); er != nil {
			return logs, er
		}
		if !inspect.Running {
			break
		}
		time.Sleep(time.Second)
	}
	if inspect.Running {
		return logs, errors.New("build timed out")
	}
	if inspect.ExitCode != 0 {
		return logs, fmt.Errorf("website build exited with code %d", inspect.ExitCode)
	}
	return logs, nil
}
func decodeDockerStream(b []byte) string {
	var out strings.Builder
	for len(b) >= 8 {
		n := int(binary.BigEndian.Uint32(b[4:8]))
		if n < 0 || n > len(b)-8 {
			break
		}
		out.Write(b[8 : 8+n])
		b = b[8+n:]
	}
	if out.Len() == 0 {
		return string(b)
	}
	return out.String()
}
func (d *dockerClient) getArchive(container, path string) ([]byte, error) {
	r, e := d.request("GET", "/containers/"+container+"/archive?path="+path, nil, "")
	if e != nil {
		return nil, e
	}
	defer r.Body.Close()
	if r.StatusCode != 200 {
		b, _ := io.ReadAll(r.Body)
		return nil, fmt.Errorf("retrieve build output returned %d: %s", r.StatusCode, string(b))
	}
	return io.ReadAll(io.LimitReader(r.Body, 32<<20))
}
func safeJoin(root, rel string) (string, error) {
	clean := filepath.Clean(rel)
	if clean == "." || filepath.IsAbs(clean) || clean == ".." || strings.HasPrefix(clean, ".."+string(os.PathSeparator)) {
		return "", errors.New("unsafe path")
	}
	p := filepath.Join(root, clean)
	if !strings.HasPrefix(p, root+string(os.PathSeparator)) {
		return "", errors.New("unsafe path")
	}
	return p, nil
}
func unpackPreview(data []byte, dest string) error {
	tmp := dest + ".tmp"
	_ = os.RemoveAll(tmp)
	if e := os.MkdirAll(tmp, 0750); e != nil {
		return e
	}
	tr := tar.NewReader(bytes.NewReader(data))
	count := 0
	total := int64(0)
	prefix := ""
	for {
		h, e := tr.Next()
		if e == io.EOF {
			break
		}
		if e != nil {
			return e
		}
		count++
		if count > 1500 {
			return errors.New("preview contains too many files")
		}
		name := strings.TrimPrefix(h.Name, "./")
		if prefix == "" {
			if name == "dist" || name == "dist/" {
				prefix = "dist/"
				continue
			}
			if strings.HasPrefix(name, "dist/") {
				prefix = "dist/"
			}
		}
		if prefix != "" {
			name = strings.TrimPrefix(name, prefix)
		}
		if name == "" {
			continue
		}
		if h.Typeflag != tar.TypeReg && h.Typeflag != tar.TypeDir {
			continue
		}
		p, e := safeJoin(tmp, name)
		if e != nil {
			return e
		}
		if h.Typeflag == tar.TypeDir {
			if e = os.MkdirAll(p, 0750); e != nil {
				return e
			}
			continue
		}
		if h.Size < 0 || h.Size > 10<<20 {
			return errors.New("preview file exceeds size limit")
		}
		total += h.Size
		if total > 30<<20 {
			return errors.New("preview exceeds size limit")
		}
		if e = os.MkdirAll(filepath.Dir(p), 0750); e != nil {
			return e
		}
		f, e := os.OpenFile(p, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0640)
		if e != nil {
			return e
		}
		_, e = io.CopyN(f, tr, h.Size)
		f.Close()
		if e != nil {
			return e
		}
	}
	if _, e := os.Stat(filepath.Join(tmp, "index.html")); e != nil {
		return errors.New("build output has no index.html")
	}
	_ = os.RemoveAll(dest)
	return os.Rename(tmp, dest)
}
func projectWorkspace(dataPath string, workspaceID, projectID int64) (string, error) {
	if workspaceID < 1 || projectID < 1 {
		return "", errors.New("invalid workspace or project identifier")
	}
	base, err := filepath.Abs(dataPath)
	if err != nil {
		return "", err
	}
	base, err = filepath.EvalSymlinks(base)
	if err != nil {
		return "", errors.New("project data directory is unavailable")
	}
	workspace := filepath.Join(base, fmt.Sprintf("ws-%d", workspaceID), fmt.Sprintf("project-%d", projectID))
	rel, err := filepath.Rel(base, workspace)
	if err != nil || rel == "." || strings.HasPrefix(rel, ".."+string(os.PathSeparator)) || rel == ".." {
		return "", errors.New("project workspace escaped data directory")
	}
	current := base
	for _, part := range strings.Split(rel, string(os.PathSeparator)) {
		current = filepath.Join(current, part)
		info, statErr := os.Lstat(current)
		if statErr != nil {
			return "", errors.New("project workspace is not ready")
		}
		if info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
			return "", errors.New("project workspace contains an unsafe path")
		}
	}
	resolved, err := filepath.EvalSymlinks(workspace)
	if err != nil || resolved != workspace {
		return "", errors.New("project workspace path is unsafe")
	}
	return workspace, nil
}

func validatePackage(workspace string) error {
	raw, err := os.ReadFile(filepath.Join(workspace, "package.json"))
	if err != nil || len(raw) > 64<<10 {
		return errors.New("project package manifest is missing or too large")
	}
	var manifest struct {
		Scripts map[string]string `json:"scripts"`
	}
	if err = json.Unmarshal(raw, &manifest); err != nil {
		return errors.New("project package.json is invalid")
	}
	build := manifest.Scripts["build"]
	if build != "vite build" && build != "tsc -b && vite build" {
		return errors.New("build script is not in the approved React/Vite build allowlist")
	}
	if manifest.Scripts["prebuild"] != "" || manifest.Scripts["postbuild"] != "" {
		return errors.New("custom prebuild/postbuild scripts are not allowed")
	}
	return nil
}

func (d *dockerClient) build(c config, req buildRequest) (buildResponse, error) {
	resp := buildResponse{Status: "FAILED", BuiltAt: time.Now().UTC()}
	workspace, e := projectWorkspace(c.dataPath, req.WorkspaceID, req.ProjectID)
	if e != nil {
		return resp, e
	}
	info, e := os.Lstat(filepath.Join(workspace, "package.json"))
	if e != nil || !info.Mode().IsRegular() {
		return resp, errors.New("project workspace is not ready")
	}
	if e = validatePackage(workspace); e != nil {
		return resp, e
	}
	var archive bytes.Buffer
	tw := tar.NewWriter(&archive)
	files := 0
	total := int64(0)
	e = filepath.WalkDir(workspace, func(path string, entry os.DirEntry, walkErr error) error {
		if walkErr != nil {
			return walkErr
		}
		if path == workspace {
			return nil
		}
		rel, er := filepath.Rel(workspace, path)
		if er != nil {
			return er
		}
		if filepath.ToSlash(rel) == "vite.config.ts" {
			return nil
		} // Always use the trusted builder config.
		parts := strings.Split(filepath.ToSlash(rel), "/")
		for _, part := range parts {
			if part == "node_modules" || part == "dist" || part == "build" || part == "preview" || part == "preview.tmp" || part == ".git" || strings.HasPrefix(part, ".") || strings.HasSuffix(part, ".aurora-tmp") {
				if entry.IsDir() {
					return filepath.SkipDir
				}
				return nil
			}
		}
		if entry.Type()&os.ModeSymlink != 0 {
			return errors.New("symbolic links are not allowed")
		}
		if entry.IsDir() {
			return nil
		}
		if !entry.Type().IsRegular() {
			return nil
		}
		fi, er := entry.Info()
		if er != nil {
			return er
		}
		files++
		total += fi.Size()
		if files > 100 || total > 5<<20 {
			return errors.New("project source exceeds build input limits")
		}
		f, er := os.Open(path)
		if er != nil {
			return er
		}
		h := &tar.Header{Name: filepath.ToSlash(rel), Mode: 0640, Size: fi.Size(), Typeflag: tar.TypeReg, Uid: 10001, Gid: 10001}
		er = tw.WriteHeader(h)
		if er == nil {
			_, er = io.Copy(tw, f)
		}
		f.Close()
		return er
	})
	if e != nil {
		return resp, e
	}
	if e = tw.Close(); e != nil {
		return resp, e
	}
	name := fmt.Sprintf("aurora-preview-%d-%d-%d", req.WorkspaceID, req.ProjectID, time.Now().UnixNano())
	body := map[string]any{
		"Image": c.image, "User": "10001:10001", "WorkingDir": "/workspace",
		"Env": []string{"HOME=/tmp", "npm_config_cache=/tmp/npm-cache", "CI=true"},
		"Cmd": []string{"/bin/sh", "-lc", "sleep 600"},
		"HostConfig": map[string]any{
			"NetworkMode": "none", "ReadonlyRootfs": false, // Disposable container; no host or workspace bind mounts.
			"Memory": 536870912, "MemorySwap": 536870912, "NanoCpus": 1000000000,
			"PidsLimit": 128, "CapDrop": []string{"ALL"},
			"StorageOpt":  map[string]string{"size": "128m"},
			"SecurityOpt": []string{"no-new-privileges:true"},
			"Tmpfs": map[string]string{
				"/tmp": "rw,nosuid,nodev,size=192m,uid=10001,gid=10001,mode=1777",
			},
		},
	}
	b, status, e := d.json("POST", "/containers/create?name="+name, body)
	if e != nil {
		return resp, e
	}
	if status != 201 {
		return resp, fmt.Errorf("create isolated build container returned %d: %s", status, string(b))
	}
	var created struct {
		ID string `json:"Id"`
	}
	if e = json.Unmarshal(b, &created); e != nil {
		return resp, e
	}
	defer func() { _, _, _ = d.json("DELETE", "/containers/"+created.ID+"?force=true&v=true", nil) }()
	if e = d.ok("POST", "/containers/"+created.ID+"/start", nil); e != nil {
		return resp, e
	}
	if e = d.archive(created.ID, archive.Bytes()); e != nil {
		return resp, e
	}
	logs, buildErr := d.exec(created.ID, "ln -s /opt/deps/node_modules /workspace/node_modules && cd /workspace && npm run build -- --config /opt/deps/preview-vite.config.ts --base=./")
	resp.Logs = logs
	if buildErr != nil {
		return resp, fmt.Errorf("%w: %s", buildErr, logs)
	}
	out, e := d.getArchive(created.ID, "/workspace/dist")
	if e != nil {
		return resp, e
	}
	preview := filepath.Join(workspace, "preview")
	if e = unpackPreview(out, preview); e != nil {
		return resp, e
	}
	resp.Status = "SUCCEEDED"
	return resp, nil
}
func main() {
	c := config{token: os.Getenv("PREVIEW_RUNNER_TOKEN"), dataPath: os.Getenv("PREVIEW_DATA_PATH"), image: os.Getenv("PREVIEW_BUILDER_IMAGE")}
	if c.token == "" || c.dataPath == "" || c.image == "" {
		log.Fatal("PREVIEW_RUNNER_TOKEN, PREVIEW_DATA_PATH and PREVIEW_BUILDER_IMAGE are required")
	}
	d := newDocker()
	buildSlots := make(chan struct{}, 1) // One bounded build at a time; avoid resource amplification.
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(204) })
	mux.HandleFunc("POST /v1/build", func(w http.ResponseWriter, r *http.Request) {
		if subtle.ConstantTimeCompare([]byte(r.Header.Get("Authorization")), []byte("Bearer "+c.token)) != 1 {
			http.Error(w, "unauthorized", 401)
			return
		}
		r.Body = http.MaxBytesReader(w, r.Body, 4096)
		var req buildRequest
		if e := json.NewDecoder(r.Body).Decode(&req); e != nil || req.WorkspaceID < 1 || req.ProjectID < 1 {
			http.Error(w, "invalid request", 400)
			return
		}
		select {
		case buildSlots <- struct{}{}:
			defer func() { <-buildSlots }()
		default:
			http.Error(w, "another preview build is already running", http.StatusTooManyRequests)
			return
		}
		result, buildErr := d.build(c, req)
		w.Header().Set("Content-Type", "application/json")
		if buildErr != nil {
			result.Status = "FAILED"
			result.Logs = buildErr.Error()
			w.WriteHeader(422)
		}
		_ = json.NewEncoder(w).Encode(result)
	})
	srv := &http.Server{Addr: ":8090", Handler: mux, ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 10 * time.Second, WriteTimeout: 8 * time.Minute, IdleTimeout: 30 * time.Second}
	log.Fatal(srv.ListenAndServe())
}
