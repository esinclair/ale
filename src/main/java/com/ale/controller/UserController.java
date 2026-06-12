package com.ale.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.ale.model.User;
import com.ale.openapi.pub.api.UsersApi;
import com.ale.repository.UserRepository;
import com.ale.service.UserSearchService;

/**
 * Implements {@link UsersApi} – the generated Spring interface derived from
 * {@code src/main/openapi/public-api.yaml} (tag: Users).
 *
 * <p>All routing annotations (@RequestMapping, @GetMapping, …) live on the
 * interface; this class only carries @RestController and the business logic.
 */
@RestController
public class UserController implements UsersApi {

    private final UserRepository userRepository;
    private final UserSearchService userSearchService;

    public UserController(UserRepository userRepository, UserSearchService userSearchService) {
        this.userRepository = userRepository;
        this.userSearchService = userSearchService;
    }

    @Override
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @Override
    public ResponseEntity<User> getUserById(UUID id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<User> createUser(UUID xTenantID, User user) {
        user.setTenantId(xTenantID);
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Override
    public ResponseEntity<User> updateUser(UUID xTenantID, UUID id, User user) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setFirstName(user.getFirstName());
                    existing.setLastName(user.getLastName());
                    existing.setUserName(user.getUserName());
                    existing.setEmail(user.getEmail());
                    existing.setTenantId(xTenantID);
                    return ResponseEntity.ok(userRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<User>> searchUsersByFirstName(UUID xTenantID, String value) {
        return ResponseEntity.ok(userSearchService.findByFirstName(value, xTenantID));
    }

    @Override
    public ResponseEntity<List<User>> searchUsersByLastName(UUID xTenantID, String value) {
        return ResponseEntity.ok(userSearchService.findByLastName(value, xTenantID));
    }

    @Override
    public ResponseEntity<List<User>> searchUsersByFirstNamePrefix(UUID xTenantID, String prefix) {
        return ResponseEntity.ok(userSearchService.findByFirstNameStartingWith(prefix, xTenantID));
    }

    @Override
    public ResponseEntity<List<User>> searchUsersByLastNamePrefix(UUID xTenantID, String prefix) {
        return ResponseEntity.ok(userSearchService.findByLastNameStartingWith(prefix, xTenantID));
    }
}
