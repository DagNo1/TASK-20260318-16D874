package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.inventory.InventoryItemUpsertRequest;
import com.pettrade.practiceplatform.api.product.ProductSkuRequest;
import com.pettrade.practiceplatform.api.product.ProductUpsertRequest;
import com.pettrade.practiceplatform.domain.Brand;
import com.pettrade.practiceplatform.domain.Category;
import com.pettrade.practiceplatform.domain.Product;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.repository.AttributeSpecRepository;
import com.pettrade.practiceplatform.repository.BrandRepository;
import com.pettrade.practiceplatform.repository.CategoryRepository;
import com.pettrade.practiceplatform.repository.ProductRepository;
import com.pettrade.practiceplatform.repository.ProductSkuRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductSkuRepository skuRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AttributeSpecRepository attributeSpecRepository;
    @Mock private InventoryService inventoryService;
    @Mock private EntityManager entityManager;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(
                productRepository,
                skuRepository,
                brandRepository,
                categoryRepository,
                attributeSpecRepository,
                inventoryService,
                entityManager
        );
    }

    @Test
    void createProductIntegratesSkuStockWithInventoryAlerts() {
        User merchant = user(1L);
        Brand brand = brand(2L);
        Category category = category(3L, 2);

        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 100L);
            return p;
        });
        when(skuRepository.save(any())).thenAnswer(i -> {
            var sku = i.getArgument(0, com.pettrade.practiceplatform.domain.ProductSku.class);
            ReflectionTestUtils.setField(sku, "id", 200L);
            return sku;
        });

        service.createProduct(new ProductUpsertRequest(
                "P-1",
                "Dog Food",
                2L,
                3L,
                List.of(new ProductSkuRequest("BC-1", "Dog Food Small", 9L, null, List.of()))
        ), merchant);

        ArgumentCaptor<InventoryItemUpsertRequest> requestCaptor = ArgumentCaptor.forClass(InventoryItemUpsertRequest.class);
        verify(inventoryService).upsertItem(requestCaptor.capture(), any(User.class));
        assertEquals("BC-1", requestCaptor.getValue().sku());
        assertEquals(9L, requestCaptor.getValue().stockQuantity());
    }

    @Test
    void rejectsCategoryDepthGreaterThanFour() {
        User merchant = user(1L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, 5)));

        assertThrows(BusinessRuleException.class, () -> service.createProduct(new ProductUpsertRequest(
                "P-2",
                "Cat Food",
                2L,
                5L,
                List.of(new ProductSkuRequest("BC-2", "Cat Food", 20L, 10L, List.of()))
        ), merchant));
    }

    @Test
    void mapsDuplicateSkuBarcodeToBusinessRuleException() {
        User merchant = user(1L);
        Brand brand = brand(2L);
        Category category = category(3L, 1);

        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 110L);
            return p;
        });
        when(skuRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThrows(BusinessRuleException.class, () -> service.createProduct(new ProductUpsertRequest(
                "P-3",
                "Bird Feed",
                2L,
                3L,
                List.of(new ProductSkuRequest("BC-X", "Bird Feed", 10L, null, List.of()))
        ), merchant));
    }

    @Test
    void exportCsvReturnsHeaderOnlyWhenNoListedProducts() {
        when(productRepository.findByMerchantUserIdAndListedOrderByIdDesc(1L, true)).thenReturn(List.of());

        String csv = service.exportCsv(user(1L));
        assertTrue(csv.startsWith("productCode,productName,brandId,categoryId,skuBarcode,skuName,stockQuantity,alertThreshold"));
    }

    @Test
    void batchImportFailsWhenRowsHaveMissingProductCodeBoundary() {
        User merchant = user(1L);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, 2)));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(brand(2L)));
        when(productRepository.save(any(Product.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThrows(BusinessRuleException.class, () -> service.batchImport(
                new com.pettrade.practiceplatform.api.product.ProductBatchImportRequest(List.of(
                        new com.pettrade.practiceplatform.api.product.ProductImportRow("", "Bad", 2L, 3L, "BC-1", "SKU", 2L, 1L)
                )),
                merchant
        ));
    }

    private User user(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername("merchant");
        return user;
    }

    private Brand brand(Long id) {
        Brand brand = new Brand();
        ReflectionTestUtils.setField(brand, "id", id);
        brand.setName("Brand");
        return brand;
    }

    private Category category(Long id, int depth) {
        Category c = new Category();
        ReflectionTestUtils.setField(c, "id", id);
        c.setName("Category");
        c.setDepth(depth);
        return c;
    }
}
