# AURORA Owner Dashboard

React + TypeScript dashboard for the Project AURORA business operating system.

## Development

```bash
npm install
npm run dev -- --host 0.0.0.0
```

Open `http://localhost:5173`. Vite proxies `/api` and `/actuator` requests to the Spring Boot backend on port 8080.

## Production build

```bash
npm run build
```

The overview page checks the backend status endpoint every 15 seconds.
