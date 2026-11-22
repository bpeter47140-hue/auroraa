# AURORA Architecture v1

## Components

- Java 25 / Spring Boot: authoritative API and business control plane.
- React + TypeScript: owner dashboard.
- Spring Data JPA: persistence.
- H2 file database: local development persistence.
- PostgreSQL: intended container/cloud persistence.
- Optional OpenAI-compatible model provider: Research Agent language model.
- Docker Compose: repeatable multi-service packaging.
- GitHub Actions: backend and frontend build/test pipeline.

## Core domains

- Business workspaces: independent business records, goals and lifecycle status.
- Tasks: priority, status and timestamps.
- Approvals: explicit owner decision record. Approval does not itself execute money movement or an external action.
- Audit: application events recorded for important state changes.
- Agents: specialist catalogue and Research Agent model adapter.

## Trust boundaries

AI agents may produce research and propose work. The Java backend validates input and records state. Financial transfers, investments, contracts, paid service activations and public publishing are not implemented as autonomous actions. They must remain behind explicit owner approval and additional policy enforcement.

AI credentials are read only from backend environment variables. They must never be embedded in frontend assets or committed to Git.

## Deployment

The Docker Compose file runs PostgreSQL, the backend and an Nginx-served dashboard. The database is not published to the host. The dashboard is bound to `127.0.0.1:8088` for local container testing; a trusted HTTPS ingress must proxy to this loopback-only port before internet exposure.

Public deployment is blocked until authentication, authorization, HTTPS, rate limiting, secret management, backups and restore procedures are added. A hosting account and deployment credentials are required for an always-on cloud installation.
