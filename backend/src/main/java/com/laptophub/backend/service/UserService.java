package com.laptophub.backend.service;


import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.exception.ConflictException;
import com.laptophub.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ConflictException("El email " + user.getEmail() + " ya está registrado");
        }
        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }
    
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public User updateUser(UUID id, User updatedUser) {
        User existingUser = findById(id);
        if (updatedUser.getNombre() != null) existingUser.setNombre(updatedUser.getNombre());
        if (updatedUser.getApellido() != null) existingUser.setApellido(updatedUser.getApellido());
        if (updatedUser.getTelefono() != null) existingUser.setTelefono(updatedUser.getTelefono());
        if (updatedUser.getDireccion() != null) existingUser.setDireccion(updatedUser.getDireccion());
        return userRepository.save(existingUser);
    }
}
