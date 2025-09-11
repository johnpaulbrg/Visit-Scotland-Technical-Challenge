package com.visitscotland.wishlist.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.visitscotland.wishlist.domain.model.Category;
import com.visitscotland.wishlist.domain.model.Item;

/**
 * DTO for outbound item responses.
 * Used to serialize Item into transport-safe format.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ItemResponse
{
    private final UUID id;
    private final String title;
    private final Category category;
    private final String description;
    private final String image;
    private final LocalDate date;
    private final Map<String, Object> metadata;

    public ItemResponse(UUID id, String title, Category category,
                        String description, String image,
                        LocalDate date, Map<String, Object> metadata) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.image = image;
        this.date = date;
        this.metadata = metadata;
    }

    public static ItemResponse from(Item item) {
        return new ItemResponse(
            item.getId(), // required
            item.getTitle(), // required
            item.getCategory(), // required
            item.getDescription().orElse(null), // Optional
            item.getImage().orElse(null), // Optional
            item.getDate().orElse(null), // Optional
            item.getMetadata().orElse(null) // Optional
        );
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public LocalDate getDate() {
        return date;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
