package com.example.client;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import io.spiffe.provider.SpiffeProvider;

@SpringBootApplication
public class ClientApplication {

	private static final String SPIFFE_ALGORITHM = "Spiffe";
	private static final String SPIFFE_PROVIDER = "Spiffe";
	private static final String SPIFFE_PROTOCOL = "Spiffe";

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

	@Bean
	@Profile("spiffe")
	SslBundleRegistrar sslBundleCustomizer() throws Exception {
		SpiffeProvider.install();
		final var keyManagerFactory = KeyManagerFactory.getInstance(SPIFFE_ALGORITHM, SPIFFE_PROVIDER);
		final var trustManagerFactory = TrustManagerFactory.getInstance(SPIFFE_ALGORITHM, SPIFFE_PROVIDER);
		final var spiffeSslBundle = SslBundle.of(null, null, null, SPIFFE_PROTOCOL, SslManagerBundle.of(keyManagerFactory, trustManagerFactory));
		return (registry) -> {
			registry.registerBundle("Spiffe", spiffeSslBundle);
		};
	}

}
