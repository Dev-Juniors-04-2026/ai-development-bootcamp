package com.example.todoapp.adapter.out.security;

import com.example.todoapp.domain.port.out.PasswordHasher;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Component;

/**
 * Out-adapter for {@link PasswordHasher} backed by argon2-jvm.
 *
 * <p>Uses Argon2id (the factory default) — the hybrid variant resistant to both
 * side-channel and GPU attacks. Domain code depends only on the port; swapping
 * to bcrypt/scrypt would only change this file.
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    // OWASP-recommended Argon2id baseline: ~50–100ms per hash on a typical server.
    // Slow enough to deter offline brute-force, fast enough that login isn't sluggish.
    private static final int ITERATIONS = 3;
    private static final int MEMORY_KIB = 65536; // 64 MiB per hash
    private static final int PARALLELISM = 1;

    // Argon2 instances are thread-safe; reuse one for the bean's lifetime.
    private final Argon2 argon2 = Argon2Factory.create();

    @Override
    public String hash(String raw) {
        // char[] (not String) so we can zero the plaintext from memory after use;
        // Strings are immutable and linger on the heap until GC.
        char[] chars = raw.toCharArray();
        try {
            // Returns an encoded string ($argon2id$v=19$m=...,t=...,p=...$salt$hash)
            // containing a random salt and the parameters — store this one value, no separate salt column.
            return argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, chars);
        } finally {
            // finally: scrub plaintext even if hash() throws.
            argon2.wipeArray(chars);
        }
    }

    @Override
    public boolean matches(String raw, String hash) {
        char[] chars = raw.toCharArray();
        try {
            // verify() parses salt + params from the encoded hash, re-hashes raw,
            // and compares in constant time (timing-attack resistant).
            return argon2.verify(hash, chars);
        } finally {
            argon2.wipeArray(chars);
        }
    }
}
