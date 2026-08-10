package com.ncop.auth.repository;

import com.ncop.auth.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}