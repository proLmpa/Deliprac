# Trivy Image Scan — Jenkinsfile Integration

## Add this stage to Jenkinsfile BETWEEN "Build Images" and "Push Images"

```groovy
stage('Scan Images') {
    steps {
        script {
            // Install Trivy if not present on the Jenkins agent
            sh '''
                if ! command -v trivy &> /dev/null; then
                    curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh \
                      | sh -s -- -b /usr/local/bin
                fi
            '''

            def services = [
                'bff-service', 'user-service', 'store-service',
                'order-service', 'notification-service', 'ai-service', 'front-service'
            ]
            def scanTasks = [:]
            for (svc in services) {
                def s = svc
                scanTasks[s] = {
                    sh """
                        trivy image \\
                          --exit-code 1 \\
                          --severity CRITICAL \\
                          --ignore-unfixed \\
                          --cache-dir /var/cache/trivy \\
                          --format table \\
                          ${DOCKER_HUB_USER}/${s}:${IMAGE_TAG}
                    """
                }
            }
            parallel scanTasks
        }
    }
}
```

## Flags explained

| Flag | Meaning |
|---|---|
| `--exit-code 1` | Return non-zero if vulnerabilities found → fails the stage |
| `--severity CRITICAL` | Only gate on CRITICAL (use CRITICAL,HIGH for stricter) |
| `--ignore-unfixed` | Skip CVEs where no patched version exists yet |
| `--cache-dir /var/cache/trivy` | Cache the vuln DB between builds (mount as Jenkins agent volume) |
| `--format table` | Human-readable output; use `--format json` for downstream parsing |

## Understanding the scan output

```
eclipse-temurin:25-jre-noble (ubuntu 24.04)
===========================================
Total: 3 (CRITICAL: 0, HIGH: 3)

┌─────────────┬────────────────┬──────────┬───────────────────┬────────────────┬─────────────────────┐
│   Library   │ Vulnerability  │ Severity │ Installed Version │  Fixed Version │        Title        │
├─────────────┼────────────────┼──────────┼───────────────────┼────────────────┼─────────────────────┤
│ libssl3     │ CVE-2024-xxxxx │ HIGH     │ 3.0.13-0ubuntu3   │ 3.0.13-0ubuntu3.4 │ ...              │
└─────────────┴────────────────┴──────────┴───────────────────┴────────────────┴─────────────────────┘
```

Two sections appear:
1. **OS packages** (ubuntu layer): apt-installed libs like openssl, libc, curl
2. **Application** (jar layer): Maven/Gradle dependencies in the Spring Boot fat jar

## Scan locally (before pushing)

```bash
# Install trivy
brew install trivy  # or: https://github.com/aquasecurity/trivy/releases

# Scan the local image (must be built first with ./gradlew bootJar + docker build)
trivy image --severity CRITICAL,HIGH --ignore-unfixed prolmpa/user-service:latest

# Generate SBOM (Software Bill of Materials) — for compliance/auditing
trivy image --format cyclonedx --output user-service-sbom.json prolmpa/user-service:latest

# Scan only the application layer (skip OS packages — useful if base is pinned by policy)
trivy image --vuln-type library prolmpa/user-service:latest
```

## Why eclipse-temurin:25-jre-noble is a good choice

- Ubuntu Noble (24.04) has newer packages than Jammy (22.04) → fewer CVEs
- `-jre` variant (not `-jdk`) excludes compiler, javac, development tools → smaller attack surface
- `-noble` gets Ubuntu security backports for 5 years

## Distroless alternative (advanced)

For near-zero OS CVEs, consider Google's distroless Java image:
```dockerfile
FROM gcr.io/distroless/java21-debian12:nonroot
# No shell, no package manager, no OS utilities — only the JRE
# Trivy OS scan: 0 CVEs (no package database to scan)
# Downside: cannot exec into the container for debugging (use ephemeral containers instead)
COPY --from=builder /app/app.jar /app.jar
ENTRYPOINT ["/app.jar"]
```
