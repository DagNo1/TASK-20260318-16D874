package com.pettrade.practiceplatform.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.auth.AuthController;
import com.pettrade.practiceplatform.api.auth.AuthRequest;
import com.pettrade.practiceplatform.api.auth.AuthResponse;
import com.pettrade.practiceplatform.api.approval.ApprovalRequestCreateDto;
import com.pettrade.practiceplatform.api.approval.ApprovalController;
import com.pettrade.practiceplatform.api.achievement.PracticeAchievementController;
import com.pettrade.practiceplatform.api.inventory.InventoryController;
import com.pettrade.practiceplatform.api.notification.NotificationController;
import com.pettrade.practiceplatform.api.notification.NotificationSubscriptionRequest;
import com.pettrade.practiceplatform.api.product.ProductBatchImportRequest;
import com.pettrade.practiceplatform.api.product.ProductController;
import com.pettrade.practiceplatform.api.product.ProductImportRow;
import com.pettrade.practiceplatform.api.product.ProductSkuRequest;
import com.pettrade.practiceplatform.api.product.ProductUpsertRequest;
import com.pettrade.practiceplatform.api.product.ProductView;
import com.pettrade.practiceplatform.api.profile.SensitiveProfileController;
import com.pettrade.practiceplatform.api.profile.SensitiveProfileUpsertRequest;
import com.pettrade.practiceplatform.api.reporting.ReportQueryRequest;
import com.pettrade.practiceplatform.api.reporting.ReportExportRequest;
import com.pettrade.practiceplatform.api.reporting.ReportingController;
import com.pettrade.practiceplatform.api.practice.CheckpointResponse;
import com.pettrade.practiceplatform.api.practice.CreatePracticeSessionRequest;
import com.pettrade.practiceplatform.api.practice.PracticeSessionController;
import com.pettrade.practiceplatform.api.practice.PracticeSessionResponse;
import com.pettrade.practiceplatform.api.practice.StepCompleteResponse;
import com.pettrade.practiceplatform.api.practice.TimerCommandRequest;
import com.pettrade.practiceplatform.api.practice.TimerStateResponse;
import com.pettrade.practiceplatform.config.SecurityConfig;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.CheckpointType;
import com.pettrade.practiceplatform.domain.enumtype.SessionStatus;
import com.pettrade.practiceplatform.domain.enumtype.StepStatus;
import com.pettrade.practiceplatform.domain.enumtype.TimerAction;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.security.JwtAuthenticationFilter;
import com.pettrade.practiceplatform.service.AuthService;
import com.pettrade.practiceplatform.service.ApprovalService;
import com.pettrade.practiceplatform.service.CheckpointService;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.InventoryService;
import com.pettrade.practiceplatform.service.NotificationService;
import com.pettrade.practiceplatform.service.PracticeAchievementService;
import com.pettrade.practiceplatform.service.PracticeSessionService;
import com.pettrade.practiceplatform.service.ProductService;
import com.pettrade.practiceplatform.service.ReportingService;
import com.pettrade.practiceplatform.service.SensitiveProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PracticeSessionController.class, AuthController.class, NotificationController.class, InventoryController.class, ReportingController.class, PracticeAchievementController.class, ApprovalController.class, ProductController.class, SensitiveProfileController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthorizationSecurityTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 3, 25, 12, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PracticeSessionService practiceSessionService;
    @MockBean
    private CheckpointService checkpointService;
    @MockBean
    private CurrentUserService currentUserService;
    @MockBean
    private AuthService authService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private NotificationService notificationService;
    @MockBean
    private InventoryService inventoryService;
    @MockBean
    private ReportingService reportingService;
    @MockBean
    private PracticeAchievementService practiceAchievementService;
    @MockBean
    private ApprovalService approvalService;
    @MockBean
    private ProductService productService;
    @MockBean
    private SensitiveProfileService sensitiveProfileService;

    @BeforeEach
    void allowFilterChainToProceed() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class),
                any(ServletResponse.class),
                any(FilterChain.class));
    }

    @Test
    void loginEndpointAllowsAnonymous() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(new AuthResponse("token", "Bearer", "user", List.of("ROLE_REGULAR_BUYER")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("user", "password123"))))
                .andExpect(status().isOk());
    }

    @Test
    void createSessionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/practice/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void reviewerCannotCreateOrMutatePractice() throws Exception {
        mockMvc.perform(post("/api/practice/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/practice/sessions/1/timers/1/command")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TimerCommandRequest(TimerAction.START.name()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/practice/sessions/1/steps/1/complete"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/practice/sessions/1/checkpoints")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void reviewerCanReadPracticeEndpoints() throws Exception {
        when(currentUserService.currentUser()).thenReturn(sampleUser());
        when(practiceSessionService.getSession(eq(1L), any(User.class))).thenReturn(sampleSessionResponse());
        when(checkpointService.loadLatestCheckpoint(eq(1L), any(User.class)))
                .thenReturn(new CheckpointResponse(10L, CheckpointType.AUTO.name(), FIXED_TIME, "{}"));

        mockMvc.perform(get("/api/practice/sessions/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/practice/sessions/1/checkpoints/latest"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void merchantOperatorCanCreateAndMutatePractice() throws Exception {
        when(currentUserService.currentUser()).thenReturn(sampleUser());
        when(practiceSessionService.createSession(any(CreatePracticeSessionRequest.class), any(User.class)))
                .thenReturn(sampleSessionResponse());
        when(practiceSessionService.commandTimer(eq(1L), eq(1L), eq(TimerAction.START), any(User.class)))
                .thenReturn(new TimerStateResponse(1L, "boil", TimerState.RUNNING, 100L, FIXED_TIME.plusSeconds(100)));
        when(practiceSessionService.completeStep(eq(1L), eq(1L), any(User.class)))
                .thenReturn(new StepCompleteResponse(1L, StepStatus.COMPLETED, FIXED_TIME));
        when(checkpointService.saveCheckpoint(eq(1L), any(User.class), any(CheckpointType.class)))
                .thenReturn(new CheckpointResponse(5L, CheckpointType.MANUAL.name(), FIXED_TIME, "{}"));

        mockMvc.perform(post("/api/practice/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/practice/sessions/1/timers/1/command")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TimerCommandRequest(TimerAction.START.name()))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/practice/sessions/1/steps/1/complete"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/practice/sessions/1/checkpoints")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void reportingQueryRejectsOutOfScopeOrganizationUserId() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(reportingService.queryAggregates(any(ReportQueryRequest.class)))
                .thenThrow(new AccessDeniedException("Access denied for requested organization scope"));

        mockMvc.perform(post("/api/reporting/query")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReportQueryRequest(
                                "INVENTORY_CHANGE_EVENTS",
                                999L,
                                null,
                                FIXED_TIME.minusDays(1),
                                FIXED_TIME
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void reportingExportRejectsOutOfScopeOrganizationUserId() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(reportingService.exportXlsx(any(ReportExportRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied for requested organization scope"));

        mockMvc.perform(post("/api/reporting/export")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReportExportRequest(
                                "INVENTORY_CHANGE_EVENTS",
                                999L,
                                null,
                                FIXED_TIME.minusDays(1),
                                FIXED_TIME
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void reportingQueryAllowsOwnOrganizationUserId() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(reportingService.queryAggregates(any(ReportQueryRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/reporting/query")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReportQueryRequest(
                                "INVENTORY_CHANGE_EVENTS",
                                1L,
                                null,
                                FIXED_TIME.minusDays(1),
                                FIXED_TIME
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void reportingQueryAllowsPlatformAdminCrossOrganizationScope() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndRole(100L, "admin", "ROLE_PLATFORM_ADMIN"));
        when(reportingService.queryAggregates(any(ReportQueryRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/reporting/query")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReportQueryRequest(
                                "INVENTORY_CHANGE_EVENTS",
                                777L,
                                null,
                                FIXED_TIME.minusDays(1),
                                FIXED_TIME
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void approvalCreateReturnsConflictOnBusinessRuleViolation() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndRole(100L, "admin", "ROLE_PLATFORM_ADMIN"));
        when(approvalService.createRequest(any(ApprovalRequestCreateDto.class), any(User.class)))
                .thenThrow(new BusinessRuleException("request invalid"));

        mockMvc.perform(post("/api/approvals")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalRequestCreateDto(
                                "INVENTORY_ADJUST",
                                "inventory:item:1",
                                "ADJUST",
                                "{}"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void inventoryStockAdjustReturnsNotFoundForMissingItem() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(inventoryService.adjustStock(eq(999L), any(), any(User.class)))
                .thenThrow(new NotFoundException("Inventory item not found"));

        mockMvc.perform(post("/api/merchant/inventory/items/999/stock")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newQuantity", 3,
                                "reason", "audit"
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void productCreateAndImportSupportHappyAndErrorPaths() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(productService.createProduct(any(ProductUpsertRequest.class), any(User.class)))
                .thenReturn(new ProductView(1L, "P-1", "Dog Food", 2L, 3L, true, List.of()));
        when(productService.batchImport(any(ProductBatchImportRequest.class), any(User.class)))
                .thenThrow(new BusinessRuleException("Duplicate sku_barcode"));

        mockMvc.perform(post("/api/merchant/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductUpsertRequest(
                                "P-1",
                                "Dog Food",
                                2L,
                                3L,
                                List.of(new ProductSkuRequest("BC-1", "Dog Food Small", 9L, null, List.of()))
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/merchant/products/import")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductBatchImportRequest(List.of(
                                new ProductImportRow("P-1", "Dog Food", 2L, 3L, "BC-1", "Dog Food Small", 9L, null)
                        )))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/merchant/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductUpsertRequest(
                                "",
                                "Dog Food",
                                2L,
                                3L,
                                List.of(new ProductSkuRequest("BC-1", "Dog Food Small", 9L, null, List.of()))
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/merchant/products/import")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductBatchImportRequest(List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void productGetReturnsNotFoundWhenMissing() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(productService.getProduct(eq(999L), any(User.class))).thenThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/api/merchant/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void productExportReturnsConflictWhenServiceRejectsBoundaryCase() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(productService.exportCsv(any(User.class))).thenThrow(new BusinessRuleException("Export window too large"));

        mockMvc.perform(get("/api/merchant/products/export"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void sensitiveProfileEndpointValidatesPayload() throws Exception {
        mockMvc.perform(post("/api/profile/sensitive")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SensitiveProfileUpsertRequest("", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void notificationSubscriptionsAcceptNewBusinessEventTypes() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));

        mockMvc.perform(post("/api/notifications/subscriptions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationSubscriptionRequest("ORDER_STATUS_UPDATED", true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/notifications/subscriptions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationSubscriptionRequest("REVIEW_RESULT_PUBLISHED", true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/notifications/subscriptions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationSubscriptionRequest("REPORT_HANDLING_UPDATED", true))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MERCHANT_OPERATOR")
    void internalErrorResponseIsSanitizedAndIncludesTraceId() throws Exception {
        when(currentUserService.currentUser()).thenReturn(userWithIdAndName(1L, "merchant"));
        when(productService.getProduct(eq(500L), any(User.class)))
                .thenThrow(new RuntimeException("db secret leaked message"));

        mockMvc.perform(get("/api/merchant/products/500")
                        .header("X-Correlation-Id", "corr-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.traceId").value("corr-123"));
    }

    private CreatePracticeSessionRequest sampleCreateRequest() {
        return new CreatePracticeSessionRequest(
                "Sample Session",
                List.of(
                        new CreatePracticeSessionRequest.StepInput(
                                "Step 1",
                                "Instruction",
                                120L,
                                List.of(),
                                List.of(),
                                List.of(new CreatePracticeSessionRequest.TimerInput("boil", 120L, List.of()))
                        )
                )
        );
    }

    private PracticeSessionResponse sampleSessionResponse() {
        return new PracticeSessionResponse(
                1L,
                "Sample Session",
                SessionStatus.IN_PROGRESS,
                List.of(
                        new PracticeSessionResponse.StepView(
                                1L,
                                1,
                                "Step 1",
                                "Instruction",
                                StepStatus.PENDING,
                                List.of(),
                                List.of(new PracticeSessionResponse.TimerView(1L, "boil", TimerState.NOT_STARTED, 120L, 120L, null))
                        )
                )
        );
    }

    private User sampleUser() {
        User user = new User();
        user.setUsername("merchant");
        return user;
    }

    private User userWithIdAndName(Long id, String username) {
        User user = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        return user;
    }

    private User userWithIdAndRole(Long id, String username, String roleName) {
        User user = userWithIdAndName(id, username);
        com.pettrade.practiceplatform.domain.Role role = new com.pettrade.practiceplatform.domain.Role();
        org.springframework.test.util.ReflectionTestUtils.setField(role, "id", id + 1000);
        org.springframework.test.util.ReflectionTestUtils.setField(role, "name", roleName);
        user.getRoles().add(role);
        return user;
    }
}
