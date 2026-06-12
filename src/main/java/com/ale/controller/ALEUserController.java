package com.ale.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.ale.encryption.TenantContext;
import com.ale.model.ALEUser;
import com.ale.openapi.pub.api.AleUsersApi;
import com.ale.repository.ALEUserRepository;
import com.ale.service.ALEUserSearchService;

/**
 * Implements {@link AleUsersApi} – the generated Spring interface derived from
 * {@code src/main/openapi/public-api.yaml} (tag: AleUsers).
 *
 * <p>Write endpoints (POST, PUT) set {@link TenantContext} so that
 * {@link com.ale.encryption.EncryptedStringConverter} selects the correct
 * per-tenant DEK during Hibernate flush; the context is always cleared in a
 * {@code finally} block after the repository call.
 *
 * <p>All routing annotations live on the interface.
 */
@RestController
public class ALEUserController implements AleUsersApi {

    private final ALEUserRepository aleUserRepository;
    private final ALEUserSearchService aleUserSearchService;

    public ALEUserController(ALEUserRepository aleUserRepository, ALEUserSearchService aleUserSearchService) {
        this.aleUserRepository = aleUserRepository;
        this.aleUserSearchService = aleUserSearchService;
    }

    @Override
    public ResponseEntity<List<ALEUser>> getAllAleUsers() {
        return ResponseEntity.ok(aleUserRepository.findAll());
    }

    @Override
    public ResponseEntity<ALEUser> getAleUserById(UUID id) {
        return aleUserRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<ALEUser> createAleUser(UUID xTenantID, ALEUser alEUser) {
        alEUser.setTenantId(xTenantID);
        TenantContext.set(xTenantID);
        try {
            ALEUser saved = aleUserRepository.save(alEUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public ResponseEntity<ALEUser> updateAleUser(UUID xTenantID, UUID id, ALEUser alEUser) {
        TenantContext.set(xTenantID);
        try {
            return aleUserRepository.findById(id)
                    .map(user -> {
                        user.setFirstName(alEUser.getFirstName());
                        user.setLastName(alEUser.getLastName());
                        user.setUserName(alEUser.getUserName());
                        user.setEmail(alEUser.getEmail());
                        user.setTenantId(xTenantID);
                        return ResponseEntity.ok(aleUserRepository.save(user));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public ResponseEntity<Void> deleteAleUser(UUID id) {
        if (!aleUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        aleUserRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<ALEUser>> searchAleUsersByFirstName(UUID xTenantID, String value) {
        return ResponseEntity.ok(aleUserSearchService.findByFirstName(value, xTenantID));
    }

    @Override
    public ResponseEntity<List<ALEUser>> searchAleUsersByLastName(UUID xTenantID, String value) {
        return ResponseEntity.ok(aleUserSearchService.findByLastName(value, xTenantID));
    }

    @Override
    public ResponseEntity<List<ALEUser>> searchAleUsersByFirstNamePrefix(UUID xTenantID, String prefix) {
        return ResponseEntity.ok(aleUserSearchService.findByFirstNameStartingWith(prefix, xTenantID));
    }

    @Override
    public ResponseEntity<List<ALEUser>> searchAleUsersByLastNamePrefix(UUID xTenantID, String prefix) {
        return ResponseEntity.ok(aleUserSearchService.findByLastNameStartingWith(prefix, xTenantID));
    }
}
