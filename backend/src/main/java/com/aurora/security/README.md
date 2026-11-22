# Authentication profiles

The default and `cloud` profiles use account sessions and workspace membership checks. The cloud profile disables first-owner bootstrap by default; set `AURORA_BOOTSTRAP_ENABLED=true` only during initial setup, create the owner account, then turn it off.

The legacy shared owner-token filter is available only with the explicit `legacy-owner-token` profile. It is for isolated single-owner compatibility only and must not be used for multi-user deployments.
