package com.ncop.auth.controller;

import com.ncop.auth.model.ModuleRight;
import com.ncop.auth.service.ModuleRightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/module-rights")
public class ModuleRightController {

    private final ModuleRightService moduleRightService;

    public ModuleRightController(ModuleRightService moduleRightService) {
        this.moduleRightService = moduleRightService;
    }

    /**
     * Create a new module right
     */
    @PostMapping
    public ResponseEntity<ModuleRight> createModuleRight(@RequestBody ModuleRight moduleRight) {
        ModuleRight created = moduleRightService.createModuleRight(moduleRight);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get module right by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ModuleRight> getModuleRightById(@PathVariable String id) {
        ModuleRight moduleRight = moduleRightService.getModuleRightById(id);
        return ResponseEntity.ok(moduleRight);
    }

    /**
     * Get module right by name
     */
    @GetMapping("/by-name/{name}")
    public ResponseEntity<ModuleRight> getModuleRightByName(@PathVariable String name) {
        ModuleRight moduleRight = moduleRightService.getModuleRightByName(name);
        return ResponseEntity.ok(moduleRight);
    }

    /**
     * Get paginated module rights
     */
    @GetMapping
    public ResponseEntity<com.ncop.common.dto.PageResponse<ModuleRight>> getModuleRights(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(moduleRightService.getModuleRights(pageable, search));
    }

    /**
     * Get all module rights (unpaginated for checklists & dropdowns)
     */
    @GetMapping("/all")
    public ResponseEntity<List<ModuleRight>> getAllModuleRights() {
        List<ModuleRight> moduleRights = moduleRightService.getAllModuleRights();
        return ResponseEntity.ok(moduleRights);
    }

    /**
     * Update module right
     */
    @PutMapping("/{id}")
    public ResponseEntity<ModuleRight> updateModuleRight(@PathVariable String id, @RequestBody ModuleRight moduleRight) {
        ModuleRight updated = moduleRightService.updateModuleRight(id, moduleRight);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete module right
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModuleRight(@PathVariable String id) {
        moduleRightService.deleteModuleRight(id);
        return ResponseEntity.noContent().build();
    }
}

