package com.laptophub.backend.service;


import com.laptophub.backend.dto.DTOMapper;
import com.laptophub.backend.dto.UserRegisterDTO;
import com.laptophub.backend.dto.UserResponseDTO;
import com.laptophub.backend.dto.UserUpdateDTO;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.exception.ConflictException;
import com.laptophub.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    @SuppressWarnings("null")
    public UserResponseDTO registerUser(UserRegisterDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("El email " + dto.getEmail() + " ya está registrado");
        }
        User user = DTOMapper.toUser(dto);
        User saved = userRepository.save(user);
        return DTOMapper.toUserResponse(saved);
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }
    
    @Transactional(readOnly = true)
    public UserResponseDTO findByIdDTO(UUID id) {
        return DTOMapper.toUserResponse(findById(id));
    }
    
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
    }
    
    @Transactional(readOnly = true)
    public UserResponseDTO findByEmailDTO(String email) {
        return DTOMapper.toUserResponse(findByEmail(email));
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAllDTO(@NonNull Pageable pageable) {
        return userRepository.findAll(pageable).map(DTOMapper::toUserResponse);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public UserResponseDTO updateUser(UUID id, UserUpdateDTO dto) {
        User existingUser = findById(id);
        if (dto.getNombre() != null) existingUser.setNombre(dto.getNombre());
        if (dto.getApellido() != null) existingUser.setApellido(dto.getApellido());
        if (dto.getTelefono() != null) existingUser.setTelefono(dto.getTelefono());
        if (dto.getDireccion() != null) existingUser.setDireccion(dto.getDireccion());
        User saved = userRepository.save(existingUser);
        return DTOMapper.toUserResponse(saved);
    }
}
