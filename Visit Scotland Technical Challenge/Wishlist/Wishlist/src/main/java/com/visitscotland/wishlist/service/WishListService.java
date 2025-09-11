package com.visitscotland.wishlist.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.visitscotland.wishlist.domain.model.Category;
import com.visitscotland.wishlist.domain.model.Item;
import com.visitscotland.wishlist.domain.model.User;
import com.visitscotland.wishlist.domain.model.WishList;

/**
 * Thread-safe, in-memory wish list service.
 * Operates on domain-safe User objects and maintains per-user wish lists.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@Service
public final class WishListService
{
    private static final Logger log = LoggerFactory.getLogger(WishListService.class);

    private final ConcurrentMap<UUID, WishList> wishLists = new ConcurrentHashMap<>();
    
    /**
     * Spring provider for creating new {@link WishList} instances.
     */
    private final ObjectProvider<WishList> wishListProvider;

    /**
     * Constructs the service with injected WishList.
     */
    public WishListService(ObjectProvider<WishList> wishListProvider) {
        this.wishListProvider = wishListProvider;
    }
    
    public void createWishList(User user) {
        UUID userId = user.getId();
        wishLists.computeIfAbsent(userId, id -> {
            log.info("Creating new wish list for user '{}'", userId);
            return wishListProvider.getObject(user);
        });
    }
    
    public void deleteWishList(User user) {
        UUID userId = user.getId();
        if (wishLists.remove(userId) != null) {
            log.info("Deleted wish list for user '{}'", userId);
        } else {
            log.info("No wish list found to delete for user '{}'", userId);
        }
    }

    public boolean hasWishList(User user) {
        UUID userId = user.getId();
        boolean exists = wishLists.containsKey(userId);
        log.debug("Wish list existence check for user '{}': {}", userId, exists);
        return exists;
    }

    public Set<Item> getItems(User user) {
        UUID userId = user.getId();
        log.debug("Retrieving all items for user '{}'", userId);
        return getOrCreate(user).getItems();
    }

    public Set<Item> getItemsByCategory(User user, Category category) {
        UUID userId = user.getId();
        log.debug("Filtering items for user '{}' by category '{}'", userId, category);
        return getOrCreate(user).filterByCategory(category);
    }

    public boolean addItem(User user, Item item) {
        UUID userId = user.getId();
        boolean added = getOrCreate(user).addItem(item);
        log.info("Add item '{}' for user '{}': {}", item.getTitle(), userId, added ? "added" : "duplicate");
        return added;
    }

    public boolean removeItem(User user, Item item) {
        UUID userId = user.getId();
        boolean removed = getOrCreate(user).removeItem(item);
        log.info("Remove item '{}' by reference for user '{}': {}", item.getTitle(), userId, removed ? "removed" : "not found");
        return removed;
    }

    public boolean removeItemById(User user, UUID itemId) {
        UUID userId = user.getId();
        boolean removed = getOrCreate(user).removeItem(itemId);
        log.info("Remove item by ID '{}' for user '{}': {}", itemId, userId, removed ? "removed" : "not found");
        return removed;
    }

    public boolean containsItem(User user, UUID itemId) {
        UUID userId = user.getId();
        boolean exists = getOrCreate(user).containsItem(itemId);
        log.debug("Check existence of item ID '{}' for user '{}': {}", itemId, userId, exists);
        return exists;
    }

    public int getWishListSize(User user) {
        UUID userId = user.getId();
        int size = getOrCreate(user).size();
        log.debug("Wish list size for user '{}': {}", userId, size);
        return size;
    }

    public void clearWishList(User user) {
        UUID userId = user.getId();
        getOrCreate(user).clear();
        log.info("Cleared wish list for user '{}'", userId);
    }

    private WishList getOrCreate(User user) {
        UUID userId = user.getId();
        return wishLists.computeIfAbsent(userId, id -> {
            log.debug("Auto-creating wish list for user '{}'", userId);
            return wishListProvider.getObject(user);
        });
    }
}
