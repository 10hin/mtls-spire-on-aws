#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

helm upgrade \
  --install \
  --namespace kube-system \
  aws-ebs-csi-driver \
  aws-ebs-csi-driver \
  --repo https://kubernetes-sigs.github.io/aws-ebs-csi-driver \
  --values values.yaml \
  ;
