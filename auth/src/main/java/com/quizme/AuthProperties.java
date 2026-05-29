package com.quizme;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AuthProperties {
    private String frontendUrl;

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String url) {
        this.frontendUrl = url;
    }
}
