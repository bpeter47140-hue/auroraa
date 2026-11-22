# AURORA AI Engine

Python service for agent orchestration. This service will communicate with the Java backend over authenticated internal APIs.

Planned first agent: Research Agent.

The AI engine will not have direct database write access or unrestricted shell access. It will submit proposed actions to the Java control plane, where permissions and approvals are enforced.
