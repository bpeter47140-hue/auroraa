# Isolated website preview builder

Aurora builds React/Vite project workspaces in disposable containers. Generated source is never executed inside the backend or the preview-runner service itself.

## Runtime flow

1. An authenticated workspace member requests **Build & preview** from the project dashboard.
2. The backend sends only workspace and project IDs to `preview-runner` over the private `preview-internal` Docker network, using `AURORA_PREVIEW_RUNNER_TOKEN`.
3. The runner validates the workspace path (including symlink checks), the `package.json` build command, and source file count/size limits.
4. The runner creates a disposable builder container from `aurora-preview-builder:1`. It has no network, runs as UID/GID 10001, drops all Linux capabilities, enables `no-new-privileges`, has a 512 MiB memory limit, 1 CPU, 128 PIDs, and a 128 MiB disposable writable container layer and a 192 MiB `/tmp` tmpfs. The container is deleted after each build.
5. The builder image contains preinstalled React, TypeScript and Vite dependencies. Project package scripts are not installed or executed; only `vite build` and `tsc -b && vite build` are allowed, with custom prebuild/postbuild scripts rejected.
6. The built static output is unpacked with path, file-count and size checks into the project's `preview/` directory in the shared workspace volume.
7. The backend issues a 30-minute HMAC-signed preview URL. It serves the static files with a restrictive Content-Security-Policy; the dashboard embeds the site in a sandboxed iframe with desktop/mobile sizing.

## Resource limits

- One preview build at a time per runner instance.
- Maximum 100 source files and 5 MiB total source input.
- Maximum 5 minutes for the build command.
- Maximum 1,500 output archive entries, 10 MiB per file and 30 MiB total output.
- Builder has no network access and no host workspace or host filesystem bind mounts.
- Preview URLs expire after 30 minutes and can be refreshed by an authorized workspace member.

## Host setup

The preview runner needs access to the host Docker socket to create disposable build containers. It runs as UID 10001 and receives the socket's numeric group as a supplementary group. Set `DOCKER_GID` in `.env` to the output of:

```sh
stat -c %g /var/run/docker.sock
```

The checked-in `.env.example` uses `947` as the example value; this is host-specific. Generate unique random values for `AURORA_PREVIEW_RUNNER_TOKEN` and `AURORA_PREVIEW_SIGNING_SECRET` (at least 32 characters each) and keep them private.

Build and start the preview services with:

```sh
docker compose build preview-builder preview-runner
docker compose up -d preview-builder preview-runner backend
```

The runner has no published host port and is attached only to the internal `preview-internal` network. The backend joins both its normal application network and `preview-internal`.

## Security boundary and production note

The website build process is isolated from the backend: no network, no host bind mounts, non-root user, dropped capabilities, strict resource limits, bounded source/output, and disposable container lifecycle. The preview-runner itself is a trusted control-plane component because access to a Docker socket is effectively host-level authority even when the process is non-root. Keep it private, protect its bearer token, and do not expose port 8090 publicly. For a multi-tenant public production deployment, move the builder to a separate isolated worker host or put a carefully restricted Docker API broker between the runner and daemon. This local Compose setup is a development-stage isolation boundary, not a claim of a hardened hostile-tenant container platform.
