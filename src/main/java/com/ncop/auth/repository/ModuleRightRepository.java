package com.ncop.auth.repository;

import com.ncop.auth.model.ModuleRight;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ModuleRightRepository extends MongoRepository<ModuleRight, String> {
    Optional<ModuleRight> findByName(String name);
}

