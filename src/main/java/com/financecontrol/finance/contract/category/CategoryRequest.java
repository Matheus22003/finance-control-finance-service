package com.financecontrol.finance.contract.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required.")
        @Size(max = 80, message = "Category name must contain at most 80 characters.")
        String name) {
}
