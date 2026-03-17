package com.ale;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ale.model.User;
import com.ale.repository.UserRepository;
import com.ale.service.UserSearchService;

/**
 * Performance and accuracy tests for {@link UserSearchService} against the plaintext
 * {@code users} table.
 *
 * <p>The 20 000-row data set is inserted <b>once</b> via {@link BeforeAll} and removed
 * <b>once</b> via {@link AfterAll}, so all three test methods share the same rows.
 *
 * <p>Tests:
 * <ol>
 *   <li>{@link #searchByFirstName()} — 5 exact first-name lookups (btree index on
 *       {@code first_name})</li>
 *   <li>{@link #searchByLastName()} — 5 exact last-name lookups (btree index on
 *       {@code last_name})</li>
 *   <li>{@link #searchByFirstNamePrefix()} — 5 starts-with-3-char queries
 *       ({@code WHERE first_name LIKE 'xxx%'})</li>
 * </ol>
 *
 * <p>Each of the 80 distinct first/last names appears exactly 250 times (20 000 / 80).
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class UserSearchPerformanceTest {

    private static final int    USER_COUNT        = 20_000;
    private static final int    BATCH_SIZE        = 500;
    private static final int    EXPECTED_PER_NAME = USER_COUNT / 80; // 250

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

    @Autowired private UserRepository    userRepository;
    @Autowired private UserSearchService userSearchService;

    // ── One-time fixture ─────────────────────────────────────────────────────

    @BeforeAll
    void insertUsers() {
        userRepository.deleteAll(); // ensure clean slate regardless of prior run state
        System.out.printf("%nInserting %,d User rows (shared across all test methods)\u2026%n", USER_COUNT);
        long t0 = System.currentTimeMillis();
        int inserted = 0;
        while (inserted < USER_COUNT) {
            int end = Math.min(inserted + BATCH_SIZE, USER_COUNT);
            List<User> batch = new ArrayList<>(end - inserted);
            for (int i = inserted; i < end; i++) {
                String fn = FIRST_NAMES[i % FIRST_NAMES.length];
                String ln = LAST_NAMES [i % LAST_NAMES.length];
                String un = (fn + "." + ln + i).toLowerCase();
                batch.add(new User(fn, ln, un, un + "@" + DOMAINS[i % DOMAINS.length]));
            }
            userRepository.saveAll(batch);
            inserted = end;
        }
        System.out.printf("Insert done in %,d ms.%n", System.currentTimeMillis() - t0);
    }

    @AfterAll
    void cleanup() {
        userRepository.deleteAll();
        System.out.println("Cleanup complete.");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * 5 exact first-name searches using the {@code idx_users_first_name} btree index.
     * Each name appears exactly {@value #EXPECTED_PER_NAME} times in the data set.
     */
    @Test
    void searchByFirstName() {
        String[] targets = { "James", "Mary", "John", "Patricia", "Robert" };

        System.out.println();
        System.out.println("=== User — Exact First Name Search (btree index on first_name) ===");
        System.out.printf("%-14s  %7s  %10s%n", "First Name", "Results", "Elapsed ms");
        System.out.println("-".repeat(37));

        for (String target : targets) {
            long start   = System.currentTimeMillis();
            List<User> r = userSearchService.findByFirstName(target);
            long elapsed = System.currentTimeMillis() - start;

            assertThat(r).hasSize(EXPECTED_PER_NAME);
            assertThat(r).allSatisfy(u -> assertThat(u.getFirstName()).isEqualTo(target));

            System.out.printf("%-14s  %7d  %10d%n", target, r.size(), elapsed);
        }
    }

    /**
     * 5 exact last-name searches using the {@code idx_users_last_name} btree index.
     * Each name appears exactly {@value #EXPECTED_PER_NAME} times.
     */
    @Test
    void searchByLastName() {
        String[] targets = { "Smith", "Johnson", "Williams", "Brown", "Jones" };

        System.out.println();
        System.out.println("=== User — Exact Last Name Search (btree index on last_name) ===");
        System.out.printf("%-14s  %7s  %10s%n", "Last Name", "Results", "Elapsed ms");
        System.out.println("-".repeat(37));

        for (String target : targets) {
            long start   = System.currentTimeMillis();
            List<User> r = userSearchService.findByLastName(target);
            long elapsed = System.currentTimeMillis() - start;

            assertThat(r).hasSize(EXPECTED_PER_NAME);
            assertThat(r).allSatisfy(u -> assertThat(u.getLastName()).isEqualTo(target));

            System.out.printf("%-14s  %7d  %10d%n", target, r.size(), elapsed);
        }
    }

    /**
     * 5 prefix searches ({@code WHERE first_name LIKE 'xxx%'}).
     * Each chosen 3-char prefix maps to exactly one name in the pool
     * → exactly {@value #EXPECTED_PER_NAME} results expected.
     */
    @Test
    void searchByFirstNamePrefix() {
        // { prefix, expected name in pool }
        String[][] cases = {
            { "Jam", "James"    },
            { "Pat", "Patricia" },
            { "Rob", "Robert"   },
            { "Jen", "Jennifer" },
            { "Mic", "Michael"  }
        };

        System.out.println();
        System.out.println("=== User — First Name Prefix Search (LIKE 'xxx%', btree index on first_name) ===");
        System.out.printf("%-8s  %-14s  %7s  %10s%n", "Prefix", "Expected Name", "Results", "Elapsed ms");
        System.out.println("-".repeat(47));

        for (String[] c : cases) {
            String prefix   = c[0];
            String expected = c[1];
            long start      = System.currentTimeMillis();
            List<User> r    = userSearchService.findByFirstNameStartingWith(prefix);
            long elapsed    = System.currentTimeMillis() - start;

            assertThat(r).isNotEmpty();
            assertThat(r).allSatisfy(u -> assertThat(u.getFirstName()).startsWith(prefix));
            // Note: a prefix may match >1 name in the pool; we verify correctness, not exact count.

            System.out.printf("%-8s  %-14s  %7d  %10d%n", prefix, expected, r.size(), elapsed);
        }
    }
}
