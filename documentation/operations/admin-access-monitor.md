# AURORA platform access monitoring

The platform owner can open **Access monitor** in the dashboard. Platform-wide identity metrics are not granted to ordinary workspace administrators.

## Metrics and definitions

- **Registered accounts:** all AURORA user accounts, including disabled accounts.
- **Active accounts:** accounts whose account status is active.
- **Disabled accounts:** accounts that cannot authenticate.
- **Accounts with sign-in history:** accounts with a recorded registration or login.
- **Pending invitations:** invitations that have not been accepted, revoked or expired.
- **Online users:** distinct active accounts with at least one valid session heartbeat in the previous five minutes.
- **Active sessions:** valid, unrevoked sessions for active accounts. An idle session can remain valid without its user being online.
- **Sessions started in 30 days:** session records created in the previous 30 days. Initial account registration also creates a session, so this is intentionally labelled sessions rather than logins.

The dashboard sends an authenticated heartbeat every 60 seconds and refreshes its access snapshot every 30 seconds while the Access monitor is open. A user with two browsers counts as one online person and two sessions. Up to 100 valid sessions are listed, newest activity first.

Anonymous visitors to a public landing page are not included. Public page-traffic analytics require a separate privacy-conscious visitor counter.

## API

- `GET /api/v1/admin/access` — platform-wide account and session snapshot.
- `POST /api/v1/admin/access/sessions/{sessionId}/revoke` — revoke a session.
- `POST /api/v1/auth/activity` — update the current authenticated session's last-seen timestamp.

The admin endpoint checks the authenticated account's `platform_admin` flag on the backend. Hiding the navigation item is only a UI convenience, not the security boundary. Session tokens remain stored as SHA-256 digests; raw tokens and IP addresses are not included in monitoring responses.

## Platform-owner assignment

Migration V12 adds the platform-admin flag and session activity timestamps. Existing installations assign the flag to the earliest-created AURORA account; first-owner registration on a new installation grants it to the first owner. Workspace ADMIN and OWNER roles do not independently grant platform-wide monitoring privileges.
