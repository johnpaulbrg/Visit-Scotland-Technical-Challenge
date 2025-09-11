package com.visitscotland.wishlist.domain.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Thread-safe, per-user wish list.
 * Encapsulates a mutable set of Item instances with full-field deduplication.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@Component
@Scope("prototype")
public final class WishList
{
    private static final Logger log = LoggerFactory.getLogger(WishList.class);

    private final User user;
    private final Set<Item> items = new HashSet<>();

    public WishList(User user) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        log.debug("Initialized empty WishList for user {}", user.getId());
    }

    public UUID getUserId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    public synchronized Set<Item> getItems() {
        return new HashSet<>(items); // Defensive copy
    }

    public synchronized boolean addItem(Item item) {
        Objects.requireNonNull(item, "item must not be null");
        boolean added = items.add(item);
        if (added) {
            log.info("Added item '{}' to wish list for user {}", item.getTitle(), user.getId());
        } else {
            log.warn("Duplicate item '{}' rejected for user {}", item.getTitle(), user.getId());
        }
        return added;
    }

    public synchronized boolean removeItem(UUID itemId) {
        boolean removed = items.removeIf(i -> i.getId().equals(itemId));
        if (removed) {
            log.info("Removed item with ID {} from wish list for user {}", itemId, user.getId());
        } else {
            log.warn("No item found with ID {} for user {}", itemId, user.getId());
        }
        return removed;
    }

    public synchronized boolean removeItem(Item item) {
        boolean removed = items.remove(item);
        if (removed) {
            log.info("Removed item '{}' by reference from wish list for user {}", item.getTitle(), user.getId());
        } else {
            log.warn("Item '{}' not found by reference for user {}", item.getTitle(), user.getId());
        }
        return removed;
    }

    public synchronized void clear() {
        int count = items.size();
        items.clear();
        log.info("Cleared {} items from wish list for user {}", count, user.getId());
    }

    public synchronized Set<Item> filterByCategory(Category category) {
        Set<Item> filtered = items.stream()
                                  .filter(i -> i.getCategory() == category)
                                  .collect(Collectors.toSet());
        log.debug("Filtered {} items by category '{}' for user {}", filtered.size(), category, user.getId());
        return filtered;
    }

    public synchronized boolean containsItem(UUID itemId) {
        boolean found = items.stream().anyMatch(i -> i.getId().equals(itemId));
        log.debug("Checked presence of item ID {} for user {}: {}", itemId, user.getId(), found);
        return found;
    }

    public synchronized int size() {
        return items.size();
    }
}
