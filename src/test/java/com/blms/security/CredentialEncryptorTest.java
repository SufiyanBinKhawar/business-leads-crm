package com.blms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** UC02 special requirement: SMTP password is never stored in plain text. */
class CredentialEncryptorTest {

    @Test
    void roundTripAndNotPlainText() {
        CredentialEncryptor enc = new CredentialEncryptor("test-key");
        String cipher = enc.encrypt("smtp-secret");
        assertNotEquals("smtp-secret", cipher);
        assertEquals("smtp-secret", enc.decrypt(cipher));
    }
}
