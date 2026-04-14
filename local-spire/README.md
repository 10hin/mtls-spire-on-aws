## local-spire

> Commands run in `local-spire/` directory.

```shell
# Create kind cluster
kind create cluster --config kind/cluster.yaml

# Install spire (server and agent)
spire/helm-install.bash

# register agent SVID
./create-registration-entry-for-node.bash
```


### Example workload: `client`/`backend`

```shell
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


### Example workload: `client2`/`server2`

```shell
# build applications

cd apps/client2/
# build `client2` app
./mvnw clean spring-boot:build-image
# load container image (built above) into kind cluster
kind load docker-image --name spire-local client2:0.0.1-SNAPSHOT
cd -

cd apps/server2/
# build `server2` app
./mvnw clean spring-boot:build-image
# load container image (built above) into kind cluster
kind load docker-image --name spire-local server2:0.0.1-SNAPSHOT
cd -


# deploy applications

# register `server2` workload (service account)
./server2/create-registration-entry-for-workload.bash
# deploy `client2` app
kubectl apply -f server2/

# register `client2` workload (service account)
./client2/create-registration-entry-for-workload.bash
# deploy `client2` app
kubectl apply -f client2/


# verify workloads

# deploy console Pod
kubectl apply -f console-deployment.yaml

# verify backend server certs
kubectl exec -it "$(kubectl get po -l app=console -o name)" -- bash -c 'openssl s_client -showcerts -connect server2:8443 | openssl x509 -text'
# Above command will print certificate detail including following lines:
# >             X509v3 Subject Alternative Name:
# >                 URI:spiffe://example.org/ns/default/sa/server2
# It shows the server(server2:8443) returns certificate with SPIFFE ID "spiffe://example.org/ns/default/sa/server2"

# verify server2 requires TLS client cert
kubectl exec -it "$(kubectl get po -l app=console -o name)" -- curl -vk https://server2:8443/hello
# Above command will fail, and will print communication details including following lines:
# > curl: (56) OpenSSL SSL_read: OpenSSL/3.5.5: error:0A00045C:SSL routines::tlsv13 alert certificate required, errno 0
# It shows the server(server2:8443) requires TLS client certs and abort the connection from console Pod.

# verify client2 works
kubectl exec -it "$(kubectl get po -l app=console -o name)" -- curl http://client2:8080/hello
# Above command will print "Hello, World! (from client2)" (without NewLine).

# verify client2 can access server2
kubectl exec -it "$(kubectl get po -l app=console -o name)" -- curl -XPOST http://client2:8080/backend
# Above command will print "result of calling backend: Hello, World! (from server2)" (without NewLine).
# It contains response of server2, and shows client2 successfully access server2.
```


## Notes

### Using `java-spiffe-provider` package

- Implementation
  - Use `SpiffeSslContextFactory.getSslContext(SslContextOptions)` to get `SSLContext`.
  - Use Jetty as embedded web server, not Tomcat.
- Test implementation
  - When application started with maven/gradle plugin in tests, main-method would not called, thus, `SpiffeProvider.install()` code in main-method will not work while tests.
    - If you want to run tests with `SpiffeProvider.install()`, call it in static initializer of `@SpringApplication` class.
    - Many other problems remains when running tests with actual SPIFFE implementation. It recommended running tests with actual SPIFFE implementation, while integration tests, not unit tests. (Ingeneral, spire-agent and spire-server are EXTERNAL-System of application)
