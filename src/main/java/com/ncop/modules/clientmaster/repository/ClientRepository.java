package com.ncop.modules.clientmaster.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ncop.modules.clientmaster.entity.Client;

@Repository
public interface ClientRepository extends MongoRepository<Client, String> {
    
    boolean existsByCustomerCode(String customerCode);

}
