#!/usr/bin/env bash
set -euo pipefail

kubectl exec -n spire-server spire-server-0 -- \
    /opt/spire/bin/spire-server entry create \
    -spiffeID spiffe://example.org/ns/default/sa/client \
    -parentID spiffe://example.org/ns/spire-server/sa/spire-agent \
    -selector k8s:ns:default \
    -selector k8s:sa:client
