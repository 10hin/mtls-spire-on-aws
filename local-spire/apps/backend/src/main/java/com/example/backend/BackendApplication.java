package com.example.backend;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.autoconfigure.web.servlet.TomcatServletWebServerFactoryCustomizer;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;

import io.spiffe.provider.SpiffeProvider;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.provider.SpiffeSslContextFactory.SslContextOptions;
import io.spiffe.workloadapi.DefaultX509Source;
import io.spiffe.workloadapi.DefaultX509Source.X509SourceOptions;


@SpringBootApplication
public class BackendApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(BackendApplication.class);

	private static final String SPIFFE_ALGORITHM = "Spiffe";
	private static final String SPIFFE_PROVIDER = "Spiffe";
	// private static final String SPIFFE_PROTOCOL = "TLSv1.2";
	private static final String SPIFFE_PROTOCOL = "TLS";

	public static void main(String[] args) throws Exception {
		final var securityPropertyLocation = System.getProperty("java.security.properties");
		System.out.println("securityPropertyLocation = " + securityPropertyLocation);
		Files.lines(Paths.get(securityPropertyLocation)).forEach(System.out::println);
		final var providerClass = Security.getProperty("security.provider.2");
		System.out.println("proiderClass = " + providerClass);

		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	@Profile("spiffe")
	SslBundleRegistrar sslBundleCustomizer() throws Exception {

		if (Files.exists(Paths.get("/run/"))) {
			System.out.println("EXISTS /run/");
		} else {
			System.out.println("NOT EXISTS /run/");
		}
		if (Files.exists(Paths.get("/run/spire/"))) {
			System.out.println("EXISTS /run/spire/");
		} else {
			System.out.println("NOT EXISTS /run/spire/");
		}
		if (Files.exists(Paths.get("/run/spire/sockets/"))) {
			System.out.println("EXISTS /run/spire/sockets/");
			System.out.println("ls /run/spire/sockets/: " + Files.list(Paths.get("/run/spire/sockets/")).toList());
		} else {
			System.out.println("NOT EXISTS /run/spire/sockets/");
		}
		if (Files.exists(Paths.get("/run/spire/sockets/spire-agent.sock"))) {
			System.out.println("EXISTS /run/spire/sockets/spire-agent.sock");
		} else {
			System.out.println("NOT EXISTS /run/spire/sockets/spire-agent.sock");
		}
		SpiffeProvider.install();
		final var keyManagerFactory = KeyManagerFactory.getInstance(SPIFFE_ALGORITHM, SPIFFE_PROVIDER);
		final var trustManagerFactory = TrustManagerFactory.getInstance(SPIFFE_ALGORITHM, SPIFFE_PROVIDER);
		final var spiffeSslBundle = SslBundle.of(null, null, null, SPIFFE_PROTOCOL, SslManagerBundle.of(keyManagerFactory, trustManagerFactory));

		final SslBundle spiffeSslBundleWrapper = new SslBundle() {

			@Override
			public SslStoreBundle getStores() {
				return spiffeSslBundle.getStores();
			}

			@Override
			public SslBundleKey getKey() {
				return spiffeSslBundle.getKey();
			}

			@Override
			public SslOptions getOptions() {
				return spiffeSslBundle.getOptions();
			}

			@Override
			public String getProtocol() {
				return spiffeSslBundle.getProtocol();
			}

			@Override
			public SslManagerBundle getManagers() {
				return spiffeSslBundle.getManagers();
			}

			@Override
			public SSLContext createSslContext() {
				try {
					final var srcOpts = X509SourceOptions.builder()
						.spiffeSocketPath("unix:/run/spire/sockets/spire-agent.sock")
						.build();
					SslContextOptions opts = SslContextOptions.builder()
						.x509Source(DefaultX509Source.newSource(srcOpts))
						.acceptAnySpiffeId()
						.build();
					return SpiffeSslContextFactory.getSslContext(opts);
				} catch (final Exception ex) {
					throw new InternalError(ex);
				}
			}

		};

		return (registry) -> {
			registry.registerBundle("Spiffe", spiffeSslBundleWrapper);
		};
	}

	@Bean
	@Profile("spiffe")
	WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer(final SslBundles bundles) {
		System.out.println("===== tomcatCustomizer called");
		return factory -> {
			System.out.println("===== WebServerFactoryCustomizer called");
			factory.setProtocol(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
			factory.addConnectorCustomizers(connector -> {
				System.out.println("===== ConnectorCustomizer called");
				final var spiffeBundle = bundles.getBundle("Spiffe");
				if (spiffeBundle == null) {
					throw new InternalError("SPIFFE Bundle not found!");
				}
				final var spiffeManagerBundle = spiffeBundle.getManagers();
				final var spiffeKeyManagerFactory = spiffeManagerBundle.getKeyManagerFactory();
				final var spiffeTrustManagerFactory = spiffeManagerBundle.getTrustManagerFactory();

				final var sslHostConfig = new SSLHostConfig();
				final var certificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
				certificate.setSslContext(this.createTomcatSSLContext(spiffeBundle.createSslContext(), spiffeKeyManagerFactory, spiffeTrustManagerFactory));
				sslHostConfig.addCertificate(certificate);
				connector.addSslHostConfig(sslHostConfig);
				connector.setSecure(true);
				connector.setPort(8443);
				connector.setScheme("https");
				final var handler = connector.getProtocolHandler();
				if (handler instanceof AbstractHttp11Protocol) {
					final var http11Handler = (AbstractHttp11Protocol<?>) handler;
					http11Handler.setSSLEnabled(true);
				} else {
					LOGGER.info("protocol handler class is not AbstractHttp11Protocol: {}", handler.getClass());
				}
			});
			factory.setSsl(null);
			factory.setSslBundles(null);
		};
	}

	private org.apache.tomcat.util.net.SSLContext createTomcatSSLContext(final SSLContext sslctx, final KeyManagerFactory keyManagerFactory, final TrustManagerFactory trustManagerFactory) {
		return new org.apache.tomcat.util.net.SSLContext() {

			@Override
			public void init(KeyManager[] kms, TrustManager[] tms, SecureRandom sr) throws KeyManagementException {
				sslctx.init(kms, tms, sr);
			}

			@Override
			public void destroy() {
				// TODO やるならSSLContextの裏にあるはずのX509Sourceのcloseとかだけどとりあえず無視
			}

			@Override
			public SSLSessionContext getServerSessionContext() {
				return sslctx.getServerSessionContext();
			}

			@Override
			public SSLEngine createSSLEngine() {
				return sslctx.createSSLEngine();
			}

			@Override
			public SSLServerSocketFactory getServerSocketFactory() {
				return sslctx.getServerSocketFactory();
			}

			@Override
			public SSLParameters getSupportedSSLParameters() {
				return sslctx.getSupportedSSLParameters();
			}

			@Override
			public X509Certificate[] getCertificateChain(String alias) {
				final var managers = keyManagerFactory.getKeyManagers();
				return Arrays.stream(managers)
					.filter(manager -> (manager instanceof X509KeyManager))
					.map(manager -> ((X509KeyManager) manager))
					.flatMap(manager -> Arrays.stream(manager.getCertificateChain(alias)))
					.toArray(X509Certificate[]::new);
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				final var managers = trustManagerFactory.getTrustManagers();
				return Arrays.stream(managers)
					.filter(manager -> (manager instanceof X509TrustManager))
					.map(manager -> ((X509TrustManager) manager))
					.flatMap(manager -> Arrays.stream(manager.getAcceptedIssuers()))
					.toArray(X509Certificate[]::new);
			}
		};
	}

	@Bean
	// @Profile("spiffe")
	CommandLineRunner bundleCheck(SslBundles sslBundles, WebServerFactory ignoredFactory) {
		return args -> {
			System.out.println("===== all bundles =====");
			for (final var bundleName : sslBundles.getBundleNames()) {
				System.out.println("  - " + bundleName);
			}
			System.out.println("-----==========-----");
		};
	}


}
