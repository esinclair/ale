package com.ale.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ale.service.TestDataService;

/**
 * REST controller for loading and clearing test data in the {@code users} and
 * {@code aleuser} tables.
 *
 * <p>All load endpoints require an {@code X-Tenant-ID} header (UUID) so that
 * inserted rows are correctly scoped to a tenant and the per-tenant DEK is selected.
 * Clear and DELETE endpoints do not require the header.
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
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        return ResponseEntity.ok(testDataService.loadUsers(count, tenantId));
    }

    /** Insert {@code count} encrypted {@code ALEUser} rows (default 100 000). */
    @PostMapping("/ale-users/load")
    public ResponseEntity<Map<String, Object>> loadALEUsers(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        return ResponseEntity.ok(testDataService.loadALEUsers(count, tenantId));
    }

    /** Insert {@code count} rows into both tables (default 100 000 each). */
    @PostMapping("/load")
    public ResponseEntity<List<Map<String, Object>>> loadAll(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.loadUsers(count, tenantId));
        results.add(testDataService.loadALEUsers(count, tenantId));
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
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.loadUsers(count, tenantId));
        return ResponseEntity.ok(results);
    }

    /** Truncate {@code aleuser}, then insert {@code count} fresh rows (default 100 000). */
    @PostMapping("/ale-users/reload")
    public ResponseEntity<List<Map<String, Object>>> reloadALEUsers(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearALEUsers());
        results.add(testDataService.loadALEUsers(count, tenantId));
        return ResponseEntity.ok(results);
    }

    /** Truncate both tables, then insert {@code count} fresh rows in each (default 100 000). */
    @PostMapping("/reload")
    public ResponseEntity<List<Map<String, Object>>> reloadAll(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "" + DEFAULT_COUNT) int count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.clearALEUsers());
        results.add(testDataService.loadUsers(count, tenantId));
        results.add(testDataService.loadALEUsers(count, tenantId));
        return ResponseEntity.ok(results);
    }
}
