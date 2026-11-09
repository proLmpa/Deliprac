# K8s Deep-Dive Workbook

Work through each directory in order. Apply with `kubectl apply -f k8s/<dir>/` and observe.

## Concept Map

```
OCI Image (layers, digest)
  └─ containerd (pulls, unpacks, runs)
       └─ runc / runsc (creates Linux namespaces + cgroups)
            └─ Pod (K8s wrapper around 1+ containers)
                 ├─ securityContext  (Phase 2)
                 ├─ ServiceAccount   (Phase 3 — RBAC)
                 ├─ NetworkPolicy    (Phase 4 — isolation)
                 ├─ ResourceQuota    (Phase 5 — limits)
                 └─ Linkerd sidecar  (Phase 6 — mTLS)
```

## Lab: Inspect running containers with crictl

```bash
# SSH into minikube node
minikube ssh

# List all running containers (containerd view, not Docker)
sudo crictl ps

# Inspect a specific container (shows image layers, mounts, securityContext applied)
sudo crictl inspect <CONTAINER_ID>

# See image layers for user-service
sudo crictl inspecti prolmpa/user-service:latest

# Check which user a process runs as (before hardening: root; after: 1000)
sudo crictl exec <CONTAINER_ID> id
```

## Learning Path

| Dir | Topic | Key command to run after applying |
|---|---|---|
| 01-psa | PodSecurityAdmission | `kubectl describe namespace baemin` |
| 02-rbac | ServiceAccounts + Roles | `kubectl auth can-i list pods --as=...` |
| 03-network-policy | Zero-trust isolation | `kubectl exec ... -- curl ...` |
| 04-quotas | ResourceQuota/LimitRange/HPA/PDB | `kubectl describe resourcequota -n baemin` |
| 05-linkerd | Service Mesh + mTLS | `linkerd viz tap deploy/bff-service` |
| 06-trivy | Image vulnerability scan | trigger Jenkins build |
| 07-falco | Runtime threat detection | `kubectl exec pod -- sh` → watch Falco logs |
