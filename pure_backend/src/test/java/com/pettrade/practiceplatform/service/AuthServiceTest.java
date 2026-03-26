package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.auth.AuthRequest;
import com.pettrade.practiceplatform.api.auth.AuthResponse;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.repository.UserRepository;
import com.pettrade.practiceplatform.security.SensitiveDataMasker;
import com.pettrade.practiceplatform.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SensitiveDataMasker sensitiveDataMasker;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;

    private AuthService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        service = new AuthService(authenticationManager, jwtTokenService, userRepository, sensitiveDataMasker, fixedClock);
    }

    @Test
    void rejectsWeakPasswordBeforeAuthentication() {
        assertThrows(BusinessRuleException.class, () -> service.login(new AuthRequest("merchant", "password")));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void locksAccountAfterFiveFailedAttempts() {
        User user = user("merchant");
        when(userRepository.findByUsername("merchant")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad creds"));

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login(new AuthRequest("merchant", "Str0ng!Pass")));
        }

        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(LocalDateTime.of(2026, 3, 25, 12, 15, 0), user.getLockedUntil());
    }

    @Test
    void rejectsLoginWhenAccountIsLocked() {
        User user = user("merchant");
        user.setLockedUntil(LocalDateTime.of(2026, 3, 25, 12, 10, 0));
        when(userRepository.findByUsername("merchant")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new AuthRequest("merchant", "Str0ng!Pass")));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void successfulLoginResetsAttemptsAndLock() {
        User user = user("merchant");
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(LocalDateTime.of(2026, 3, 25, 11, 0, 0));

        when(userRepository.findByUsername("merchant")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("merchant");
        when(userDetails.getAuthorities()).thenReturn(List.of());
        when(jwtTokenService.generateToken(userDetails)).thenReturn("jwt");

        AuthResponse response = service.login(new AuthRequest("merchant", "Str0ng!Pass"));

        assertEquals("jwt", response.accessToken());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void acceptsAlphanumericPasswordMatchingMinimumPolicy() {
        User user = user("merchant");
        when(userRepository.findByUsername("merchant")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("merchant");
        when(userDetails.getAuthorities()).thenReturn(List.of());
        when(jwtTokenService.generateToken(userDetails)).thenReturn("jwt");

        AuthResponse response = service.login(new AuthRequest("merchant", "abcd1234"));
        assertEquals("jwt", response.accessToken());
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEnabled(true);
        user.setPasswordHash("hash");
        return user;
    }
}
