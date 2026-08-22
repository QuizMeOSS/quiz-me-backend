package com.quizme;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Auth auth = new Auth();
    private final Kafka kafka = new Kafka();

    public Auth getAuth() {
        return auth;
    }

    public Kafka getKafka() {
        return kafka;
    }


    public static class Auth {
        private String pepper;
        private Jwt jwt = new Jwt();

        public String getPepper() {
            return pepper;
        }

        public void setPepper(String pepper) {
            this.pepper = pepper;
        }

        public Jwt getJwt() {
            return jwt;
        }

    }

    public static class Jwt {

        private String secret;
        private long accessTokenDuration;
        private long refreshTokenDuration;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenDuration() {
            return accessTokenDuration;
        }

        public void setAccessTokenDuration(long duration) {
            this.accessTokenDuration = duration;
        }

        public long getRefreshTokenDuration() {
            return refreshTokenDuration;
        }

        public void setRefreshTokenDuration(long duration) {
            this.refreshTokenDuration = duration;
        }
    }

    public static class Kafka {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
