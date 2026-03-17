package com.ale.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ale.encryption.TenantContext;
import com.ale.model.ALEUser;
import com.ale.model.User;
import com.ale.repository.ALEUserRepository;
import com.ale.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Service responsible for high-throughput test-data generation.
 *
 * <p>Inserts use {@link EntityManager#persist} with periodic {@code flush()} + {@code clear()}
 * so the persistence context never grows unbounded and Hibernate's JDBC batch writer can
 * coalesce rows into single multi-row INSERT statements
 * (requires {@code hibernate.jdbc.batch_size} to be set in application.properties).
 *
 * <p>Clears use a single {@code TRUNCATE TABLE} native query — the fastest way to empty
 * a large table in PostgreSQL.  Because these entities carry no {@code @PreRemove} lifecycle
 * callbacks and have no foreign-key children, TRUNCATE is safe here.
 */
@Service
public class TestDataService {

    static final int BATCH_SIZE = 1_000;

    // ── Name / domain pools (80 first names, 80 last names, 10 domains) ──────

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

    // ── Dependencies ──────────────────────────────────────────────────────────

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository    userRepository;
    private final ALEUserRepository aleUserRepository;

    public TestDataService(UserRepository userRepository, ALEUserRepository aleUserRepository) {
        this.userRepository    = userRepository;
        this.aleUserRepository = aleUserRepository;
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    /**
     * Inserts {@code count} {@link User} rows using JDBC batch writes.
     * The persistence context is flushed and cleared every {@value #BATCH_SIZE} rows so
     * memory usage stays flat regardless of {@code count}.
     */
    @Transactional
    public Map<String, Object> loadUsers(int count, UUID tenantId) {
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            entityManager.persist(buildUser(i, tenantId));
            if ((i + 1) % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        return buildResult("users", count, elapsed);
    }

    /**
     * Inserts {@code count} {@link ALEUser} rows using JDBC batch writes.
     * {@link TenantContext} is set for the duration of the operation so that
     * {@link com.ale.encryption.EncryptedStringConverter} selects the correct per-tenant DEK.
     */
    @Transactional
    public Map<String, Object> loadALEUsers(int count, UUID tenantId) {
        long start = System.currentTimeMillis();
        TenantContext.set(tenantId);
        try {
            for (int i = 0; i < count; i++) {
                entityManager.persist(buildALEUser(i, tenantId));
                if ((i + 1) % BATCH_SIZE == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            // Flush remaining entities while TenantContext is still set.
            entityManager.flush();
            entityManager.clear();
        } finally {
            TenantContext.clear();
        }

        long elapsed = System.currentTimeMillis() - start;
        return buildResult("ale-users", count, elapsed);
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    /**
     * Truncates the {@code users} table in a single DDL statement — far faster than
     * row-by-row deletes on a large table.
     */
    @Transactional
    public Map<String, Object> clearUsers() {
        long start = System.currentTimeMillis();
        entityManager.createNativeQuery("TRUNCATE TABLE users").executeUpdate();
        long elapsed = System.currentTimeMillis() - start;
        return buildClearResult("users", elapsed);
    }

    /**
     * Truncates the {@code aleuser} table in a single DDL statement.
     */
    @Transactional
    public Map<String, Object> clearALEUsers() {
        long start = System.currentTimeMillis();
        entityManager.createNativeQuery("TRUNCATE TABLE aleuser").executeUpdate();
        long elapsed = System.currentTimeMillis() - start;
        return buildClearResult("ale-users", elapsed);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User buildUser(int i, UUID tenantId) {
        String firstName = FIRST_NAMES[i % FIRST_NAMES.length];
        String lastName  = LAST_NAMES [i % LAST_NAMES.length];
        String userName  = (firstName + "." + lastName + i).toLowerCase();
        String email     = userName + "@" + DOMAINS[i % DOMAINS.length];
        return new User(firstName, lastName, userName, email, tenantId);
    }

    private ALEUser buildALEUser(int i, UUID tenantId) {
        String firstName = FIRST_NAMES[i % FIRST_NAMES.length];
        String lastName  = LAST_NAMES [i % LAST_NAMES.length];
        String userName  = (firstName + "." + lastName + i).toLowerCase();
        String email     = userName + "@" + DOMAINS[i % DOMAINS.length];
        return new ALEUser(firstName, lastName, userName, email, tenantId);
    }

    private Map<String, Object> buildResult(String target, int count, long elapsedMs) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("target",           target);
        r.put("inserted",         count);
        r.put("elapsedMs",        elapsedMs);
        r.put("insertsPerSecond", elapsedMs > 0 ? Math.round(count / (elapsedMs / 1000.0)) : count);
        return r;
    }

    private Map<String, Object> buildClearResult(String target, long elapsedMs) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("target",    target);
        r.put("cleared",   true);
        r.put("elapsedMs", elapsedMs);
        return r;
    }
}
