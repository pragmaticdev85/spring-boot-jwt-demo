package com.springboot.springbootsecurity.user.service.impl;

import com.springboot.springbootsecurity.auth.exception.PasswordNotValidException;
import com.springboot.springbootsecurity.auth.model.Token;
import com.springboot.springbootsecurity.auth.model.dto.request.LoginRequest;
import com.springboot.springbootsecurity.auth.service.TokenService;
import com.springboot.springbootsecurity.base.AbstractBaseServiceTest;
import com.springboot.springbootsecurity.builder.UserEntityBuilder;
import com.springboot.springbootsecurity.user.exception.UserNotFoundException;
import com.springboot.springbootsecurity.user.model.entity.UserEntity;
import com.springboot.springbootsecurity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserLoginServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private UserLoginServiceImpl userLoginService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Test
    void login_ValidCredentials_ReturnsToken() {

        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        UserEntity userEntity = new UserEntityBuilder().withValidFields().build();

        Token expectedToken = Token.builder()
                .accessToken("mockAccessToken")
                .accessTokenExpiresAt(123456789L)
                .refreshToken("mockRefreshToken")
                .build();

        // When
        when(userRepository.findUserEntityByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(userEntity));

        when(passwordEncoder.matches(loginRequest.getPassword(), userEntity.getPassword()))
                .thenReturn(true);

        when(tokenService.generateToken(userEntity.getClaims())).thenReturn(expectedToken);

        Token actualToken = userLoginService.login(loginRequest);

        // Then
        assertEquals(expectedToken.getAccessToken(), actualToken.getAccessToken());
        assertEquals(expectedToken.getRefreshToken(), actualToken.getRefreshToken());
        assertEquals(expectedToken.getAccessTokenExpiresAt(), actualToken.getAccessTokenExpiresAt());

        // Verify
        verify(userRepository).findUserEntityByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), userEntity.getPassword());
        verify(tokenService).generateToken(userEntity.getClaims());

    }

    @Test
    void login_InvalidEmail_ThrowsAdminNotFoundException() {

        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        // When
        when(userRepository.findUserEntityByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        // Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userLoginService.login(loginRequest));

        assertEquals("User not found!\n Can't find with given email: " + loginRequest.getEmail(), exception.getMessage());

        // Verify
        verify(userRepository).findUserEntityByEmail(loginRequest.getEmail());
        verifyNoInteractions(passwordEncoder, tokenService);

    }

    @Test
    void login_InvalidPassword_ThrowsPasswordNotValidException() {

        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("invalidPassword")
                .build();

        UserEntity userEntity = new UserEntityBuilder()
                .withEmail(loginRequest.getEmail())
                .withPassword("encodedPassword")
                .build();

        // When
        when(userRepository.findUserEntityByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(userEntity));

        when(passwordEncoder.matches(loginRequest.getPassword(), userEntity.getPassword()))
                .thenReturn(false);

        // Then
        PasswordNotValidException exception = assertThrows(PasswordNotValidException.class,
                () -> userLoginService.login(loginRequest));

        assertNotNull(exception);

        // Verify
        verify(userRepository).findUserEntityByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), userEntity.getPassword());
        verifyNoInteractions(tokenService);

    }

}