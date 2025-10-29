package com.visitscotland.wishlist.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for outbound wish list responses.
 * Wraps a user ID and list of items.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WishListResponse
{
    private final UUID userId;
    private final List<ItemResponse> items;

    public WishListResponse(UUID userId, List<ItemResponse> items) {
        this.userId = userId;
        this.items = items;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<ItemResponse> getItems() {
        return items;
    }
}
