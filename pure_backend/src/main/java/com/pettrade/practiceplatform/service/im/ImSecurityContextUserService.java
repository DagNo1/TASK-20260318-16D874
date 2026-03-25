package com.pettrade.practiceplatform.service.im;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class ImSecurityContextUserService {

    private final UserRepository userRepository;

    public ImSecurityContextUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User fromPrincipal(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new NotFoundException("Authenticated user not found");
        }
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }
}
