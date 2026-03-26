package com.pettrade.practiceplatform.api.profile;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.SensitiveProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/sensitive")
@Tag(name = "SensitiveProfile")
public class SensitiveProfileController {

    private final SensitiveProfileService sensitiveProfileService;
    private final CurrentUserService currentUserService;

    public SensitiveProfileController(SensitiveProfileService sensitiveProfileService, CurrentUserService currentUserService) {
        this.sensitiveProfileService = sensitiveProfileService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create or update encrypted sensitive profile")
    public ResponseEntity<SensitiveProfileView> upsert(@Valid @RequestBody SensitiveProfileUpsertRequest request) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(sensitiveProfileService.upsert(user, request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get masked sensitive profile")
    public ResponseEntity<SensitiveProfileView> get() {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(sensitiveProfileService.get(user));
    }
}
