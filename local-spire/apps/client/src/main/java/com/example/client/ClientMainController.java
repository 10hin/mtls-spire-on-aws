package com.example.client;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ClientMainController {

    private final String backendBaseURL;
    private final RestTemplate backendClient;

    public ClientMainController(
        @Value("${client.backend.base_url}")
        final String baseURL,
        final RestTemplateBuilder builder,
        final SslBundles sslBundles
    ) {
        this.backendBaseURL = baseURL;
        builder.rootUri(this.backendBaseURL);
        if (sslBundles.getBundleNames().contains("Spiffe")) {
            final var spiffeBundle = sslBundles.getBundle("Spiffe");
            builder.sslBundle(spiffeBundle);
        }
        this.backendClient = builder.build();
    }

    @RequestMapping(method={RequestMethod.GET,RequestMethod.POST}, path="/hello")
    public String hello() {
        return "Hello, this is client";
    }

    @PostMapping("/backend")
    public BackendResponse backend(
        @RequestBody BackendRequest req
    ) {
        if (Optional.ofNullable(req.getUrl()).orElse("").startsWith(this.backendBaseURL)) {
            final HttpMethod method = HttpMethod.valueOf(Optional.ofNullable(req.getMethod()).orElse("GET"));
            return this.backendClient.execute(
                req.getUrl(),
                method,
                (rawReq) -> {
                    if (req.getContentType() != null) {
                        try {
                            final var contentType = MediaType.parseMediaType(req.getContentType());
                            rawReq.getHeaders().setContentType(contentType);
                        } catch (InvalidMediaTypeException ignored) {
                            // ignore request content-type field and keep automatically selected content-type
                        }
                    }
                    this.backendClient.httpEntityCallback(req.getBody()).doWithRequest(rawReq);
                },
                (rawResp) -> {
                    final var resp = new BackendResponse();
                    resp.setUrl(req.getUrl());
                    resp.setStatus(rawResp.getStatusCode().value());
                    resp.setContentType(rawResp.getHeaders().getContentType().toString());
                    resp.setBody(new String(rawResp.getBody().readAllBytes(), StandardCharsets.UTF_8));
                    return resp;
                }
            );
        }
        return new BackendResponse();
    }

}
