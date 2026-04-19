package com.example.server2;

import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.SpiffeIdUtils;
import io.spiffe.workloadapi.DefaultX509Source;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLContext;
import java.security.Security;
import java.util.Set;

@SpringBootApplication
public class Server2Application {
	static {
		Security.insertProviderAt(new SpiffeProvider(), 1);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Server2Application.class);

	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) {
		SpringApplication.run(Server2Application.class, args);
	}

	private SSLContext getSpiffeSSLContext(
			final Set<SpiffeId> allowedSPIFFEIDs
	) {
		try {
			final var spiffeSslContextOptions = SpiffeSslContextFactory.SslContextOptions.builder()
					.sslProtocol(SPIFFE_PROTOCOL)
					.x509Source(DefaultX509Source.newSource())
					.acceptedSpiffeIdsSupplier(() -> allowedSPIFFEIDs)
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
			final boolean spiffeEnabled,
			@Value("${server2.spiffe.allowed-ids:}")
			final String pipeSeparatedAllowedSPIFFEIDs
	) {
		final var factory = new JettyServletWebServerFactory();
		if (spiffeEnabled) {
			final Set<SpiffeId> allowedSPIFFEIDs = SpiffeIdUtils.toSetOfSpiffeIds(pipeSeparatedAllowedSPIFFEIDs);
			final var spiffeSSLContext = this.getSpiffeSSLContext(allowedSPIFFEIDs);
			factory.addServerCustomizers(server -> {
				for (final var connector : server.getConnectors()) {
					server.removeConnector(connector);
				}
				final var sslContextFactory = new SslContextFactory.Server();
				sslContextFactory.setSslContext(spiffeSSLContext);
				sslContextFactory.setNeedClientAuth(true);
//				sslContextFactory.setSniRequired(false);
				LOGGER.info("sslContextFactory.isSniRequired(): {}", sslContextFactory.isSniRequired());
//				sslContextFactory.setEndpointIdentificationAlgorithm(null);
				LOGGER.info("sslContextFactory.getEndpointIdentificationAlgorithm(): {}", sslContextFactory.getEndpointIdentificationAlgorithm());
				final var httpsConfig = new HttpConfiguration();
				final var secureRequestCustomizer = new SecureRequestCustomizer();
//				secureRequestCustomizer.setSniHostCheck(false);
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

}
