package com.financecontrol.finance.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.financecontrol.finance.contract.category.CategoryRequest;
import com.financecontrol.finance.contract.category.CategoryResponse;
import com.financecontrol.finance.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        path = "/api/v1/finance/categories",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Categories", description = "Available expense categories")
public class CategoryController {

    private static final String USER_ID_HEADER = "X-Finance-Control-User-Id";

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List all expense categories")
    public List<CategoryResponse> findAll(@RequestHeader(USER_ID_HEADER) UUID userId) {
        return categoryService.findAll(userId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a custom expense category")
    public ResponseEntity<CategoryResponse> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody CategoryRequest request) {
        var category = categoryService.create(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/finance/categories/" + category.id()))
                .body(category);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rename a custom expense category")
    public CategoryResponse update(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an unused custom expense category")
    public ResponseEntity<Void> delete(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PathVariable Long id) {
        categoryService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
