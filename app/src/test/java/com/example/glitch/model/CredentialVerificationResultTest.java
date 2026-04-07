package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Verifies value semantics of credential verification result.
 */
public class CredentialVerificationResultTest {

    @Test
    public void constructor_storesAllFields() {
        CredentialVerificationResult result = new CredentialVerificationResult(
                true,
                "ID-100",
                "Ayesha Khan",
                "Credential verified successfully."
        );

        assertTrue(result.isValid());
        assertEquals("ID-100", result.getIdentifier());
        assertEquals("Ayesha Khan", result.getHolderName());
        assertEquals("Credential verified successfully.", result.getMessage());
    }

    @Test
    public void constructor_handlesInvalidResult() {
        CredentialVerificationResult result = new CredentialVerificationResult(
                false,
                "ID-404",
                "",
                "Credential not found."
        );

        assertFalse(result.isValid());
        assertEquals("ID-404", result.getIdentifier());
        assertEquals("", result.getHolderName());
        assertEquals("Credential not found.", result.getMessage());
    }
}

