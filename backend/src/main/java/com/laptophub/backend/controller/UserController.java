package com.laptophub.backend.controller;

import com.laptophub.backend.dto.ChangePasswordDTO;
import com.laptophub.backend.dto.UserRegisterDTO;
import com.laptophub.backend.dto.UserResponseDTO;
import com.laptophub.backend.dto.UserUpdateDTO;
import com.laptophub.backend.model.Role;
import com.laptophub.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public UserResponseDTO register(@Valid @RequestBody UserRegisterDTO dto) {
        return userService.registerUser(dto);
    }

    @GetMapping("/{id}")
    public UserResponseDTO findById(@PathVariable UUID id) {
        return userService.findByIdDTO(id);
    }

    @GetMapping
    public Page<UserResponseDTO> findAll(@NonNull Pageable pageable) {
        return userService.findAllDTO(pageable);
    }

    @GetMapping("/inactive")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponseDTO> findAllInactive(@NonNull Pageable pageable) {
        return userService.findAllInactiveDTO(pageable);
    }

    @GetMapping("/email")
    public UserResponseDTO findByEmail(@RequestParam String email) {
        return userService.findByEmailDTO(email);
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(@PathVariable UUID id, @Valid @RequestBody UserUpdateDTO dto) {
        return userService.updateUser(id, dto);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable UUID id) {
        userService.deactivateUser(id);
    }

    @PutMapping("/{id}/reactivate")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO reactivate(@PathVariable UUID id) {
        return userService.reactivateUser(id);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO changeRole(
            @PathVariable UUID id,
            @RequestParam Role role) {
        return userService.changeRole(id, role);
    }
}