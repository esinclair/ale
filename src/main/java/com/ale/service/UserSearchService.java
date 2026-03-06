package com.ale.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ale.model.User;
import com.ale.repository.UserRepository;

/**
 * Search service for the plaintext {@link User} entity.
 *
 * <p>Because {@code User} fields are stored unencrypted, Spring Data derived queries handle
 * the SQL {@code WHERE} clause automatically; no pre-processing is required by the caller.
 */
@Service
public class UserSearchService {

    private final UserRepository userRepository;

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns every {@link User} whose {@code firstName} exactly matches the supplied value
     * (case-sensitive, as stored in the database).
     */
    public List<User> findByFirstName(String firstName) {
        return userRepository.findByFirstName(firstName);
    }

    /**
     * Returns every {@link User} whose {@code lastName} exactly matches the supplied value
     * (case-sensitive, as stored in the database).
     */
    public List<User> findByLastName(String lastName) {
        return userRepository.findByLastName(lastName);
    }

    /**
     * Returns every {@link User} whose {@code firstName} starts with the supplied prefix.
     * Translates to {@code WHERE first_name LIKE 'prefix%'}.
     */
    public List<User> findByFirstNameStartingWith(String prefix) {
        return userRepository.findByFirstNameStartingWith(prefix);
    }

    /**
     * Returns every {@link User} whose {@code lastName} starts with the supplied prefix.
     * Translates to {@code WHERE last_name LIKE 'prefix%'}.
     */
    public List<User> findByLastNameStartingWith(String prefix) {
        return userRepository.findByLastNameStartingWith(prefix);
    }
}
