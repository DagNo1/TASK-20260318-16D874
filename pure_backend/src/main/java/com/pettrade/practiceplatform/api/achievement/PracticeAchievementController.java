package com.pettrade.practiceplatform.api.achievement;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.PracticeAchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/practice-achievements")
@Tag(name = "Practice Achievements")
public class PracticeAchievementController {

    private final PracticeAchievementService service;
    private final CurrentUserService currentUserService;

    public PracticeAchievementController(PracticeAchievementService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER','REVIEWER')")
    @Operation(summary = "List achievements")
    public ResponseEntity<List<AchievementView>> listAchievements() {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(service.listAchievements(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Create achievement")
    public ResponseEntity<AchievementView> create(@Valid @RequestBody AchievementUpsertRequest request) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(service.createAchievement(request, user));
    }

    @PutMapping("/{achievementId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Update achievement (strict version increment)")
    public ResponseEntity<AchievementView> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody AchievementUpsertRequest request
    ) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(service.updateAchievement(achievementId, request, user));
    }

    @PostMapping("/{achievementId}/attachments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Add versioned attachment")
    public ResponseEntity<AchievementAttachmentView> addAttachment(
            @PathVariable Long achievementId,
            @Valid @RequestBody AchievementAttachmentRequest request
    ) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(service.addAttachment(achievementId, request, user));
    }

    @GetMapping("/{achievementId}/attachments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER','REVIEWER')")
    @Operation(summary = "List achievement attachments")
    public ResponseEntity<List<AchievementAttachmentView>> listAttachments(@PathVariable Long achievementId) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(service.listAttachments(achievementId, user));
    }

    @PostMapping("/{achievementId}/assessment-form")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Upsert assessment form")
    public ResponseEntity<AssessmentFormView> upsertAssessment(
            @PathVariable Long achievementId,
            @Valid @RequestBody AssessmentFormRequest request
    ) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(service.upsertAssessmentForm(achievementId, request, user));
    }

    @GetMapping("/{achievementId}/export/completion-certificate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER','REVIEWER')")
    @Operation(summary = "Export completion certificate PDF")
    public ResponseEntity<byte[]> exportCertificate(@PathVariable Long achievementId) {
        User user = currentUserService.currentUser();
        byte[] bytes = service.exportCompletionCertificatePdf(achievementId, user);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("completion-certificate.pdf").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/{achievementId}/export/assessment-form")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER','REVIEWER')")
    @Operation(summary = "Export assessment form XLSX")
    public ResponseEntity<byte[]> exportAssessment(@PathVariable Long achievementId) {
        User user = currentUserService.currentUser();
        byte[] bytes = service.exportAssessmentFormXlsx(achievementId, user);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("assessment-form.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
