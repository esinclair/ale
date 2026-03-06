package com.ale.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ale.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
