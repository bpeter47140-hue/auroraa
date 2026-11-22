# AURORA AI Engine — future component

The current AURORA release executes its specialist agents inside the Java/Spring Boot backend. This directory is a placeholder for a possible future, separately deployed Python orchestration service; it is not part of the current Docker Compose deployment and is not required to run the product.

If introduced later, the service must communicate through authenticated internal APIs. It must not receive direct database write access, unrestricted shell access, provider credentials that bypass the control plane, or permission to perform financial/public actions without owner approval.
