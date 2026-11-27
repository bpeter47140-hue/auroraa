package com.aurora.projects;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class CreativeStudioServiceTests {
    private boolean valid(String type, byte[] bytes) throws Exception {
        CreativeStudioService service = new CreativeStudioService(null, null, null);
        Method method = CreativeStudioService.class.getDeclaredMethod("validSignature", String.class, byte[].class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, type, bytes);
    }

    @Test void acceptsSupportedImageAndVideoSignatures() throws Exception {
        assertTrue(valid("image/png", new byte[]{(byte)137,80,78,71,13,10,26,10}));
        assertTrue(valid("image/jpeg", new byte[]{(byte)255,(byte)216,(byte)255,0}));
        assertTrue(valid("image/gif", "GIF89a".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(valid("image/webp", "RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(valid("video/mp4", "0000ftypisom".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(valid("video/webm", new byte[]{0x1A,0x45,(byte)0xDF,(byte)0xA3}));
    }

    @Test void rejectsMismatchedAndUnsupportedContent() throws Exception {
        assertFalse(valid("image/png", "not a png".getBytes(StandardCharsets.UTF_8)));
        assertFalse(valid("image/jpeg", new byte[]{1,2,3,4}));
        assertFalse(valid("image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8)));
        assertFalse(valid("video/mp4", new byte[]{1,2,3,4}));
    }
}