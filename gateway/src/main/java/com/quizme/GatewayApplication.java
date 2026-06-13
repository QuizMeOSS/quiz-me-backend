package com.quizme;

import com.quizme.auth.AddAccessTokenCookieFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;


@SpringBootApplication
@EnableDiscoveryClient // for service discovery
public class GatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> myRoutes(AddAccessTokenCookieFilter addAccessTokenCookieFilter) {
        return route("my_backend")
                .route(path("/**"), http())
                .filter(lb("quizme")) // default load balancer uses RoundRobin strategy
                .filter(addAccessTokenCookieFilter)
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

