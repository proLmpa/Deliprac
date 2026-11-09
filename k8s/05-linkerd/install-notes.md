# Linkerd Install on minikube

## Prerequisites
```bash
# Verify minikube has sufficient resources
minikube config view
# Recommended: --memory=4096 --cpus=4

# Install Linkerd CLI
curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/install | sh
export PATH=$PATH:$HOME/.linkerd2/bin

# Validate cluster readiness
linkerd check --pre
```

## Install (CNI mode — avoids NET_ADMIN / PSA conflict)

Using CNI mode means Linkerd does NOT inject an init container that needs
NET_ADMIN capability. Instead, a CNI plugin handles iptables routing at the
node level. This keeps the `baemin` namespace compatible with PSA Restricted.

```bash
# Step 1: Install CRDs (separate chart in Linkerd 2.14+)
linkerd install --crds | kubectl apply -f -

# Step 2: Install CNI plugin (must be before control plane)
linkerd install-cni | kubectl apply -f -

# Step 3: Install control plane with CNI enabled + reduced resource footprint for minikube
linkerd install \
  --set cniEnabled=true \
  --set controllerLogLevel=warn \
  --set proxy.resources.cpu.request=10m \
  --set proxy.resources.memory.request=20Mi \
  | kubectl apply -f -

# Step 4: Verify (wait ~2 minutes for all pods to become ready)
linkerd check
```

## Install Viz (optional — provides dashboard and tap)

Viz is heavy (~512MB extra RAM). Skip if minikube memory is tight.
```bash
linkerd viz install | kubectl apply -f -
linkerd viz dashboard &   # opens browser dashboard
```

## Verify mTLS is working

```bash
# After injecting baemin namespace (see namespace-inject.yaml):
# Tap live traffic — shows protocol, response codes, latency
linkerd viz tap deploy/bff-service -n baemin

# Check mTLS edges (shows which connections are mTLS vs plain)
linkerd viz edges deploy -n baemin

# Check that proxies are healthy
linkerd check --proxy -n baemin
```
