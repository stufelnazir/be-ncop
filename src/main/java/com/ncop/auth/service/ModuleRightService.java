package com.ncop.auth.service;

import com.ncop.auth.model.ModuleRight;
import com.ncop.auth.model.Role;
import com.ncop.auth.model.User;
import com.ncop.auth.repository.ModuleRightRepository;
import com.ncop.auth.repository.RoleRepository;
import com.ncop.auth.repository.UserRepository;
import com.ncop.auth.exception.DuplicateResourceException;
import com.ncop.auth.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ModuleRightService {

    private final ModuleRightRepository moduleRightRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public ModuleRightService(ModuleRightRepository moduleRightRepository,
                              RoleRepository roleRepository,
                              UserRepository userRepository) {
        this.moduleRightRepository = moduleRightRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new module right
     */
    public ModuleRight createModuleRight(ModuleRight moduleRight) {
        if (moduleRight.getName() == null || moduleRight.getName().isBlank()) {
            throw new IllegalArgumentException("Module right name is required");
        }

        String name = moduleRight.getName().trim().toUpperCase();
        if (moduleRightRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("Module right with name '" + name + "' already exists");
        }

        moduleRight.setName(name);
        if (moduleRight.getLabel() == null || moduleRight.getLabel().isBlank()) {
            moduleRight.setLabel(formatLabel(name));
        }
        moduleRight.setCreatedOn(Instant.now());
        moduleRight.setLastUpdatedOn(Instant.now());

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
        return moduleRightRepository.findByName(name.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Module right not found with name: " + name));
    }

    /**
     * Get all module rights
     */
    public List<ModuleRight> getAllModuleRights() {
        return moduleRightRepository.findAll();
    }

    /**
     * Get paginated and filtered module rights
     */
    public com.ncop.common.dto.PageResponse<ModuleRight> getModuleRights(org.springframework.data.domain.Pageable pageable, String search) {
        List<ModuleRight> all = moduleRightRepository.findAll();
        List<ModuleRight> filtered = all.stream().filter(mr -> {
            if (search == null || search.isBlank()) return true;
            return (mr.getName() != null && mr.getName().toLowerCase().contains(search.toLowerCase())) ||
                    (mr.getLabel() != null && mr.getLabel().toLowerCase().contains(search.toLowerCase())) ||
                    (mr.getDescription() != null && mr.getDescription().toLowerCase().contains(search.toLowerCase()));
        }).toList();

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<ModuleRight> paged = filtered.subList(fromIndex, toIndex);

        return com.ncop.common.dto.PageResponse.of(paged, pageNumber, pageSize, filtered.size());
    }

    /**
     * Update module right and propagate changes to Roles and Users
     */
    public ModuleRight updateModuleRight(String id, ModuleRight moduleRight) {
        ModuleRight existing = getModuleRightById(id);
        String oldName = existing.getName();
        boolean nameChanged = false;

        if (moduleRight.getName() != null && !moduleRight.getName().isBlank()) {
            String newName = moduleRight.getName().trim().toUpperCase();
            if (!newName.equalsIgnoreCase(existing.getName()) &&
                    moduleRightRepository.findByName(newName).isPresent()) {
                throw new DuplicateResourceException("Module right with name '" + newName + "' already exists");
            }
            existing.setName(newName);
            nameChanged = !newName.equals(oldName);
        }

        if (moduleRight.getLabel() != null) {
            existing.setLabel(moduleRight.getLabel());
        }

        if (moduleRight.getDescription() != null) {
            existing.setDescription(moduleRight.getDescription());
        }

        existing.setLastUpdatedOn(Instant.now());
        ModuleRight saved = moduleRightRepository.save(existing);

        // Propagate renamed module right key to all Roles and Users
        if (nameChanged && oldName != null) {
            String newName = saved.getName();
            // Update in all Roles
            List<Role> allRoles = roleRepository.findAll();
            for (Role r : allRoles) {
                if (r.getModuleRights() != null && r.getModuleRights().contains(oldName)) {
                    r.getModuleRights().remove(oldName);
                    r.getModuleRights().add(newName);
                    roleRepository.save(r);
                }
            }
            // Update in all Users
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                if (u.getModuleRights() != null && u.getModuleRights().contains(oldName)) {
                    u.getModuleRights().remove(oldName);
                    u.getModuleRights().add(newName);
                    userRepository.save(u);
                }
            }
        }

        return saved;
    }

    /**
     * Delete module right and remove from all Roles and Users
     */
    public void deleteModuleRight(String id) {
        ModuleRight existing = moduleRightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module right not found with id: " + id));

        String name = existing.getName();
        if (name != null) {
            // Remove from all roles
            List<Role> allRoles = roleRepository.findAll();
            for (Role r : allRoles) {
                if (r.getModuleRights() != null && r.getModuleRights().remove(name)) {
                    roleRepository.save(r);
                }
            }
            // Remove from all users
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                if (u.getModuleRights() != null && u.getModuleRights().remove(name)) {
                    userRepository.save(u);
                }
            }
        }

        moduleRightRepository.deleteById(id);
    }

    private String formatLabel(String name) {
        String[] parts = name.split("[_-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
