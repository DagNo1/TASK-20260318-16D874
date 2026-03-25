package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.achievement.AchievementAttachmentRequest;
import com.pettrade.practiceplatform.api.achievement.AchievementAttachmentView;
import com.pettrade.practiceplatform.api.achievement.AchievementUpsertRequest;
import com.pettrade.practiceplatform.api.achievement.AchievementView;
import com.pettrade.practiceplatform.api.achievement.AssessmentFormRequest;
import com.pettrade.practiceplatform.api.achievement.AssessmentFormView;
import com.pettrade.practiceplatform.domain.PracticeAchievement;
import com.pettrade.practiceplatform.domain.PracticeAchievementAttachment;
import com.pettrade.practiceplatform.domain.PracticeAssessmentForm;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.PracticeAchievementAttachmentRepository;
import com.pettrade.practiceplatform.repository.PracticeAchievementRepository;
import com.pettrade.practiceplatform.repository.PracticeAssessmentFormRepository;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PracticeAchievementService {

    private final PracticeAchievementRepository achievementRepository;
    private final PracticeAchievementAttachmentRepository attachmentRepository;
    private final PracticeAssessmentFormRepository assessmentFormRepository;

    public PracticeAchievementService(
            PracticeAchievementRepository achievementRepository,
            PracticeAchievementAttachmentRepository attachmentRepository,
            PracticeAssessmentFormRepository assessmentFormRepository
    ) {
        this.achievementRepository = achievementRepository;
        this.attachmentRepository = attachmentRepository;
        this.assessmentFormRepository = assessmentFormRepository;
    }

    @Transactional
    public AchievementView createAchievement(AchievementUpsertRequest request, User owner) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BusinessRuleException("periodEnd must be on/after periodStart");
        }
        PracticeAchievement achievement = new PracticeAchievement();
        achievement.setOwnerUser(owner);
        achievement.setVersionNo(request.versionNo());
        achievement.setTitle(request.title());
        achievement.setPeriodStart(request.periodStart());
        achievement.setPeriodEnd(request.periodEnd());
        achievement.setResponsiblePerson(request.responsiblePerson());
        achievement.setConclusion(request.conclusion());
        return toView(achievementRepository.save(achievement));
    }

    @Transactional
    public AchievementView updateAchievement(Long achievementId, AchievementUpsertRequest request, User owner) {
        PracticeAchievement achievement = loadOwnedAchievement(achievementId, owner);
        if (request.versionNo() <= achievement.getVersionNo()) {
            throw new BusinessRuleException("versionNo must increment strictly; rollback is not allowed");
        }
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BusinessRuleException("periodEnd must be on/after periodStart");
        }

        achievement.setVersionNo(request.versionNo());
        achievement.setTitle(request.title());
        achievement.setPeriodStart(request.periodStart());
        achievement.setPeriodEnd(request.periodEnd());
        achievement.setResponsiblePerson(request.responsiblePerson());
        achievement.setConclusion(request.conclusion());
        return toView(achievementRepository.save(achievement));
    }

    @Transactional
    public AchievementAttachmentView addAttachment(Long achievementId, AchievementAttachmentRequest request, User owner) {
        PracticeAchievement achievement = loadOwnedAchievement(achievementId, owner);
        attachmentRepository.findTopByAchievementIdOrderByAttachmentVersionDesc(achievementId)
                .ifPresent(last -> {
                    if (request.attachmentVersion() <= last.getAttachmentVersion()) {
                        throw new BusinessRuleException("attachmentVersion must increment strictly");
                    }
                });

        PracticeAchievementAttachment attachment = new PracticeAchievementAttachment();
        attachment.setAchievement(achievement);
        attachment.setAttachmentVersion(request.attachmentVersion());
        attachment.setFileName(request.fileName());
        attachment.setFileUrl(request.fileUrl());
        attachment.setMimeType(request.mimeType());
        return toAttachmentView(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<AchievementAttachmentView> listAttachments(Long achievementId, User owner) {
        loadOwnedAchievement(achievementId, owner);
        return attachmentRepository.findByAchievementIdOrderByAttachmentVersionDesc(achievementId)
                .stream()
                .map(this::toAttachmentView)
                .toList();
    }

    @Transactional
    public AssessmentFormView upsertAssessmentForm(Long achievementId, AssessmentFormRequest request, User owner) {
        PracticeAchievement achievement = loadOwnedAchievement(achievementId, owner);
        PracticeAssessmentForm form = assessmentFormRepository.findByAchievementId(achievementId)
                .orElseGet(PracticeAssessmentForm::new);
        form.setAchievement(achievement);
        form.setAssessorName(request.assessorName());
        form.setAssessedAt(request.assessedAt());
        form.setOverallScore(request.overallScore());
        form.setStrengths(request.strengths());
        form.setImprovements(request.improvements());
        form.setRecommendations(request.recommendations());
        form.setCriteriaJson(request.criteriaJson());
        return toAssessmentView(assessmentFormRepository.save(form));
    }

    @Transactional(readOnly = true)
    public byte[] exportCompletionCertificatePdf(Long achievementId, User owner) {
        PracticeAchievement achievement = loadOwnedAchievement(achievementId, owner);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(70, 700);
                content.showText("Practice Completion Certificate");
                content.newLineAtOffset(0, -30);
                content.setFont(PDType1Font.HELVETICA, 12);
                content.showText("Title: " + achievement.getTitle());
                content.newLineAtOffset(0, -20);
                content.showText("Period: " + achievement.getPeriodStart() + " to " + achievement.getPeriodEnd());
                content.newLineAtOffset(0, -20);
                content.showText("Responsible: " + achievement.getResponsiblePerson());
                content.newLineAtOffset(0, -20);
                content.showText("Conclusion: " + achievement.getConclusion());
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export completion certificate PDF", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportAssessmentFormXlsx(Long achievementId, User owner) {
        loadOwnedAchievement(achievementId, owner);
        PracticeAssessmentForm form = assessmentFormRepository.findByAchievementId(achievementId)
                .orElseThrow(() -> new NotFoundException("Assessment form not found"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("assessment_form");
            sheet.createRow(0).createCell(0).setCellValue("Assessor Name");
            sheet.getRow(0).createCell(1).setCellValue(form.getAssessorName());
            sheet.createRow(1).createCell(0).setCellValue("Assessed At");
            sheet.getRow(1).createCell(1).setCellValue(form.getAssessedAt().toString());
            sheet.createRow(2).createCell(0).setCellValue("Overall Score");
            sheet.getRow(2).createCell(1).setCellValue(form.getOverallScore().doubleValue());
            sheet.createRow(3).createCell(0).setCellValue("Strengths");
            sheet.getRow(3).createCell(1).setCellValue(form.getStrengths() == null ? "" : form.getStrengths());
            sheet.createRow(4).createCell(0).setCellValue("Improvements");
            sheet.getRow(4).createCell(1).setCellValue(form.getImprovements() == null ? "" : form.getImprovements());
            sheet.createRow(5).createCell(0).setCellValue("Recommendations");
            sheet.getRow(5).createCell(1).setCellValue(form.getRecommendations() == null ? "" : form.getRecommendations());
            sheet.createRow(6).createCell(0).setCellValue("Criteria JSON");
            sheet.getRow(6).createCell(1).setCellValue(form.getCriteriaJson());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export assessment form xlsx", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AchievementView> listAchievements(User owner) {
        return achievementRepository.findByOwnerUserIdOrderByUpdatedAtDesc(owner.getId())
                .stream()
                .map(this::toView)
                .toList();
    }

    private PracticeAchievement loadOwnedAchievement(Long achievementId, User owner) {
        return achievementRepository.findByIdAndOwnerUserId(achievementId, owner.getId())
                .orElseThrow(() -> new NotFoundException("Practice achievement not found"));
    }

    private AchievementView toView(PracticeAchievement a) {
        return new AchievementView(
                a.getId(),
                a.getVersionNo(),
                a.getTitle(),
                a.getPeriodStart(),
                a.getPeriodEnd(),
                a.getResponsiblePerson(),
                a.getConclusion()
        );
    }

    private AchievementAttachmentView toAttachmentView(PracticeAchievementAttachment a) {
        return new AchievementAttachmentView(
                a.getId(),
                a.getAttachmentVersion(),
                a.getFileName(),
                a.getFileUrl(),
                a.getMimeType(),
                a.getCreatedAt()
        );
    }

    private AssessmentFormView toAssessmentView(PracticeAssessmentForm form) {
        return new AssessmentFormView(
                form.getId(),
                form.getAssessorName(),
                form.getAssessedAt(),
                form.getOverallScore(),
                form.getStrengths(),
                form.getImprovements(),
                form.getRecommendations(),
                form.getCriteriaJson()
        );
    }
}
