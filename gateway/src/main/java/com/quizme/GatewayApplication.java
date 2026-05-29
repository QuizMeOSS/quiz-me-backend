package com.quizme;

import com.quizme.auth.AddTokenHeaderFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import javax.net.ssl.SSLContext;

import java.net.http.HttpClient;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;


@SpringBootApplication
public class GatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> myRoutes(@Value("${app.backend_url}") String backendUrl,
                                                   AddTokenHeaderFilter addTokenHeaderFilter) {
        return route("my_backend")
                .route(path("/**"), http())
                .before(uri(backendUrl))
                .filter(addTokenHeaderFilter)
                .build();
    }

    @Bean
    public ClientHttpRequestFactory gatewayClientHttpRequestFactory(SslBundles sslBundles) {
        SSLContext sslContext = sslBundles.getBundle("sslbundle").createSslContext();

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        return new JdkClientHttpRequestFactory(httpClient);
    }
}

