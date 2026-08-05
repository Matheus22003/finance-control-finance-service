package com.financecontrol.finance.api;

import java.util.List;

import com.financecontrol.finance.domain.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        path = "/api/v1/finance/categories",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Categories", description = "Available expense categories")
public class CategoryController {

    @GetMapping
    @Operation(summary = "List all expense categories")
    public List<Category> findAll() {
        return List.of(Category.values());
    }
}
