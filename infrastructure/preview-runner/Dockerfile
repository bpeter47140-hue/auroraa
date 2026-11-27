FROM golang:1.24-alpine AS build
WORKDIR /src
COPY go.mod main.go ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /preview-runner main.go

FROM alpine:3.21
RUN addgroup -S -g 10001 runner && adduser -S -u 10001 -G runner runner \
  && mkdir -p /app/data/project-workspaces && chown -R runner:runner /app
COPY --from=build --chown=runner:runner /preview-runner /usr/local/bin/preview-runner
ENV PREVIEW_DATA_PATH=/app/data/project-workspaces PREVIEW_BUILDER_IMAGE=aurora-preview-builder:1
USER 10001:10001
EXPOSE 8090
ENTRYPOINT ["/usr/local/bin/preview-runner"]
