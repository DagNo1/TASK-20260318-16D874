package com.pettrade.practiceplatform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TlsProfileConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void tlsProfilePropertiesCanBeResolved() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=tls",
                        "server.ssl.enabled=true",
                        "TLS_KEYSTORE_PATH=classpath:tls/test-keystore.p12",
                        "TLS_KEYSTORE_PASSWORD=changeit",
                        "server.ssl.key-store=${TLS_KEYSTORE_PATH}",
                        "server.ssl.key-store-password=${TLS_KEYSTORE_PASSWORD}"
                )
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("server.ssl.enabled")).isEqualTo("true");
                    assertThat(context.getEnvironment().getProperty("server.ssl.key-store")).isEqualTo("classpath:tls/test-keystore.p12");
                });
    }

    @Test
    void devProfileCanKeepSslDisabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "server.ssl.enabled=false"
                )
                .run(context -> assertThat(context.getEnvironment().getProperty("server.ssl.enabled")).isEqualTo("false"));
    }
}
