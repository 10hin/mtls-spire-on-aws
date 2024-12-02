#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

helm upgrade \
  --install \
  --namespace spire-server \
  spire-crds \
  spire-crds \
  --repo https://spiffe.github.io/helm-charts-hardened/ \
  --create-namespace \
  ;
helm upgrade \
  --install \
  --namespace spire-server \
  spire \
  spire \
  --repo https://spiffe.github.io/helm-charts-hardened/ \
  --values values.yaml \
  ;
