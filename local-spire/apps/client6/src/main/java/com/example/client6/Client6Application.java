package com.example.client6;

import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.SpiffeIdUtils;
import io.spiffe.workloadapi.DefaultX509Source;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLContext;
import java.security.Security;
import java.util.Set;

@SpringBootApplication
public class Client6Application {

	static {
		Security.insertProviderAt(new SpiffeProvider(), 1);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Client6Application.class);

	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) {
		SpringApplication.run(Client6Application.class, args);
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
	public WebClient backendClient(
			@Value("${client6.backend.base-url}")
			final String backendBaseURL,
			@Value("${client6.spiffe.enabled}")
			final boolean spiffeEnabled,
			@Value("${client6.spiffe.allowed-ids:}")
			final String pipeSeparatedAllowedSPIFFEIDs,
			final WebClient.Builder webClientBuilder
	) {
		webClientBuilder.baseUrl(backendBaseURL);
		if (spiffeEnabled) {
			final Set<SpiffeId> allowedSPIFFEIDs = SpiffeIdUtils.toSetOfSpiffeIds(pipeSeparatedAllowedSPIFFEIDs);
			final var sslContext = this.getSpiffeSSLContext(allowedSPIFFEIDs);
			final var clientTLSStrategy = new BasicClientTlsStrategy(sslContext);
			final var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
					.setTlsStrategy(clientTLSStrategy)
					.build();
			final var httpClient = HttpAsyncClients.custom()
					.setConnectionManager(connectionManager)
					.build();
			;
			webClientBuilder.clientConnector(new HttpComponentsClientHttpConnector(httpClient));
		}
		return webClientBuilder.build();
	}

}
