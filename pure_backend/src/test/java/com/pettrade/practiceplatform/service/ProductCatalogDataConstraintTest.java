package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.product.ProductSkuRequest;
import com.pettrade.practiceplatform.api.product.ProductUpsertRequest;
import com.pettrade.practiceplatform.domain.Brand;
import com.pettrade.practiceplatform.domain.Category;
import com.pettrade.practiceplatform.domain.Product;
import com.pettrade.practiceplatform.domain.ProductSku;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.repository.BrandRepository;
import com.pettrade.practiceplatform.repository.CategoryRepository;
import com.pettrade.practiceplatform.repository.ProductRepository;
import com.pettrade.practiceplatform.repository.ProductSkuRepository;
import com.pettrade.practiceplatform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class ProductCatalogDataConstraintTest {

    @Autowired private UserRepository userRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductSkuRepository productSkuRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void productCodeMustBeUnique() {
        User merchant = createUser("merchant-a");
        Brand brand = createBrand("Brand-A");
        Category category = createCategory("Cat-A", 1);

        productRepository.save(product("P-CODE", merchant, brand, category));
        assertThrows(DataIntegrityViolationException.class,
                () -> productRepository.saveAndFlush(product("P-CODE", merchant, brand, category)));
    }

    @Test
    void skuBarcodeMustBeUnique() {
        User merchant = createUser("merchant-b");
        Brand brand = createBrand("Brand-B");
        Category category = createCategory("Cat-B", 1);

        Product p1 = productRepository.save(product("P-1", merchant, brand, category));
        Product p2 = productRepository.save(product("P-2", merchant, brand, category));
        productSkuRepository.save(productSku("BC-UNIQUE", p1));

        assertThrows(DataIntegrityViolationException.class,
                () -> productSkuRepository.saveAndFlush(productSku("BC-UNIQUE", p2)));
    }

    @Test
    void categoryDepthConstraintRejectsDepthAboveFour() {
        User merchant = createUser("merchant-depth");
        Brand brand = createBrand("Brand-Depth");

        Category category = new Category();
        category.setName("TooDeep");
        category.setDepth(5);
        category = categoryRepository.saveAndFlush(category);

        ProductService productService = new ProductService(
                productRepository,
                productSkuRepository,
                brandRepository,
                categoryRepository,
                null,
                null,
                entityManager
        );

        Category finalCategory = category;
        assertThrows(BusinessRuleException.class, () -> productService.createProduct(new ProductUpsertRequest(
                "P-DEPTH",
                "Depth Product",
                brand.getId(),
                finalCategory.getId(),
                List.of(new ProductSkuRequest("BC-DEPTH", "Depth SKU", 5L, null, List.of()))
        ), merchant));
    }

    private User createUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOHiR0Jw4u7v6/9erjRzCQXDpUe1koX6.");
        u.setEnabled(true);
        return userRepository.save(u);
    }

    private Brand createBrand(String name) {
        Brand b = new Brand();
        b.setName(name);
        return brandRepository.save(b);
    }

    private Category createCategory(String name, int depth) {
        Category c = new Category();
        c.setName(name);
        c.setDepth(depth);
        return categoryRepository.save(c);
    }

    private Product product(String code, User merchant, Brand brand, Category category) {
        Product p = new Product();
        p.setMerchantUser(merchant);
        p.setProductCode(code);
        p.setName("Product " + code);
        p.setBrand(brand);
        p.setCategory(category);
        p.setListed(true);
        return p;
    }

    private ProductSku productSku(String barcode, Product product) {
        ProductSku sku = new ProductSku();
        sku.setProduct(product);
        sku.setSkuBarcode(barcode);
        sku.setName("Sku " + barcode);
        sku.setStockQuantity(8L);
        sku.setAlertThreshold(10L);
        return sku;
    }
}
