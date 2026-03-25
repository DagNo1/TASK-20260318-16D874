package com.pettrade.practiceplatform.service.im;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.ImSessionMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class ImSessionGuardService {

    private final ImSessionMemberRepository sessionMemberRepository;

    public ImSessionGuardService(ImSessionMemberRepository sessionMemberRepository) {
        this.sessionMemberRepository = sessionMemberRepository;
    }

    public void ensureMember(Long sessionId, User user) {
        boolean member = sessionMemberRepository.findBySessionIdAndUserId(sessionId, user.getId()).isPresent();
        if (!member) {
            throw new NotFoundException("IM session not found");
        }
    }
}
