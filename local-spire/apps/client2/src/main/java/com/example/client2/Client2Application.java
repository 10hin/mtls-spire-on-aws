package com.example.client2;

import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.SpiffeIdUtils;
import io.spiffe.workloadapi.DefaultX509Source;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.security.Security;
import java.util.Set;

@SpringBootApplication
public class Client2Application {

	static {
		Security.insertProviderAt(new SpiffeProvider(), 1);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Client2Application.class);

	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) {
		SpringApplication.run(Client2Application.class, args);
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
	public RestClient backendClient(
			@Value("${client2.backend.base-url}")
			final String backendBaseURL,
			@Value("${client2.spiffe.enabled}")
			final boolean spiffeEnabled,
			@Value("${client2.spiffe.allowed-ids:}")
			final String pipeSeparatedAllowedSPIFFEIDs,
			final RestClient.Builder restClientBuilder
	) {
		restClientBuilder.baseUrl(backendBaseURL);
		final var clientConnector = new ClientConnector();
		if (spiffeEnabled) {
			final Set<SpiffeId> allowedSPIFFEIDs = SpiffeIdUtils.toSetOfSpiffeIds(pipeSeparatedAllowedSPIFFEIDs);
			final var sslContext = this.getSpiffeSSLContext(allowedSPIFFEIDs);
			final var sslContextFactory = new SslContextFactory.Client();
			sslContextFactory.setSslContext(sslContext);
			sslContextFactory.setEndpointIdentificationAlgorithm(null);
			sslContextFactory.setTrustAll(true);
			clientConnector.setSslContextFactory(sslContextFactory);
		}
		final var httpClient = new HttpClient(new HttpClientTransportDynamic(clientConnector));
		restClientBuilder.requestFactory(new JettyClientHttpRequestFactory(httpClient));
		return restClientBuilder.build();
	}

}
