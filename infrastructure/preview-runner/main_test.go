package main

import (
	"archive/tar"
	"bytes"
	"os"
	"path/filepath"
	"testing"
)

func TestSafeJoinRejectsTraversal(t *testing.T) {
	root := t.TempDir()
	for _, name := range []string{"../escape", "/etc/passwd", "a/../../escape"} {
		if _, err := safeJoin(root, name); err == nil {
			t.Fatalf("expected unsafe path rejection for %q", name)
		}
	}
	if _, err := safeJoin(root, "assets/app.js"); err != nil {
		t.Fatalf("safe relative path rejected: %v", err)
	}
}

func TestUnpackPreviewAcceptsDistRoot(t *testing.T) {
	var buf bytes.Buffer
	tw := tar.NewWriter(&buf)
	for _, entry := range []struct {
		name, body string
		kind       byte
	}{{"dist/", "", tar.TypeDir}, {"dist/index.html", "<h1>Preview</h1>", tar.TypeReg}, {"dist/assets/app.js", "console.log('ok')", tar.TypeReg}} {
		h := &tar.Header{Name: entry.name, Mode: 0644, Size: int64(len(entry.body)), Typeflag: entry.kind}
		if err := tw.WriteHeader(h); err != nil {
			t.Fatal(err)
		}
		if entry.kind == tar.TypeReg {
			if _, err := tw.Write([]byte(entry.body)); err != nil {
				t.Fatal(err)
			}
		}
	}
	if err := tw.Close(); err != nil {
		t.Fatal(err)
	}
	dest := filepath.Join(t.TempDir(), "preview")
	if err := unpackPreview(buf.Bytes(), dest); err != nil {
		t.Fatal(err)
	}
	content, err := os.ReadFile(filepath.Join(dest, "index.html"))
	if err != nil {
		t.Fatal(err)
	}
	if string(content) != "<h1>Preview</h1>" {
		t.Fatalf("unexpected index: %s", content)
	}
	if _, err = os.Stat(filepath.Join(dest, "assets", "app.js")); err != nil {
		t.Fatal(err)
	}
}

func TestUnpackPreviewRejectsTraversal(t *testing.T) {
	var buf bytes.Buffer
	tw := tar.NewWriter(&buf)
	h := &tar.Header{Name: "../outside", Mode: 0644, Size: 1, Typeflag: tar.TypeReg}
	if err := tw.WriteHeader(h); err != nil {
		t.Fatal(err)
	}
	_, _ = tw.Write([]byte("x"))
	_ = tw.Close()
	if err := unpackPreview(buf.Bytes(), filepath.Join(t.TempDir(), "preview")); err == nil {
		t.Fatal("expected traversal archive to be rejected")
	}
}

func TestProjectWorkspaceRejectsSymlinkedTenantDirectory(t *testing.T) {
	base := t.TempDir()
	outside := t.TempDir()
	if err := os.Symlink(outside, filepath.Join(base, "ws-7")); err != nil {
		t.Skipf("symlink unavailable: %v", err)
	}
	if _, err := projectWorkspace(base, 7, 9); err == nil {
		t.Fatal("expected symlinked workspace to be rejected")
	}
}

func TestProjectWorkspaceRequiresRealProjectDirectories(t *testing.T) {
	base := t.TempDir()
	if _, err := projectWorkspace(base, 7, 9); err == nil {
		t.Fatal("expected missing workspace to be rejected")
	}
	if err := os.Mkdir(filepath.Join(base, "ws-7"), 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.Mkdir(filepath.Join(base, "ws-7", "project-9"), 0755); err != nil {
		t.Fatal(err)
	}
	got, err := projectWorkspace(base, 7, 9)
	if err != nil {
		t.Fatalf("valid workspace rejected: %v", err)
	}
	if got != filepath.Join(base, "ws-7", "project-9") {
		t.Fatalf("unexpected workspace path: %s", got)
	}
}

func TestValidatePackageAllowsOnlyKnownBuildScripts(t *testing.T) {
	workspace := t.TempDir()
	manifest := `{"scripts":{"build":"tsc -b && vite build"}}`
	if err := os.WriteFile(filepath.Join(workspace, "package.json"), []byte(manifest), 0600); err != nil {
		t.Fatal(err)
	}
	if err := validatePackage(workspace); err != nil {
		t.Fatalf("approved manifest rejected: %v", err)
	}
	for _, bad := range []string{
		`{"scripts":{"build":"vite build && curl evil"}}`,
		`{"scripts":{"build":"vite build","prebuild":"node exploit.js"}}`,
		`{"scripts":{"build":"npm install"}}`,
	} {
		if err := os.WriteFile(filepath.Join(workspace, "package.json"), []byte(bad), 0600); err != nil {
			t.Fatal(err)
		}
		if err := validatePackage(workspace); err == nil {
			t.Fatalf("unsafe manifest accepted: %s", bad)
		}
	}
}
