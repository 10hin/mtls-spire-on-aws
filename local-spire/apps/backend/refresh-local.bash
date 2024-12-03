#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

kind_cluster_name="$(yq '.name' ../../kind/cluster.yaml)"
kube_context="$(kind get kubeconfig --name "${kind_cluster_name}" | yq -r '.contexts[0].name')"
gradle_project="$(./gradlew :properties | grep -E '^name: ' | sed -r -e 's/^name: ([^ ]*)$/\1/')"
gradle_version="$(./gradlew :properties | grep -E '^version: ' | sed -r -e 's/^version: ([^ ]*)$/\1/')"

./gradlew bootImage
kind load docker-image --name "${kind_cluster_name}" "${gradle_project}:${gradle_version}"
