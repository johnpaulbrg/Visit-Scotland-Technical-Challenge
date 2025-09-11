package com.visitscotland.wishlist.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.visitscotland.wishlist.domain.model.Category;

/**
 * DTO for incoming item creation or removal.
 * Used in POST /wishlist/{userId}/item and DELETE /wishlist/{userId}/item.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ItemRequest
{
    private UUID id; // Optional for creation, required for removal

    @NotBlank(message = "Item title must not be blank")
    @Size(max = 100, message = "Item title must not exceed 100 characters")
    private String title;

    @NotNull(message = "Category must be specified")
    private Category category;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String image;

    private LocalDate date;

    private Map<String, Object> metadata;

    public ItemRequest() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
