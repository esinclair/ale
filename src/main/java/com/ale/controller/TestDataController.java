package com.ale.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ale.service.TestDataService;

/**
 * REST controller for loading and clearing test data in the {@code users} and
 * {@code aleuser} tables.
 *
 * <h3>Endpoints</h3>
 * <pre>
 * POST   /test-data/users/load?count=100000       insert N User rows
 * POST   /test-data/ale-users/load?count=100000   insert N ALEUser rows
 * POST   /test-data/load?count=100000             insert N of each
 *
 * DELETE /test-data/users                         truncate users table
 * DELETE /test-data/ale-users                     truncate aleuser table
 * DELETE /test-data                               truncate both tables
 *
 * POST   /test-data/users/reload?count=100000     truncate then insert N User rows
 * POST   /test-data/ale-users/reload?count=100000 truncate then insert N ALEUser rows
 * POST   /test-data/reload?count=100000           truncate both then insert N of each
 * </pre>
 *
 * <p>All load operations use JDBC batching ({@code hibernate.jdbc.batch_size=1000}) and
 * flush/clear the persistence context every 1 000 rows to keep heap usage flat.
 * Clear operations issue a single {@code TRUNCATE TABLE} statement per table.
 *
 * <p><b>Note:</b> disable {@code spring.jpa.show-sql} before loading large batches to
 * avoid console I/O becoming the bottleneck.
 */
@RestController
@RequestMapping("/test-data")
public class TestDataController {

    private static final int DEFAULT_COUNT = 100_000;

    private final TestDataService testDataService;

    public TestDataController(TestDataService testDataService) {
        this.testDataService = testDataService;
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    /** Insert {@code count} plaintext {@code User} rows (default 100 000). */
    @PostMapping("/users/load")
    public ResponseEntity<Map<String, Object>> loadUsers(
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        return ResponseEntity.ok(testDataService.loadUsers(count));
    }

    /** Insert {@code count} encrypted {@code ALEUser} rows (default 100 000). */
    @PostMapping("/ale-users/load")
    public ResponseEntity<Map<String, Object>> loadALEUsers(
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        return ResponseEntity.ok(testDataService.loadALEUsers(count));
    }

    /** Insert {@code count} rows into both tables (default 100 000 each). */
    @PostMapping("/load")
    public ResponseEntity<List<Map<String, Object>>> loadAll(
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.loadUsers(count));
        results.add(testDataService.loadALEUsers(count));
        return ResponseEntity.ok(results);
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    /** Truncate the {@code users} table. */
    @DeleteMapping("/users")
    public ResponseEntity<Map<String, Object>> clearUsers() {
        return ResponseEntity.ok(testDataService.clearUsers());
    }

    /** Truncate the {@code aleuser} table. */
    @DeleteMapping("/ale-users")
    public ResponseEntity<Map<String, Object>> clearALEUsers() {
        return ResponseEntity.ok(testDataService.clearALEUsers());
    }

    /** Truncate both tables. */
    @DeleteMapping
    public ResponseEntity<List<Map<String, Object>>> clearAll() {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.clearALEUsers());
        return ResponseEntity.ok(results);
    }

    // ── Reload (clear + load) ─────────────────────────────────────────────────

    /** Truncate {@code users}, then insert {@code count} fresh rows (default 100 000). */
    @PostMapping("/users/reload")
    public ResponseEntity<List<Map<String, Object>>> reloadUsers(
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.loadUsers(count));
        return ResponseEntity.ok(results);
    }

    /** Truncate {@code aleuser}, then insert {@code count} fresh rows (default 100 000). */
    @PostMapping("/ale-users/reload")
    public ResponseEntity<List<Map<String, Object>>> reloadALEUsers(
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearALEUsers());
        results.add(testDataService.loadALEUsers(count));
        return ResponseEntity.ok(results);
    }

    /** Truncate both tables, then insert {@code count} fresh rows in each (default 100 000). */
    @PostMapping("/reload")
    public ResponseEntity<List<Map<String, Object>>> reloadAll(
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.clearALEUsers());
        results.add(testDataService.loadUsers(count));
        results.add(testDataService.loadALEUsers(count));
        return ResponseEntity.ok(results);
    }
}
