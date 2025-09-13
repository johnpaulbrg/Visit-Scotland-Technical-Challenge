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

    public static final String USER_NOT_NULL = "user must not be null";
    public static final String ITEM_NOT_NULL = "item must not be null";
    public static final String INIT_EMPTY_WISHLIST = "Initialized empty WishList for user {}";
    public static final String ADD_ITEM_SUCCESS = "Added item '{}' to wish list for user {}";
    public static final String ADD_ITEM_DUPLICATE = "Duplicate item '{}' rejected for user {}";
    public static final String REMOVE_ITEM_BY_ID_SUCCESS = "Removed item with ID {} from wish list for user {}";
    public static final String REMOVE_ITEM_BY_ID_NOT_FOUND = "No item found with ID {} for user {}";
    public static final String REMOVE_ITEM_BY_REF_SUCCESS = "Removed item '{}' by reference from wish list for user {}";
    public static final String REMOVE_ITEM_BY_REF_NOT_FOUND = "Item '{}' not found by reference for user {}";
    public static final String CLEAR_WISHLIST = "Cleared {} items from wish list for user {}";
    public static final String FILTER_BY_CATEGORY = "Filtered {} items by category '{}' for user {}";
    public static final String CONTAINS_ITEM_CHECK = "Checked presence of item ID {} for user {}: {}";

    private final User user;
    private final Set<Item> items = new HashSet<>();

    public WishList(User user) {
        this.user = Objects.requireNonNull(user, USER_NOT_NULL);
        log.debug(INIT_EMPTY_WISHLIST, user.getId());
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
        Objects.requireNonNull(item, ITEM_NOT_NULL);
        boolean added = items.add(item);
        if (added) {
            log.info(ADD_ITEM_SUCCESS, item.getTitle(), user.getId());
        } else {
            log.warn(ADD_ITEM_DUPLICATE, item.getTitle(), user.getId());
        }
        return added;
    }

    public synchronized boolean removeItem(UUID itemId) {
        boolean removed = items.removeIf(i -> i.getId().equals(itemId));
        if (removed) {
            log.info(REMOVE_ITEM_BY_ID_SUCCESS, itemId, user.getId());
        } else {
            log.warn(REMOVE_ITEM_BY_ID_NOT_FOUND, itemId, user.getId());
        }
        return removed;
    }

    public synchronized boolean removeItem(Item item) {
        boolean removed = items.remove(item);
        if (removed) {
            log.info(REMOVE_ITEM_BY_REF_SUCCESS, item.getTitle(), user.getId());
        } else {
            log.warn(REMOVE_ITEM_BY_REF_NOT_FOUND, item.getTitle(), user.getId());
        }
        return removed;
    }

    public synchronized void clear() {
        int count = items.size();
        items.clear();
        log.info(CLEAR_WISHLIST, count, user.getId());
    }

    public synchronized Set<Item> filterByCategory(Category category) {
        Set<Item> filtered = items.stream()
                                  .filter(i -> i.getCategory() == category)
                                  .collect(Collectors.toSet());
        log.debug(FILTER_BY_CATEGORY, filtered.size(), category, user.getId());
        return filtered;
    }

    public synchronized boolean containsItem(UUID itemId) {
        boolean found = items.stream().anyMatch(i -> i.getId().equals(itemId));
        log.debug(CONTAINS_ITEM_CHECK, itemId, user.getId(), found);
        return found;
    }

    public synchronized int size() {
        return items.size();
    }
}
