package io.github.xiaotang.novartc.server.util;

import io.github.xiaotang.novartc.server.config.MinioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MinioUrlResolverTest {
    private MinioProperties properties;
    private MinioUrlResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setEndpoint("http://minio:9000");
        properties.setUrlPrefix("https://files.example.com/");
        properties.setBucket("novartc");
        resolver = new MinioUrlResolver(properties);
    }

    @Test
    void shouldResolveObjectKeyFromPublicUrl() {
        assertThat(resolver.resolveObjectKey(
                "http://localhost:19000/novartc/file/20260907/a.pdf"))
                .isEqualTo("file/20260907/a.pdf");
        assertThat(resolver.resolveObjectKey("file/20260907/a.pdf"))
                .isEqualTo("file/20260907/a.pdf");
        assertThat(resolver.resolveObjectKey("https://example.com/other/file/a.pdf"))
                .isNull();
    }

    @Test
    void shouldResolveObjectKeyWithPublicPrefix() {
        assertThat(resolver.resolve("avatar/20260907/a.jpg"))
                .isEqualTo("https://files.example.com/novartc/avatar/20260907/a.jpg");
    }

    @Test
    void shouldKeepLegacyAbsoluteUrlDuringMigration() {
        assertThat(resolver.resolve("http://localhost:9000/novartc/avatar/a.jpg"))
                .isEqualTo("http://localhost:9000/novartc/avatar/a.jpg");
    }
}
