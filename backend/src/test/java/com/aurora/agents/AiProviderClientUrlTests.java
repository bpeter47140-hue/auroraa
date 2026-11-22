package com.aurora.agents;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderClientUrlTests {
    @Test
    void openRouterBaseUrlAndCompletionPathProduceOneVersionedEndpoint() {
        DefaultUriBuilderFactory uris = new DefaultUriBuilderFactory("https://openrouter.ai/api/v1");

        assertThat(uris.expand(AiProviderClient.COMPLETIONS_PATH).toString())
            .isEqualTo("https://openrouter.ai/api/v1/chat/completions");
    }
}
