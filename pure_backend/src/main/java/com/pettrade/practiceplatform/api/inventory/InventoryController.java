package com.pettrade.practiceplatform.api.inventory;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final CurrentUserService currentUserService;

    public InventoryController(InventoryService inventoryService, CurrentUserService currentUserService) {
        this.inventoryService = inventoryService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Create or update inventory item")
    public ResponseEntity<InventoryItemView> upsertItem(@Valid @RequestBody InventoryItemUpsertRequest request) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(inventoryService.upsertItem(request, merchant));
    }

    @PostMapping("/items/{itemId}/stock")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Adjust inventory stock")
    public ResponseEntity<InventoryItemView> adjustStock(
            @PathVariable Long itemId,
            @Valid @RequestBody InventoryStockAdjustRequest request
    ) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(inventoryService.adjustStock(itemId, request, merchant));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "List inventory alert history")
    public ResponseEntity<List<InventoryAlertView>> listAlerts() {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(inventoryService.listAlerts(merchant));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "List inventory logs")
    public ResponseEntity<List<InventoryLogView>> listLogs() {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(inventoryService.listLogs(merchant));
    }
}
