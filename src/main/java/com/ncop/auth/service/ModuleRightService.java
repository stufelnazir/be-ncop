package com.ncop.auth.service;

import com.ncop.auth.model.ModuleRight;
import com.ncop.auth.repository.ModuleRightRepository;
import com.ncop.auth.exception.DuplicateResourceException;
import com.ncop.auth.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleRightService {

    private final ModuleRightRepository moduleRightRepository;

    public ModuleRightService(ModuleRightRepository moduleRightRepository) {
        this.moduleRightRepository = moduleRightRepository;
    }

    /**
     * Create a new module right
     */
    public ModuleRight createModuleRight(ModuleRight moduleRight) {
        if (moduleRight.getName() == null || moduleRight.getName().isBlank()) {
            throw new IllegalArgumentException("Module right name is required");
        }

        if (moduleRightRepository.findByName(moduleRight.getName()).isPresent()) {
            throw new DuplicateResourceException("Module right with name '" + moduleRight.getName() + "' already exists");
        }

        return moduleRightRepository.save(moduleRight);
    }

    /**
     * Get module right by ID
     */
    public ModuleRight getModuleRightById(String id) {
        return moduleRightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module right not found with id: " + id));
    }

    /**
     * Get module right by name
     */
    public ModuleRight getModuleRightByName(String name) {
        return moduleRightRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Module right not found with name: " + name));
    }

    /**
     * Get all module rights
     */
    public List<ModuleRight> getAllModuleRights() {
        return moduleRightRepository.findAll();
    }

    /**
     * Update module right
     */
    public ModuleRight updateModuleRight(String id, ModuleRight moduleRight) {
        ModuleRight existing = getModuleRightById(id);

        if (moduleRight.getName() != null && !moduleRight.getName().isBlank()) {
            if (!moduleRight.getName().equals(existing.getName()) &&
                    moduleRightRepository.findByName(moduleRight.getName()).isPresent()) {
                throw new DuplicateResourceException("Module right with name '" + moduleRight.getName() + "' already exists");
            }
            existing.setName(moduleRight.getName());
        }

        if (moduleRight.getLabel() != null) {
            existing.setLabel(moduleRight.getLabel());
        }

        if (moduleRight.getDescription() != null) {
            existing.setDescription(moduleRight.getDescription());
        }

        return moduleRightRepository.save(existing);
    }

    /**
     * Delete module right by ID
     */
    public void deleteModuleRight(String id) {
        if (!moduleRightRepository.existsById(id)) {
            throw new ResourceNotFoundException("Module right not found with id: " + id);
        }
        moduleRightRepository.deleteById(id);
    }
}

