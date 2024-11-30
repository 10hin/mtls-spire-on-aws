# mtls-spire-on-aws

## これはなに

SPIREを用いた、Amazon EKS環境におけるmTLS構成を勉強するためのリポジトリ

## このコミットについて

### TL;DR

以下を実行します

```shell
# create cluster
eksctl create cluster -f eks-cluster.yaml

# aws-ebs-csi-driver and gp3 StorageClass installed with helm
./aws-ebs-csi-driver/helm-install.bash

# spire-server and spire-agent install with helm
./spire/helm-install.bash

# create registration entry for first node/spire-agent
./create-registration-entry-for-node.bash

# create registration entry for workload with namespace `default` and serviceaccount `default`
./create-registration-entry-for-workload.bash

# run `client` Pod
kubectl apply -f client-deployment.yaml

# get SVID for `client` Pod
kubectl exec -it "$(kubectl get po -l app=client -o name)" -c client -- /opt/spire/bin/spire-agent api fetch x509 -socketPath='/run/spire/sockets/spire-agent.sock' -write /run/share

# show detail of X.509 SVID
kubectl exec -it "$(kubectl get po -l app=client -o name)" -c console -- openssl x509 -text -in /run/share/svid.0.pem
```

すると最後のコマンドでSPIREによって `client` Podに発行されたX.509形式のSVIDの内容の詳細がOpenSSLによって以下のように表示されます[^1]:

```text
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
        Signature Algorithm: sha256WithRSAEncryption
        Issuer: C = ARPA, O = Example, CN = example.org, serialNumber = 123456789000000000000000000000000000000
        Validity
            Not Before: Jan 1 01:00:00 2024 GMT
            Not After : Jan 1 05:00:10 2024 GMT
        Subject: C = US, O = SPIRE
        Subject Public Key Info:
            Public Key Algorithm: id-ecPublicKey
                Public-Key: (256 bit)
                pub:
                    00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
                    00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
                    00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
                    00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
                    00:00:00:00:00
                ASN1 OID: prime256v1
                NIST CURVE: P-256
        X509v3 extensions:
            X509v3 Key Usage: critical
                Digital Signature, Key Encipherment, Key Agreement
            X509v3 Extended Key Usage:
                TLS Web Server Authentication, TLS Web Client Authentication
            X509v3 Basic Constraints: critical
                CA:FALSE
            X509v3 Subject Key Identifier:
                00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
            X509v3 Authority Key Identifier:
                00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
            X509v3 Subject Alternative Name:
                URI:spiffe://example.org/ns/default/sa/default
    Signature Algorithm: sha256WithRSAEncryption
    Signature Value:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:
        00:00:00:00
-----BEGIN CERTIFICATE-----
(reduced)
-----END CERTIFICATE-----
```

[^1]: SVIDは短期認証情報であるため、実際の値をここに記載しても問題ないはずですが、念のため鍵情報などはゼロ値(など)で塗りつぶししてあります

`X509v3 Subject Alternative Name` のところに `URI:spiffe://example.org/ns/default/sa/default` とあり、これは `client` Podに割り当てられることが期待されたSPIFFEIDです。したがって、クライアント/サーバーのいずれとしてもこの証明書を提示し、通信先はこの証明書を検証(CA署名(必須)+有効期限(必須)+失効確認(任意))したうえでSANsに指定されたSPIFFEIDをこのPodの認証情報として認可・アプリケーション処理を行います。
