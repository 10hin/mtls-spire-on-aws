package com.example.client5;

import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.SpiffeIdUtils;
import io.spiffe.workloadapi.DefaultX509Source;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.security.Security;
import java.util.Set;

@SpringBootApplication
public class Client5Application {

	static {
		Security.insertProviderAt(new SpiffeProvider(), 1);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Client5Application.class);

	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) {
		SpringApplication.run(Client5Application.class, args);
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
			@Value("${client5.backend.base-url}")
			final String backendBaseURL,
			@Value("${client5.spiffe.enabled}")
			final boolean spiffeEnabled,
			@Value("${client5.spiffe.allowed-ids:}")
			final String pipeSeparatedAllowedSPIFFEIDs,
			final RestClient.Builder restClientBuilder
	) {
		restClientBuilder.baseUrl(backendBaseURL);
		if (spiffeEnabled) {
			final Set<SpiffeId> allowedSPIFFEIDs = SpiffeIdUtils.toSetOfSpiffeIds(pipeSeparatedAllowedSPIFFEIDs);
			final var sslContext = this.getSpiffeSSLContext(allowedSPIFFEIDs);
			final var clientTLSStrategy = new DefaultClientTlsStrategy(sslContext);
			final var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
					.setTlsSocketStrategy(clientTLSStrategy)
					.build();
			final var httpClient = HttpClients.custom()
					.setConnectionManager(connectionManager)
					.build();
			restClientBuilder.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
		}
		return restClientBuilder.build();
	}

}
