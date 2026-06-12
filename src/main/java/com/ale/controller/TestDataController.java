package com.ale.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.ale.openapi.admin.api.TestDataApi;
import com.ale.service.TestDataService;

/**
 * Implements {@link TestDataApi} – the generated Spring interface derived from
 * {@code src/main/openapi/admin-api.yaml} (tag: TestData).
 *
 * <p>All routing annotations live on the interface.
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
public class TestDataController implements TestDataApi {

    private final TestDataService testDataService;

    public TestDataController(TestDataService testDataService) {
        this.testDataService = testDataService;
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<Map<String, Object>> loadUsers(UUID xTenantID, Integer count) {
        return ResponseEntity.ok(testDataService.loadUsers(count, xTenantID));
    }

    @Override
    public ResponseEntity<Map<String, Object>> loadAleUsers(UUID xTenantID, Integer count) {
        return ResponseEntity.ok(testDataService.loadALEUsers(count, xTenantID));
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> loadAll(UUID xTenantID, Integer count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.loadUsers(count, xTenantID));
        results.add(testDataService.loadALEUsers(count, xTenantID));
        return ResponseEntity.ok(results);
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<Map<String, Object>> clearUsers() {
        return ResponseEntity.ok(testDataService.clearUsers());
    }

    @Override
    public ResponseEntity<Map<String, Object>> clearAleUsers() {
        return ResponseEntity.ok(testDataService.clearALEUsers());
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> clearAll() {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.clearALEUsers());
        return ResponseEntity.ok(results);
    }

    // ── Reload (clear + load) ─────────────────────────────────────────────────

    @Override
    public ResponseEntity<List<Map<String, Object>>> reloadUsers(UUID xTenantID, Integer count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.loadUsers(count, xTenantID));
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> reloadAleUsers(UUID xTenantID, Integer count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearALEUsers());
        results.add(testDataService.loadALEUsers(count, xTenantID));
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> reloadAll(UUID xTenantID, Integer count) {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(testDataService.clearUsers());
        results.add(testDataService.clearALEUsers());
        results.add(testDataService.loadUsers(count, xTenantID));
        results.add(testDataService.loadALEUsers(count, xTenantID));
        return ResponseEntity.ok(results);
    }
}
