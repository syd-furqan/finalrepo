package com.example.glitch.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Verifies domain-specific auth error normalization behavior.
 */
public class FirebaseAuthRepositoryTest {

    @Test
    public void normalizeAuthError_returnsNetworkMessage() throws Exception {
        FirebaseAuthRepository repository = new FirebaseAuthRepository(null, null);

        String message = invokeNormalizeAuthError(repository, new Exception("A network error timeout"));

        assertEquals("Network timeout. Check emulator/device internet, then retry.", message);
    }

    @Test
    public void normalizeAuthError_returnsFallbackWhenErrorMissing() throws Exception {
        FirebaseAuthRepository repository = new FirebaseAuthRepository(null, null);

        String message = invokeNormalizeAuthError(repository, null);

        assertEquals("Login failed. Please verify credentials.", message);
    }

    @Test
    public void normalizeAuthError_passesThroughRawMessageForGenericErrors() throws Exception {
        FirebaseAuthRepository repository = new FirebaseAuthRepository(null, null);

        String message = invokeNormalizeAuthError(repository, new Exception("Wrong password"));

        assertEquals("Wrong password", message);
    }

    private String invokeNormalizeAuthError(FirebaseAuthRepository repository, Exception error)
            throws Exception {
        Method method = FirebaseAuthRepository.class
                .getDeclaredMethod("normalizeAuthError", Exception.class);
        method.setAccessible(true);
        return (String) method.invoke(repository, error);
    }
}
