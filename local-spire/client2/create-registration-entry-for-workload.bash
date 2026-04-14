#!/usr/bin/env bash
set -euo pipefail

#parent_spiffe_id='spiffe://example.org/spire/agent/k8s_psat/example-cluster/bc21ec4c-9192-45f9-bc71-57e08b1d41f2'
parent_spiffe_id='spiffe://example.org/ns/spire-server/sa/spire-agent'
kubectl exec -n spire-server spire-server-0 -- \
    /opt/spire/bin/spire-server entry create \
    -spiffeID spiffe://example.org/ns/default/sa/client2 \
    -parentID "${parent_spiffe_id}" \
    -selector k8s:ns:default \
    -selector k8s:sa:client2 \
    ;
