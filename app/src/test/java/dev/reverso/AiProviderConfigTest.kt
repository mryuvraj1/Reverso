package dev.reverso

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiProviderConfigTest {
    @Test fun providerDefaultsMatchTheirProtocols() {
        assertEquals("https://api.openai.com/v1/chat/completions", AiProvider.OPENAI.defaultEndpoint)
        assertEquals(Protocol.ANTHROPIC, AiProvider.ANTHROPIC.protocol)
        assertEquals(Protocol.OPENAI_COMPATIBLE, AiProvider.OPENROUTER.protocol)
    }

    @Test fun configurationRequiresSecureEndpointAndModel() {
        assertNull(AiProviderConfig(AiProvider.OPENAI, AiProvider.OPENAI.defaultEndpoint, "gpt-4o-mini").validationError())
        assertEquals("Enter a valid HTTPS API endpoint", AiProviderConfig(AiProvider.CUSTOM, "", "model").validationError())
        assertEquals("The API endpoint must use HTTPS and cannot contain credentials, query parameters, or a fragment", AiProviderConfig(AiProvider.CUSTOM, "http://example.com/api", "model").validationError())
        assertEquals("Enter a model name", AiProviderConfig(AiProvider.CUSTOM, "https://example.com/api", " ").validationError())
    }

    @Test fun endpointCannotEmbedSecretsOrQueryCredentials() {
        val withUserInfo = AiProviderConfig(AiProvider.CUSTOM, "https://user:secret@example.com/api", "model")
        val withQuery = AiProviderConfig(AiProvider.CUSTOM, "https://example.com/api?key=secret", "model")
        assertEquals("The API endpoint must use HTTPS and cannot contain credentials, query parameters, or a fragment", withUserInfo.validationError())
        assertEquals("The API endpoint must use HTTPS and cannot contain credentials, query parameters, or a fragment", withQuery.validationError())
    }

    @Test fun sourceContextIsOnlyAddedToTheExplicitlyAttachedMessage() {
        val plain = AiMessage("user", "Explain this")
        val attached = AiMessage("user", "Explain this", "Active file: Main.kt")
        assertEquals("Explain this", plain.requestContent())
        assertEquals("Explain this\n\nActive file: Main.kt", attached.requestContent())
    }
}
