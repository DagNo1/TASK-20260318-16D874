package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.inventory.InventoryItemUpsertRequest;
import com.pettrade.practiceplatform.api.product.ProductBatchImportRequest;
import com.pettrade.practiceplatform.api.product.ProductImportRow;
import com.pettrade.practiceplatform.api.product.ProductListResponse;
import com.pettrade.practiceplatform.api.product.ProductSkuRequest;
import com.pettrade.practiceplatform.api.product.ProductSkuView;
import com.pettrade.practiceplatform.api.product.ProductUpsertRequest;
import com.pettrade.practiceplatform.api.product.ProductView;
import com.pettrade.practiceplatform.domain.AttributeSpec;
import com.pettrade.practiceplatform.domain.Brand;
import com.pettrade.practiceplatform.domain.Category;
import com.pettrade.practiceplatform.domain.Product;
import com.pettrade.practiceplatform.domain.ProductSku;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.AttributeSpecRepository;
import com.pettrade.practiceplatform.repository.BrandRepository;
import com.pettrade.practiceplatform.repository.CategoryRepository;
import com.pettrade.practiceplatform.repository.ProductRepository;
import com.pettrade.practiceplatform.repository.ProductSkuRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeSpecRepository attributeSpecRepository;
    private final InventoryService inventoryService;
    private final EntityManager entityManager;

    public ProductService(
            ProductRepository productRepository,
            ProductSkuRepository skuRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            AttributeSpecRepository attributeSpecRepository,
            InventoryService inventoryService,
            EntityManager entityManager
    ) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.attributeSpecRepository = attributeSpecRepository;
        this.inventoryService = inventoryService;
        this.entityManager = entityManager;
    }

    @Transactional
    public ProductView createProduct(ProductUpsertRequest request, User merchant) {
        return saveProduct(null, request, merchant);
    }

    @Transactional
    public ProductView updateProduct(Long productId, ProductUpsertRequest request, User merchant) {
        return saveProduct(productId, request, merchant);
    }

    @Transactional(readOnly = true)
    public ProductView getProduct(Long productId, User merchant) {
        Product product = productRepository.findByIdAndMerchantUserId(productId, merchant.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return toView(product, skuRepository.findByProductIdOrderByIdAsc(product.getId()));
    }

    @Transactional(readOnly = true)
    public ProductListResponse listProducts(boolean listed, User merchant) {
        List<Product> products = productRepository.findByMerchantUserIdAndListedOrderByIdDesc(merchant.getId(), listed);
        Map<Long, List<ProductSku>> skuMap = skuRepository.findAll().stream()
                .filter(s -> s.getProduct().getMerchantUser().getId().equals(merchant.getId()))
                .collect(Collectors.groupingBy(s -> s.getProduct().getId()));
        return new ProductListResponse(products.stream().map(p -> toView(p, skuMap.getOrDefault(p.getId(), List.of()))).toList());
    }

    @Transactional
    public ProductView listProduct(Long productId, User merchant) {
        Product product = productRepository.findByIdAndMerchantUserId(productId, merchant.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        product.setListed(true);
        return toView(productRepository.save(product), skuRepository.findByProductIdOrderByIdAsc(productId));
    }

    @Transactional
    public ProductView delistProduct(Long productId, User merchant) {
        Product product = productRepository.findByIdAndMerchantUserId(productId, merchant.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        product.setListed(false);
        return toView(productRepository.save(product), skuRepository.findByProductIdOrderByIdAsc(productId));
    }

    @Transactional
    public ProductListResponse batchImport(ProductBatchImportRequest request, User merchant) {
        Map<String, List<ProductImportRow>> grouped = request.rows().stream()
                .collect(Collectors.groupingBy(ProductImportRow::productCode, LinkedHashMap::new, Collectors.toList()));
        List<ProductView> views = new ArrayList<>();
        for (List<ProductImportRow> rows : grouped.values()) {
            ProductImportRow first = rows.get(0);
            ProductUpsertRequest upsert = new ProductUpsertRequest(
                    first.productCode(),
                    first.productName(),
                    first.brandId(),
                    first.categoryId(),
                    rows.stream().map(r -> new ProductSkuRequest(
                            r.skuBarcode(),
                            r.skuName(),
                            r.stockQuantity(),
                            r.alertThreshold(),
                            List.of()
                    )).toList()
            );
            views.add(createProduct(upsert, merchant));
        }
        return new ProductListResponse(views);
    }

    @Transactional(readOnly = true)
    public String exportCsv(User merchant) {
        List<Product> products = productRepository.findByMerchantUserIdAndListedOrderByIdDesc(merchant.getId(), true);
        StringBuilder csv = new StringBuilder();
        csv.append("productCode,productName,brandId,categoryId,skuBarcode,skuName,stockQuantity,alertThreshold\n");
        for (Product p : products) {
            List<ProductSku> skus = skuRepository.findByProductIdOrderByIdAsc(p.getId());
            for (ProductSku sku : skus) {
                csv.append(p.getProductCode()).append(',')
                        .append(p.getName()).append(',')
                        .append(p.getBrand().getId()).append(',')
                        .append(p.getCategory().getId()).append(',')
                        .append(sku.getSkuBarcode()).append(',')
                        .append(sku.getName()).append(',')
                        .append(sku.getStockQuantity()).append(',')
                        .append(sku.getAlertThreshold() == null ? "" : sku.getAlertThreshold())
                        .append('\n');
            }
        }
        return csv.toString();
    }

    private ProductView saveProduct(Long productId, ProductUpsertRequest request, User merchant) {
        validateCategoryDepth(request.categoryId());
        Product product = productId == null
                ? new Product()
                : productRepository.findByIdAndMerchantUserId(productId, merchant.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new NotFoundException("Brand not found"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        product.setMerchantUser(merchant);
        product.setProductCode(request.productCode());
        product.setName(request.name());
        product.setBrand(brand);
        product.setCategory(category);
        if (productId == null) {
            product.setListed(true);
        }

        Product saved;
        try {
            saved = productRepository.save(product);
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException("Duplicate product_code");
        }

        if (productId != null) {
            skuRepository.findByProductIdOrderByIdAsc(saved.getId()).forEach(skuRepository::delete);
            entityManager.flush();
        }

        List<AttributeSpec> allSpecs = resolveAttributeSpecs(request.skus());
        Map<Long, AttributeSpec> specMap = new HashMap<>();
        for (AttributeSpec spec : allSpecs) {
            specMap.put(spec.getId(), spec);
        }

        List<ProductSku> savedSkus = new ArrayList<>();
        for (ProductSkuRequest skuRequest : request.skus()) {
            ProductSku sku = new ProductSku();
            sku.setProduct(saved);
            sku.setSkuBarcode(skuRequest.skuBarcode());
            sku.setName(skuRequest.name());
            sku.setStockQuantity(skuRequest.stockQuantity());
            sku.setAlertThreshold(skuRequest.alertThreshold());
            if (skuRequest.attributeSpecIds() != null) {
                for (Long id : skuRequest.attributeSpecIds()) {
                    AttributeSpec spec = specMap.get(id);
                    if (spec == null) {
                        throw new NotFoundException("Attribute spec not found");
                    }
                    sku.getAttributeSpecs().add(spec);
                }
            }
            ProductSku skuSaved;
            try {
                skuSaved = skuRepository.save(sku);
                entityManager.flush();
            } catch (DataIntegrityViolationException ex) {
                throw new BusinessRuleException("Duplicate sku_barcode");
            }
            savedSkus.add(skuSaved);

            inventoryService.upsertItem(new InventoryItemUpsertRequest(
                    skuSaved.getSkuBarcode(),
                    skuSaved.getName(),
                    skuSaved.getStockQuantity(),
                    skuSaved.getAlertThreshold()
            ), merchant);
        }

        return toView(saved, savedSkus);
    }

    private List<AttributeSpec> resolveAttributeSpecs(List<ProductSkuRequest> skuRequests) {
        List<Long> ids = skuRequests.stream()
                .flatMap(s -> s.attributeSpecIds() == null ? List.<Long>of().stream() : s.attributeSpecIds().stream())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return attributeSpecRepository.findByIdIn(ids);
    }

    private void validateCategoryDepth(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (category.getDepth() > 4) {
            throw new BusinessRuleException("Category depth must be <= 4");
        }
    }

    private ProductView toView(Product product, List<ProductSku> skus) {
        return new ProductView(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getBrand().getId(),
                product.getCategory().getId(),
                product.isListed(),
                skus.stream().map(s -> new ProductSkuView(
                        s.getId(),
                        s.getSkuBarcode(),
                        s.getName(),
                        s.getStockQuantity(),
                        s.getAlertThreshold(),
                        s.getAttributeSpecs().stream().map(AttributeSpec::getId).toList()
                )).toList()
        );
    }
}
