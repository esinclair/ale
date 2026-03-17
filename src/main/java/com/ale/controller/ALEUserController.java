package com.ale.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ale.encryption.TenantContext;
import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;
import com.ale.service.ALEUserSearchService;

/**
 * REST controller for {@link ALEUser}.
 *
 * <p>Every endpoint requires an {@code X-Tenant-ID} header (a UUID string).  Write endpoints
 * (POST, PUT) set {@link TenantContext} so that {@link com.ale.encryption.EncryptedStringConverter}
 * selects the correct per-tenant DEK during Hibernate flush; the context is always cleared in a
 * {@code finally} block after the repository call.
 */
@RestController
@RequestMapping("/ale-users")
public class ALEUserController {

    private final ALEUserRepository aleUserRepository;
    private final ALEUserSearchService aleUserSearchService;

    public ALEUserController(ALEUserRepository aleUserRepository, ALEUserSearchService aleUserSearchService) {
        this.aleUserRepository = aleUserRepository;
        this.aleUserSearchService = aleUserSearchService;
    }

    @GetMapping
    public List<ALEUser> getAllALEUsers() {
        return aleUserRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ALEUser> getALEUserById(@PathVariable UUID id) {
        return aleUserRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ALEUser> createALEUser(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestBody ALEUser aleUser) {
        aleUser.setTenantId(tenantId);
        TenantContext.set(tenantId);
        try {
            ALEUser saved = aleUserRepository.save(aleUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ALEUser> updateALEUser(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id,
            @RequestBody ALEUser updated) {
        TenantContext.set(tenantId);
        try {
            return aleUserRepository.findById(id)
                    .map(user -> {
                        user.setFirstName(updated.getFirstName());
                        user.setLastName(updated.getLastName());
                        user.setUserName(updated.getUserName());
                        user.setEmail(updated.getEmail());
                        user.setTenantId(tenantId);
                        return ResponseEntity.ok(aleUserRepository.save(user));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } finally {
            TenantContext.clear();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteALEUser(@PathVariable UUID id) {
        if (!aleUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        aleUserRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/first-name")
    public List<ALEUser> searchByFirstName(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam("value") String value) {
        return aleUserSearchService.findByFirstName(value, tenantId);
    }

    @GetMapping("/search/last-name")
    public List<ALEUser> searchByLastName(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam("value") String value) {
        return aleUserSearchService.findByLastName(value, tenantId);
    }

    @GetMapping("/search/first-name-prefix")
    public List<ALEUser> searchByFirstNamePrefix(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam("prefix") String prefix) {
        return aleUserSearchService.findByFirstNameStartingWith(prefix, tenantId);
    }

    @GetMapping("/search/last-name-prefix")
    public List<ALEUser> searchByLastNamePrefix(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam("prefix") String prefix) {
        return aleUserSearchService.findByLastNameStartingWith(prefix, tenantId);
    }
}
