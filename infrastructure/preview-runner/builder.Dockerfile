FROM node:24-alpine
WORKDIR /opt/deps
RUN npm install --no-audit --no-fund --ignore-scripts \
  react@19 react-dom@19 vite@6 typescript@5 \
  @vitejs/plugin-react@4 @types/react@19 @types/react-dom@19
RUN addgroup -S -g 10001 preview && adduser -S -u 10001 -G preview preview \
  && mkdir -p /workspace && chown -R preview:preview /workspace
COPY vite.config.ts /opt/deps/preview-vite.config.ts
USER 10001:10001
WORKDIR /workspace
