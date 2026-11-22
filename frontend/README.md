# AURORA Owner Dashboard

React + TypeScript dashboard for the Project AURORA business operating system.

## Development

    npm ci
    npm run dev

Open http://127.0.0.1:5173. Vite binds to loopback by default. To use the isolated Neon development backend on port 8081:

    AURORA_BACKEND_TARGET=http://127.0.0.1:8081 npm run dev -- --port 5174

Vite proxies /api and /actuator requests to Spring Boot on port 8080 by default. Set AURORA_BACKEND_TARGET to override the target. Never expose the Vite development server as the production dashboard; use the built static assets served by the Nginx container.

## Production build

    npm ci
    npm run lint
    npm run build

The overview page checks the backend status endpoint every 15 seconds.
