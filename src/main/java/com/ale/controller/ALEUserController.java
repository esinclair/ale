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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;
import com.ale.service.ALEUserSearchService;

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
    public ResponseEntity<ALEUser> createALEUser(@RequestBody ALEUser aleUser) {
        ALEUser saved = aleUserRepository.save(aleUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ALEUser> updateALEUser(@PathVariable UUID id, @RequestBody ALEUser updated) {
        return aleUserRepository.findById(id)
                .map(user -> {
                    user.setFirstName(updated.getFirstName());
                    user.setLastName(updated.getLastName());
                    user.setUserName(updated.getUserName());
                    user.setEmail(updated.getEmail());
                    return ResponseEntity.ok(aleUserRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
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
    public List<ALEUser> searchByFirstName(@RequestParam("value") String value) {
        return aleUserSearchService.findByFirstName(value);
    }

    @GetMapping("/search/last-name")
    public List<ALEUser> searchByLastName(@RequestParam("value") String value) {
        return aleUserSearchService.findByLastName(value);
    }

    @GetMapping("/search/first-name-prefix")
    public List<ALEUser> searchByFirstNamePrefix(@RequestParam("prefix") String prefix) {
        return aleUserSearchService.findByFirstNameStartingWith(prefix);
    }

    @GetMapping("/search/last-name-prefix")
    public List<ALEUser> searchByLastNamePrefix(@RequestParam("prefix") String prefix) {
        return aleUserSearchService.findByLastNameStartingWith(prefix);
    }
}
