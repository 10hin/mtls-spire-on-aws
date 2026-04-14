package com.example.server2;

import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.workloadapi.DefaultX509Source;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SniX509ExtendedKeyManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.Security;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class Server2Application {
	static {
		Security.insertProviderAt(new SpiffeProvider(), 1);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Server2Application.class);

	private static final String SPIFFE_ALGORITHM = "Spiffe";
	private static final String SPIFFE_PROVIDER = "Spiffe";
	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) {
		SpringApplication.run(Server2Application.class, args);
	}

	private SSLContext getSpiffeSSLContext() {
		try {
			final var spiffeSslContextOptions = SpiffeSslContextFactory.SslContextOptions.builder()
					.sslProtocol(SPIFFE_PROTOCOL)
					.x509Source(DefaultX509Source.newSource())
					.acceptedSpiffeIdsSupplier(() -> Set.of(SpiffeId.parse("spiffe://example.org/ns/default/sa/client2")))
					.build();
			return SpiffeSslContextFactory.getSslContext(spiffeSslContextOptions);
		} catch (final Exception e) {
			throw new InternalError("SSLContext initialization failed", e);
		}
	}

	@Bean
	public JettyServletWebServerFactory jettyFactory(
			@Value("${server.port}")
			final int serverPort,
			@Value("${server2.spiffe.enabled}")
			final boolean spiffeEnabled
	) {
		final var factory = new JettyServletWebServerFactory();
		if (spiffeEnabled) {
			final var spiffeSSLContext = this.getSpiffeSSLContext();
			factory.addServerCustomizers(server -> {
				for (final var connector : server.getConnectors()) {
					server.removeConnector(connector);
				}
				final var sslContextFactory = new SslContextFactory.Server();
				sslContextFactory.setSslContext(spiffeSSLContext);
				sslContextFactory.setNeedClientAuth(true);
				sslContextFactory.setSniRequired(false);
				LOGGER.info("sslContextFactory.isSniRequired(): {}", sslContextFactory.isSniRequired());
				sslContextFactory.setEndpointIdentificationAlgorithm(null);
				LOGGER.info("sslContextFactory.getEndpointIdentificationAlgorithm(): {}", sslContextFactory.getEndpointIdentificationAlgorithm());
				final var httpsConfig = new HttpConfiguration();
				final var secureRequestCustomizer = new SecureRequestCustomizer();
				secureRequestCustomizer.setSniHostCheck(false);
				httpsConfig.addCustomizer(secureRequestCustomizer);
				final var sslConnector = new ServerConnector(
						server,
						new SslConnectionFactory(sslContextFactory, "http/1.1"),
						new HttpConnectionFactory(httpsConfig)
				);
				sslConnector.setPort(serverPort);
				server.addConnector(sslConnector);
			});
		}
		return factory;
	}

//	@Bean
//	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
//		return factory -> factory.addConnectorCustomizers(connector -> {
//
//			try {
//				// SPIFFE SSLContext
//				this.getSpiffeSSLContext();
//
//				// ProtocolHandler取得
//				Http11NioProtocol protocol =
//						(Http11NioProtocol) connector.getProtocolHandler();
//
//				protocol.setSSLEnabled(true);
//
//				// SSLHostConfig作成
//				SSLHostConfig sslHostConfig = new SSLHostConfig();
//				sslHostConfig.setProtocols("TLSv1.2,TLSv1.3");
//				sslHostConfig.setCertificateVerification("required");
//
//				// 証明書設定（ダミー的に必要）
//				SSLHostConfigCertificate cert =
//						new SSLHostConfigCertificate(
//								sslHostConfig,
//								SSLHostConfigCertificate.Type.UNDEFINED
//						);
//
//				sslHostConfig.addCertificate(cert);
//
//				// 既存設定をクリアして差し替え
//				protocol.clearSslHostConfigs();
//				protocol.addSslHostConfig(sslHostConfig);
//
//			} catch (Exception e) {
//				throw new IllegalStateException("Failed to configure SPIFFE TLS", e);
//			}
//		});
//	}

}
