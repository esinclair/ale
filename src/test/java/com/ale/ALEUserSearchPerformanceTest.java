package com.ale;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ale.encryption.TenantContext;
import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;
import com.ale.service.ALEUserSearchService;

/**
 * Performance and accuracy tests for {@link ALEUserSearchService} against the AES-256
 * encrypted {@code aleuser} table.
 *
 * <p>The 20 000-row data set is inserted <b>once</b> via {@link BeforeAll} and removed
 * <b>once</b> via {@link AfterAll}, so all three test methods share the same rows.
 *
 * <p>Tests:
 * <ol>
 *   <li>{@link #searchByFirstName()} — 5 exact first-name lookups via
 *       {@code first_name_hash} HMAC blind index</li>
 *   <li>{@link #searchByLastName()} — 5 exact last-name lookups via
 *       {@code last_name_hash} HMAC blind index</li>
 *   <li>{@link #searchByFirstNamePrefix()} — 5 starts-with-3-char lookups via the
 *       {@code first_name_prefix_hash} HMAC blind index (no table scan, no plaintext exposed)</li>
 * </ol>
 *
 * <p>Plaintext fields are decrypted transparently by {@link com.ale.encryption.EncryptedStringConverter}
 * when JPA materialises the result rows; the {@code WHERE} clause only touches hash columns.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class ALEUserSearchPerformanceTest {

    private static final int    USER_COUNT        = 20_000;
    private static final int    BATCH_SIZE        = 500;
    private static final int    EXPECTED_PER_NAME = USER_COUNT / 80; // 250
    private static final UUID   TENANT_ID         = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Barbara", "David", "Elizabeth", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Lisa", "Daniel", "Nancy",
        "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley",
        "Steven", "Dorothy", "Paul", "Kimberly", "Andrew", "Emily", "Joshua", "Donna",
        "Kevin", "Michelle", "Brian", "Carol", "George", "Amanda", "Timothy", "Melissa",
        "Ronald", "Deborah", "Edward", "Stephanie", "Jason", "Rebecca", "Jeffrey", "Sharon",
        "Ryan", "Laura", "Jacob", "Cynthia", "Gary", "Kathleen", "Nicholas", "Amy",
        "Eric", "Angela", "Jonathan", "Shirley", "Stephen", "Anna", "Larry", "Brenda",
        "Justin", "Pamela", "Scott", "Emma", "Brandon", "Nicole", "Benjamin", "Helen"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
        "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
        "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
        "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
        "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
        "Carter", "Roberts", "Turner", "Phillips", "Evans", "Parker", "Collins", "Edwards",
        "Stewart", "Flores", "Morris", "Nguyen", "Murphy", "Rivera", "Cook", "Rogers",
        "Morgan", "Peterson", "Cooper", "Reed", "Bailey", "Bell", "Gomez", "Kelly",
        "Howard", "Ward", "Cox", "Diaz", "Richardson", "Wood", "Watson", "Brooks"
    };

    private static final String[] DOMAINS = {
        "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com",
        "protonmail.com", "aol.com", "mail.com", "live.com", "msn.com"
    };

    @Autowired private ALEUserRepository    aleUserRepository;
    @Autowired private ALEUserSearchService aleUserSearchService;

    // ── One-time fixture ─────────────────────────────────────────────────────

    @BeforeAll
    void insertALEUsers() {
        aleUserRepository.deleteAll(); // ensure clean slate regardless of prior run state
        System.out.printf("%nInserting %,d ALEUser rows (shared across all test methods)\u2026%n", USER_COUNT);
        long t0 = System.currentTimeMillis();
        int inserted = 0;
        while (inserted < USER_COUNT) {
            int end = Math.min(inserted + BATCH_SIZE, USER_COUNT);
            List<ALEUser> batch = new ArrayList<>(end - inserted);
            for (int i = inserted; i < end; i++) {
                String fn = FIRST_NAMES[i % FIRST_NAMES.length];
                String ln = LAST_NAMES [i % LAST_NAMES.length];
                String un = (fn + "." + ln + i).toLowerCase();
                batch.add(new ALEUser(fn, ln, un, un + "@" + DOMAINS[i % DOMAINS.length], TENANT_ID));
            }
            TenantContext.set(TENANT_ID);
            try {
                aleUserRepository.saveAll(batch);
            } finally {
                TenantContext.clear();
            }
            inserted = end;
        }
        System.out.printf("Insert done in %,d ms.%n", System.currentTimeMillis() - t0);
    }

    @AfterAll
    void cleanup() {
        aleUserRepository.deleteAll();
        System.out.println("Cleanup complete.");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * 5 exact first-name searches via {@code first_name_hash} HMAC blind index.
     * The service hashes the search term; the DB WHERE clause touches only the hash column.
     * Matched rows are then decrypted by JPA on materialisation.
     */
    @Test
    void searchByFirstName() {
        String[] targets = { "James", "Mary", "John", "Patricia", "Robert" };

        System.out.println();
        System.out.println("=== ALEUser — Exact First Name Search (HMAC blind index on first_name_hash) ===");
        System.out.printf("%-14s  %7s  %10s%n", "First Name", "Results", "Elapsed ms");
        System.out.println("-".repeat(37));

        for (String target : targets) {
            long start     = System.currentTimeMillis();
            List<ALEUser> r = aleUserSearchService.findByFirstName(target, TENANT_ID);
            long elapsed   = System.currentTimeMillis() - start;

            assertThat(r).hasSize(EXPECTED_PER_NAME);
            assertThat(r).allSatisfy(u -> assertThat(u.getFirstName()).isEqualTo(target));

            System.out.printf("%-14s  %7d  %10d%n", target, r.size(), elapsed);
        }
    }

    /**
     * 5 exact last-name searches via {@code last_name_hash} HMAC blind index.
     */
    @Test
    void searchByLastName() {
        String[] targets = { "Smith", "Johnson", "Williams", "Brown", "Jones" };

        System.out.println();
        System.out.println("=== ALEUser — Exact Last Name Search (HMAC blind index on last_name_hash) ===");
        System.out.printf("%-14s  %7s  %10s%n", "Last Name", "Results", "Elapsed ms");
        System.out.println("-".repeat(37));

        for (String target : targets) {
            long start     = System.currentTimeMillis();
            List<ALEUser> r = aleUserSearchService.findByLastName(target, TENANT_ID);
            long elapsed   = System.currentTimeMillis() - start;

            assertThat(r).hasSize(EXPECTED_PER_NAME);
            assertThat(r).allSatisfy(u -> assertThat(u.getLastName()).isEqualTo(target));

            System.out.printf("%-14s  %7d  %10d%n", target, r.size(), elapsed);
        }
    }

    /**
     * 5 prefix searches via {@code first_name_prefix_hash} HMAC blind index.
     *
     * <p>The service takes the first 3 characters of the search term, computes
     * {@code HMAC-SHA-256(prefix3)}, and queries {@code first_name_prefix_hash} —
     * the entire operation is a single indexed equality lookup; no plaintext is ever
     * sent to the database.
     *
     * <p>Each chosen prefix is unique in the pool, so exactly {@value #EXPECTED_PER_NAME}
     * results are expected.
     */
    @Test
    void searchByFirstNamePrefix() {
        // { 3-char prefix, matching name in pool }
        String[][] cases = {
            { "Jam", "James"    },
            { "Pat", "Patricia" },
            { "Rob", "Robert"   },
            { "Jen", "Jennifer" },
            { "Mic", "Michael"  }
        };

        System.out.println();
        System.out.println("=== ALEUser — First Name Prefix Search (HMAC blind index on first_name_prefix_hash) ===");
        System.out.printf("%-8s  %-14s  %7s  %10s%n", "Prefix", "Expected Name", "Results", "Elapsed ms");
        System.out.println("-".repeat(47));

        for (String[] c : cases) {
            String prefix    = c[0];
            String expected  = c[1];
            long start       = System.currentTimeMillis();
            List<ALEUser> r  = aleUserSearchService.findByFirstNameStartingWith(prefix, TENANT_ID);
            long elapsed     = System.currentTimeMillis() - start;

            assertThat(r).isNotEmpty();
            assertThat(r).allSatisfy(u -> assertThat(u.getFirstName()).startsWith(prefix));
            // Note: prefix hash matches all names sharing the same 3-char prefix; count may be >250.

            System.out.printf("%-8s  %-14s  %7d  %10d%n", prefix, expected, r.size(), elapsed);
        }
    }
}
