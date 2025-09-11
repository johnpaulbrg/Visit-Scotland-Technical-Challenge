package com.visitscotland.wishlist.domain.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Represents a single wish list item.
 * Immutable and equality-safe for use in Set<Item> collections.
 * Optional fields include description, image, date, and metadata.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@Component
@Scope("prototype")
public final class Item
{
    private static final Logger log = LoggerFactory.getLogger(Item.class);

    private final UUID id;
    private final String title;
    private final Category category;
    private final String description;
    private final String image;
    private final LocalDate date;
    private final Map<String, Object> metadata;

    public Item(UUID id, String title, Category category,
                String description, String image,
                LocalDate date, Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "Item ID must not be null");
        this.title = Objects.requireNonNull(title, "Item title must not be null");
        this.category = Objects.requireNonNull(category, "Item category must not be null");
        this.description = description;
        this.image = image;
        this.date = date;
        this.metadata = metadata;

        log.debug("Constructed Item: title='{}', category={}, id={}", title, category, id);
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

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getImage() {
        return Optional.ofNullable(image);
    }

    public Optional<LocalDate> getDate() {
        return Optional.ofNullable(date);
    }

    public Optional<Map<String, Object>> getMetadata() {
        return Optional.ofNullable(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item other = (Item) o;
        boolean match = id.equals(other.id) &&
                        title.equalsIgnoreCase(other.title) &&
                        category == other.category &&
                        Objects.equals(description, other.description) &&
                        Objects.equals(image, other.image) &&
                        Objects.equals(date, other.date) &&
                        Objects.equals(metadata, other.metadata);

        log.debug("equals: comparing Item '{}' to '{}': {}", this.id, other.id, match);
        return match;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title.toLowerCase(),
            category,
            description,
            image,
            date,
            metadata
        );
    }
}
