package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.auth.AuthRequest;
import com.pettrade.practiceplatform.api.auth.AuthResponse;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.repository.UserRepository;
import com.pettrade.practiceplatform.security.SensitiveDataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pettrade.practiceplatform.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final Pattern PASSWORD_COMPLEXITY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15L;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final SensitiveDataMasker sensitiveDataMasker;
    private final Clock clock;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            UserRepository userRepository,
            SensitiveDataMasker sensitiveDataMasker,
            Clock clock
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.clock = clock;
    }

    public AuthResponse login(AuthRequest request) {
        validatePasswordComplexity(request.password());
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<User> userOpt = userRepository.findByUsername(request.username());
        userOpt.ifPresent(user -> resetExpiredLock(user, now));

        if (userOpt.isPresent() && isCurrentlyLocked(userOpt.get(), now)) {
            throw new BadCredentialsException("Account is locked. Try again later.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException ex) {
            userOpt.ifPresent(user -> registerFailedAttempt(user, now));
            log.warn("Authentication failed for username={}", sensitiveDataMasker.maskGeneric(request.username()));
            throw new BadCredentialsException("Invalid username or password");
        }

        userOpt.ifPresent(this::clearFailedAttempts);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(userDetails);
        return new AuthResponse(
                token,
                "Bearer",
                userDetails.getUsername(),
                userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }

    private void validatePasswordComplexity(String password) {
        if (!PASSWORD_COMPLEXITY.matcher(password).matches()) {
            throw new BusinessRuleException("Password must be at least 8 characters and include letters and numbers");
        }
    }

    private boolean isCurrentlyLocked(User user, LocalDateTime now) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }

    private void resetExpiredLock(User user, LocalDateTime now) {
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    private void registerFailedAttempt(User user, LocalDateTime now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }

    private void clearFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }
}
