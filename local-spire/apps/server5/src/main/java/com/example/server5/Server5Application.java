package com.example.server5;

import io.spiffe.provider.SpiffeKeyManagerFactory;
import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.provider.SpiffeTrustManagerFactory;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.SpiffeIdUtils;
import io.spiffe.workloadapi.DefaultX509Source;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLContext;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.Security;
import java.util.Set;
import java.util.function.Supplier;

@SpringBootApplication
public class Server5Application {

	static {
		Security.insertProviderAt(new SpiffeProvider(), 1);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Server5Application.class);

	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) {
		SpringApplication.run(Server5Application.class, args);
	}

	private SSLContext getSpiffeSSLContext(
			final Set<SpiffeId> allowedSPIFFEIDs
	) {
		try {
			final var x509Source = DefaultX509Source.newSource();
			final Supplier<Set<SpiffeId>> allowedSPIFFEIDSupplier = () -> allowedSPIFFEIDs;
			final var spiffeSslContextOptions = SpiffeSslContextFactory.SslContextOptions.builder()
					.sslProtocol(SPIFFE_PROTOCOL)
					.x509Source(x509Source)
					.acceptedSpiffeIdsSupplier(allowedSPIFFEIDSupplier)
					.build();
			final var jsseSSLContext = SpiffeSslContextFactory.getSslContext(spiffeSslContextOptions);
			return SSLUtil.createSSLContext(
					jsseSSLContext,
					(X509KeyManager) new SpiffeKeyManagerFactory().engineGetKeyManagers(x509Source)[0],
					(X509TrustManager) new SpiffeTrustManagerFactory().engineGetTrustManagers(x509Source, allowedSPIFFEIDSupplier)[0]
			);
		} catch (final Exception e) {
			throw new InternalError("SSLContext initialization failed", e);
		}
	}

	@Bean
	@ConditionalOnBooleanProperty("server5.spiffe.enabled")
	public WebServerFactoryCustomizer<TomcatWebServerFactory> tomcatWebServerFactoryCustomizerForSpiffe(
			@Value("${server5.spiffe.allowed-ids}")
			final String pipeSeparatedAllowedSPIFFEIDs
	) {
		final Set<SpiffeId> allowedSPIFFEIDs = SpiffeIdUtils.toSetOfSpiffeIds(pipeSeparatedAllowedSPIFFEIDs);
		final var spiffeSSLContext = this.getSpiffeSSLContext(allowedSPIFFEIDs);
		return factory -> {
			LOGGER.info("WebServerFactoryCustomizer<TomcatWebServerFactory> called!");
			factory.addConnectorCustomizers(connector -> {
				LOGGER.info("TomcatConnectorCustomizer called!");
				final var protocol = (Http11NioProtocol) connector.getProtocolHandler();
				protocol.setSSLEnabled(true);
				final var sslHostConfig = new SSLHostConfig();
				sslHostConfig.setProtocols("TLSv1.2,TLSv1.3");
				sslHostConfig.setCertificateVerification("required");
				final var cert = new SSLHostConfigCertificate(
						sslHostConfig,
						SSLHostConfigCertificate.Type.UNDEFINED
				);
				cert.setSslContext(spiffeSSLContext);
				sslHostConfig.addCertificate(cert);
				protocol.addSslHostConfig(sslHostConfig);
			});
		};
	}

}
