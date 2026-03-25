package com.pettrade.practiceplatform.config;

import com.pettrade.practiceplatform.security.JwtTokenService;
import com.pettrade.practiceplatform.security.CustomUserDetailsService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;

    public WebSocketAuthChannelInterceptor(JwtTokenService jwtTokenService, CustomUserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> headers = accessor.getNativeHeader("Authorization");
            if (headers != null && !headers.isEmpty()) {
                String value = headers.get(0);
                if (value != null && value.startsWith("Bearer ")) {
                    String token = value.substring(7);
                    String username = jwtTokenService.extractUsername(token);
                    UserDetails user = userDetailsService.loadUserByUsername(username);
                    if (jwtTokenService.isTokenValid(token, user)) {
                        accessor.setUser(new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities()));
                    }
                }
            }
        }
        return message;
    }
}
