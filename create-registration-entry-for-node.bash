#!/usr/bin/env bash
set -euo pipefail

kubectl exec -n spire-server spire-server-0 -- \
    /opt/spire/bin/spire-server entry create \
    -spiffeID spiffe://example.org/ns/spire-server/sa/spire-agent \
    -selector k8s_sat:cluster:example-cluster \
    -selector k8s_sat:agent_ns:spire-server \
    -selector k8s_sat:agent_sa:spire-agent \
    -node
