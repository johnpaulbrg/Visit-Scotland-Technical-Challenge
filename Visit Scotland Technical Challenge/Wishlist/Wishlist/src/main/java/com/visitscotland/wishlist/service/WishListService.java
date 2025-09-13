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

    public static final String ADDED = "added";
    public static final String DUPLICATE = "duplicate";
    public static final String REMOVED = "removed";
    public static final String NOT_FOUND = "not found";
    public static final String CREATE_WISHLIST = "Creating new wish list for user '{}'";
    public static final String DELETE_WISHLIST_SUCCESS = "Deleted wish list for user '{}'";
    public static final String DELETE_WISHLIST_NOT_FOUND = "No wish list found to delete for user '{}'";
    public static final String HAS_WISHLIST_CHECK = "Wish list existence check for user '{}': {}";
    public static final String RETRIEVE_ITEMS = "Retrieving all items for user '{}'";
    public static final String FILTER_ITEMS_BY_CATEGORY = "Filtering items for user '{}' by category '{}'";
    public static final String ADD_ITEM_RESULT = "Add item '{}' for user '{}': {}";
    public static final String REMOVE_ITEM_BY_REF_RESULT = "Remove item '{}' by reference for user '{}': {}";
    public static final String REMOVE_ITEM_BY_ID_RESULT = "Remove item by ID '{}' for user '{}': {}";
    public static final String CONTAINS_ITEM_CHECK = "Check existence of item ID '{}' for user '{}': {}";
    public static final String WISHLIST_SIZE = "Wish list size for user '{}': {}";
    public static final String CLEAR_WISHLIST = "Cleared wish list for user '{}'";
    public static final String AUTO_CREATE_WISHLIST = "Auto-creating wish list for user '{}'";

    private final ConcurrentMap<UUID, WishList> wishLists = new ConcurrentHashMap<>();

    // Spring provider for creating new {@link WishList} instances
    private final ObjectProvider<WishList> wishListProvider;

    // Constructs the service with injected WishList
    public WishListService(ObjectProvider<WishList> wishListProvider) {
        this.wishListProvider = wishListProvider;
    }

    public void createWishList(User user) {
        UUID userId = user.getId();
        wishLists.computeIfAbsent(userId, id -> {
            log.info(CREATE_WISHLIST, userId);
            return wishListProvider.getObject(user);
        });
    }

    public void deleteWishList(User user) {
        UUID userId = user.getId();
        if (wishLists.remove(userId) != null) {
            log.info(DELETE_WISHLIST_SUCCESS, userId);
        } else {
            log.info(DELETE_WISHLIST_NOT_FOUND, userId);
        }
    }

    public boolean hasWishList(User user) {
        UUID userId = user.getId();
        boolean exists = wishLists.containsKey(userId);
        log.debug(HAS_WISHLIST_CHECK, userId, exists);
        return exists;
    }

    public Set<Item> getItems(User user) {
        log.debug(RETRIEVE_ITEMS, user.getId());
        return getOrCreate(user).getItems();
    }

    public Set<Item> getItemsByCategory(User user, Category category) {
        log.debug(FILTER_ITEMS_BY_CATEGORY, user.getId(), category);
        return getOrCreate(user).filterByCategory(category);
    }

    public boolean addItem(User user, Item item) {
        boolean added = getOrCreate(user).addItem(item);
        log.info(ADD_ITEM_RESULT, item.getTitle(), user.getId(), added ? ADDED : DUPLICATE);
        return added;
    }

    public boolean removeItem(User user, Item item) {
        boolean removed = getOrCreate(user).removeItem(item);
        log.info(REMOVE_ITEM_BY_REF_RESULT, item.getTitle(), user.getId(), removed ? REMOVED : NOT_FOUND);
        return removed;
    }

    public boolean removeItemById(User user, UUID itemId) {
        boolean removed = getOrCreate(user).removeItem(itemId);
        log.info(REMOVE_ITEM_BY_ID_RESULT, itemId, user.getId(), removed ? REMOVED : NOT_FOUND);
        return removed;
    }

    public boolean containsItem(User user, UUID itemId) {
        boolean exists = getOrCreate(user).containsItem(itemId);
        log.debug(CONTAINS_ITEM_CHECK, itemId,  user.getId(), exists);
        return exists;
    }

    public int getWishListSize(User user) {
        int size = getOrCreate(user).size();
        log.debug(WISHLIST_SIZE, user.getId(), size);
        return size;
    }

    public void clearWishList(User user) {
        getOrCreate(user).clear();
        log.info(CLEAR_WISHLIST, user.getId());
    }

    private WishList getOrCreate(User user) {
        UUID userId = user.getId();
        return wishLists.computeIfAbsent(userId, id -> {
            log.debug(AUTO_CREATE_WISHLIST, userId);
            return wishListProvider.getObject(user);
        });
    }
}
