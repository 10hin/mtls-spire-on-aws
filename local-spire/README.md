## local-spire

> Commands run in `local-spire/` directory.

```shell
# Create kind cluster
kind create cluster --config kind/cluster.yaml

# Install spire (server and agent)
spire/helm-install.bash

# register agent SVID
./create-registration-entry-for-node.bash

# build applications
# build `backend` app
cd apps/backend/
./gradlew bootBuildImage
kind load docker-image --name spire-local backend:0.0.1-SNAPSHOT
cd -
# build client app
cd apps/client/
./gradlew bootBuildImage
kind load docker-image --name spire-local client::0.0.1-SNAPSHOT
cd -

# deploy applications
# register `backend` workload
./backend/create-registration-entry-for-workload.bash
# deploy `backend` app
kubectl apply -f backend/
# register `client` workload
./client/create-registration-entry-for-workload.bash
# deploy `client` app
kubectl apply -f client/

# deploy console Pod
kubectl apply -f console-deployment.yaml

# check client app
kubectl exec -it "$(kubectl get po -l app=console -o name)" -- curl client/hello
# above command would response successfully

# check backend app
kubectl exec -it "$(kubectl get po -l app=console -o name)" -- bash -c 'openssl s_client -connect backend.default.svc.cluster.local:443 | openssl x509 -text'
# above command would show TLS certificate details for backend app

# request client tot backend
kubectl exec -it "$(kubectl get po -l app=console -o name)" -- curl -XPOST -H 'Content-Type: application/json' -d '{"url":"https://backend.default.svc.cluster.local/hello","method":"GET","body":"","content-type":"application/json"}' http://client.default.svc.cluster.local/backend
# Currently, above request may FAIL with certificate error like following, between client and backend
```
