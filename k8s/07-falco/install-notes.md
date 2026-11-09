# Falco Install on minikube

## Install via Helm

```bash
# Add repo
helm repo add falcosecurity https://falcosecurity.github.io/charts
helm repo update

# Create namespace (needs privileged PSA — Falco reads kernel syscalls)
kubectl create namespace falco
kubectl label namespace falco pod-security.kubernetes.io/enforce=privileged

# Install with eBPF driver (more portable than kernel module on minikube)
# Reduced resource requests for minikube
helm install falco falcosecurity/falco \
  --namespace falco \
  --set driver.kind=ebpf \
  --set falcosidekick.enabled=false \
  --set resources.requests.cpu=100m \
  --set resources.requests.memory=256Mi \
  --set resources.limits.cpu=500m \
  --set resources.limits.memory=512Mi

# Wait for Falco DaemonSet to become ready (~2 minutes)
kubectl rollout status daemonset/falco -n falco

# Verify Falco is detecting events (you should see default rule hits)
kubectl logs -n falco -l app.kubernetes.io/name=falco --tail=30
```

## Load custom rules

```bash
# Apply the custom rules ConfigMap
kubectl apply -f k8s/07-falco/falco-configmap.yaml

# Restart Falco DaemonSet to pick up new rules
kubectl rollout restart daemonset/falco -n falco

# Watch for rule violations in real time
kubectl logs -n falco -l app.kubernetes.io/name=falco -f
```

## Test a rule

```bash
# This should trigger "Unexpected Process in Spring Boot Container"
kubectl exec -n baemin deploy/user-service -- sh -c "id"
# Expected Falco output:
# 15:30:01.123456789: Critical Unexpected process in Spring Boot container
#   (user=root command=sh -c id container=user-service image=prolmpa/user-service)
```
