package ru.practicum.category.service;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;

public interface CategoryService {
    CategoryDto saveCategory(NewCategoryDto dto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, NewCategoryDto dto);
}
