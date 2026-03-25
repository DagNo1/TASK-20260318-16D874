package com.pettrade.practiceplatform.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.auth.AuthController;
import com.pettrade.practiceplatform.api.auth.AuthRequest;
import com.pettrade.practiceplatform.api.auth.AuthResponse;
import com.pettrade.practiceplatform.api.approval.ApprovalController;
import com.pettrade.practiceplatform.api.achievement.PracticeAchievementController;
import com.pettrade.practiceplatform.api.inventory.InventoryController;
import com.pettrade.practiceplatform.api.notification.NotificationController;
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
import com.pettrade.practiceplatform.security.JwtAuthenticationFilter;
import com.pettrade.practiceplatform.service.AuthService;
import com.pettrade.practiceplatform.service.ApprovalService;
import com.pettrade.practiceplatform.service.CheckpointService;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.InventoryService;
import com.pettrade.practiceplatform.service.NotificationService;
import com.pettrade.practiceplatform.service.PracticeAchievementService;
import com.pettrade.practiceplatform.service.PracticeSessionService;
import com.pettrade.practiceplatform.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PracticeSessionController.class, AuthController.class, NotificationController.class, InventoryController.class, ReportingController.class, PracticeAchievementController.class, ApprovalController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthorizationSecurityTest {

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

    @BeforeEach
    void allowFilterChainToProceed() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
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
                .andExpect(status().isUnauthorized());
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
                .thenReturn(new CheckpointResponse(10L, CheckpointType.AUTO.name(), LocalDateTime.now(), "{}"));

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
                .thenReturn(new TimerStateResponse(1L, "boil", TimerState.RUNNING, 100L, LocalDateTime.now().plusSeconds(100)));
        when(practiceSessionService.completeStep(eq(1L), eq(1L), any(User.class)))
                .thenReturn(new StepCompleteResponse(1L, StepStatus.COMPLETED, LocalDateTime.now()));
        when(checkpointService.saveCheckpoint(eq(1L), any(User.class), any(CheckpointType.class)))
                .thenReturn(new CheckpointResponse(5L, CheckpointType.MANUAL.name(), LocalDateTime.now(), "{}"));

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
}
