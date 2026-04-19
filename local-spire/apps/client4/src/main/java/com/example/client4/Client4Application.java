package com.example.client4;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.provider.SpiffeTrustManager;
import io.spiffe.provider.SpiffeTrustManagerFactory;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.SpiffeIdUtils;
import io.spiffe.workloadapi.DefaultX509Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@SpringBootApplication
public class Client4Application {

	static {
		Security.insertProviderAt(new SpiffeProvider(), 1);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Client4Application.class);

	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) {
		SpringApplication.run(Client4Application.class, args);
	}

	private SslContext getSpiffeSSLContextForNetty(
			final Set<SpiffeId> allowedSPIFFEIDs
	) {
		try {
			final var keyManagerFactory = KeyManagerFactory.getInstance("Spiffe", "Spiffe");
			final var trustManager = new SpiffeTrustManager(
					DefaultX509Source.newSource(),
					() -> allowedSPIFFEIDs
			);
			return SslContextBuilder.forClient()
					.sslProvider(SslProvider.JDK)
					.keyManager(keyManagerFactory)
					.trustManager(trustManager)
					.build();
		} catch (final Exception e) {
			throw new InternalError("SSLContext initialization failed", e);
		}
	}

	@Bean
	public WebClient backendClient(
			@Value("${client4.backend.base-url}")
			final String backendBaseURL,
			@Value("${client4.spiffe.enabled}")
			final boolean spiffeEnabled,
			@Value("${client4.spiffe.allowed-ids:}")
			final String pipeSeparatedAllowedSPIFFEIDs,
			final WebClient.Builder webClientBuilder
	) {
		webClientBuilder.baseUrl(backendBaseURL);
		if (spiffeEnabled) {
			final Set<SpiffeId> allowedSPIFFEIDs = SpiffeIdUtils.toSetOfSpiffeIds(pipeSeparatedAllowedSPIFFEIDs);
			final var nettySSLContext = this.getSpiffeSSLContextForNetty(allowedSPIFFEIDs);
			final var httpClient = HttpClient.create()
							.secure(spec -> spec.sslContext(nettySSLContext));
			webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
		}
		return webClientBuilder.build();
	}

}
