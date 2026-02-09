package com.laptophub.backend.controller;

import com.laptophub.backend.dto.UserRegisterDTO;
import com.laptophub.backend.dto.UserResponseDTO;
import com.laptophub.backend.dto.UserUpdateDTO;
import com.laptophub.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
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

    @GetMapping("/email")
    public UserResponseDTO findByEmail(@RequestParam String email) {
        return userService.findByEmailDTO(email);
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(@PathVariable UUID id, @Valid @RequestBody UserUpdateDTO dto) {
        return userService.updateUser(id, dto);
    }
}