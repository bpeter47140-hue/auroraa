FROM node:22-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends chromium ca-certificates fonts-liberation fonts-noto-color-emoji && rm -rf /var/lib/apt/lists/* && groupadd --system qa && useradd --system --uid 10001 --gid qa --home-dir /app qa
WORKDIR /app
COPY package.json ./
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
RUN npm install --omit=dev --no-audit --no-fund
COPY server.js ./
USER 10001:10001
ENV NODE_ENV=production CHROMIUM_PATH=/usr/bin/chromium
EXPOSE 8091
CMD ["npm","start"]
