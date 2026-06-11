package com.digital.ethaccumulator;

import com.digital.ethaccumulator.pow.PoWSolver;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class PoWSolverTest {

    private final PoWSolver solver = new PoWSolver();

    @Test
    void solvesLowDifficulty() throws Exception {
        String challenge = "test-challenge-abc123";
        int difficulty = 10; // low difficulty — fast for tests

        PoWSolver.PoWSolution solution = solver.solve(challenge, difficulty);

        assertNotNull(solution);
        assertTrue(solution.nonce() >= 0);
        assertTrue(solution.durationMs() >= 0);

        // Verify the solution hash actually meets target
        String input = challenge + solution.nonce();
        byte[] hash = MessageDigest.getInstance("SHA-256")
            .digest(input.getBytes(StandardCharsets.UTF_8));

        // First 10 bits should be zero (1 full byte zero + top 2 bits of second byte)
        assertEquals(0, hash[0] & 0xFF);
        assertEquals(0, hash[1] & 0b11000000);
    }
}
