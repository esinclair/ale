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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ale.model.User;
import com.ale.repository.UserRepository;
import com.ale.service.UserSearchService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserSearchService userSearchService;

    public UserController(UserRepository userRepository, UserSearchService userSearchService) {
        this.userRepository = userRepository;
        this.userSearchService = userSearchService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody User updated) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(updated.getFirstName());
                    user.setLastName(updated.getLastName());
                    user.setUserName(updated.getUserName());
                    user.setEmail(updated.getEmail());
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/first-name")
    public List<User> searchByFirstName(@RequestParam("value") String value) {
        return userSearchService.findByFirstName(value);
    }

    @GetMapping("/search/last-name")
    public List<User> searchByLastName(@RequestParam("value") String value) {
        return userSearchService.findByLastName(value);
    }

    @GetMapping("/search/first-name-prefix")
    public List<User> searchByFirstNamePrefix(@RequestParam("prefix") String prefix) {
        return userSearchService.findByFirstNameStartingWith(prefix);
    }

    @GetMapping("/search/last-name-prefix")
    public List<User> searchByLastNamePrefix(@RequestParam("prefix") String prefix) {
        return userSearchService.findByLastNameStartingWith(prefix);
    }
}
