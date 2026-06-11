package com.digital.ethaccumulator;

import com.digital.ethaccumulator.pow.Argon2Solver;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class Argon2SolverTest {

    private final Argon2Solver solver = new Argon2Solver();

    @Test
    void difficultyMaskMatchesFaucetFormula() {
        // difficulty 11 → byteCount 2, bitCount 3, maxValue 2^5=32=0x20 → "0020"
        assertEquals("0020", solver.difficultyMask(11));
        // difficulty 8 → byteCount 2, bitCount 0, maxValue 2^8=256=0x100 → padded "0100"
        assertEquals("0100", solver.difficultyMask(8));
    }

    @Test
    void hashIsDeterministic() {
        byte[] preimage = Base64.getDecoder().decode("AQIDBAUGBwg="); // 8 bytes
        Argon2Solver.Params p = new Argon2Solver.Params(0, 19, 4, 4096, 1, 16, 8);

        String h1 = solver.hash(42L, preimage, p);
        String h2 = solver.hash(42L, preimage, p);

        assertEquals(h1, h2, "same nonce+preimage must yield same hash");
        assertEquals(32, h1.length(), "16-byte hash = 32 hex chars");
    }

    @Test
    void minesValidShareAtLowDifficulty() {
        byte[] preimage = Base64.getDecoder().decode("AQIDBAUGBwg=");
        // very low difficulty so the test is fast
        Argon2Solver.Params p = new Argon2Solver.Params(0, 19, 1, 256, 1, 16, 6);

        Argon2Solver.Solution sol = solver.mine(preimage, p, 0, 1,
            new AtomicBoolean(false), new AtomicLong(0));

        assertNotNull(sol);
        assertTrue(solver.meetsDifficulty(sol.hashHex(), solver.difficultyMask(p.difficulty())));
    }
}
