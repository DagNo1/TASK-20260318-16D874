package com.pettrade.practiceplatform.api.product;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;
    private final CurrentUserService currentUserService;

    public ProductController(ProductService productService, CurrentUserService currentUserService) {
        this.productService = productService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Create product with SKUs")
    public ResponseEntity<ProductView> create(@Valid @RequestBody ProductUpsertRequest request) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(productService.createProduct(request, merchant));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Update product and SKUs")
    public ResponseEntity<ProductView> update(@PathVariable Long productId, @Valid @RequestBody ProductUpsertRequest request) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(productService.updateProduct(productId, request, merchant));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Get product by id")
    public ResponseEntity<ProductView> get(@PathVariable Long productId) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(productService.getProduct(productId, merchant));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "List products by listed state")
    public ResponseEntity<ProductListResponse> list(@RequestParam(defaultValue = "true") boolean listed) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(productService.listProducts(listed, merchant));
    }

    @PostMapping("/{productId}/list")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "List product")
    public ResponseEntity<ProductView> listProduct(@PathVariable Long productId) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(productService.listProduct(productId, merchant));
    }

    @PostMapping("/{productId}/delist")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Delist product")
    public ResponseEntity<ProductView> delistProduct(@PathVariable Long productId) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(productService.delistProduct(productId, merchant));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Batch import products")
    public ResponseEntity<ProductListResponse> batchImport(@Valid @RequestBody ProductBatchImportRequest request) {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok(productService.batchImport(request, merchant));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR')")
    @Operation(summary = "Export products as CSV")
    public ResponseEntity<String> export() {
        User merchant = currentUserService.currentUser();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(productService.exportCsv(merchant));
    }
}
