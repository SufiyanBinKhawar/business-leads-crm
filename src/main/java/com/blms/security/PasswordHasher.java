package com.blms.security;

/** D5 Figure 2 «interface» «Pure Fabrication» PasswordHasher - Protected Variations over the hashing algorithm. */
public interface PasswordHasher {
    boolean matches(String raw, String hash);
}
