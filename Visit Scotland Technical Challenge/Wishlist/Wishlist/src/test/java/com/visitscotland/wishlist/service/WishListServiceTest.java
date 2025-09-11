package com.visitscotland.wishlist.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import com.visitscotland.wishlist.domain.model.Category;
import com.visitscotland.wishlist.domain.model.Item;
import com.visitscotland.wishlist.domain.model.User;
import com.visitscotland.wishlist.domain.model.WishList;

/**
 * Unit tests for WishListService.
 * Verifies delegation and lifecycle behavior using mocked WishList and Item.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WishListServiceTest
{
    @Mock
    private ObjectProvider<WishList> wishListProvider;

    @Mock
    private WishList mockWishList;

    @Mock
    private Item mockItem;

    private WishListService service;
    private User user;

    @BeforeEach
    void setup() {
        UUID id = UUID.nameUUIDFromBytes("test-user".getBytes());
        user = new User(id, "test-user");

        when(wishListProvider.getObject(user)).thenReturn(mockWishList);
        service = new WishListService(wishListProvider);
    }

    @Test
    void createWishList_shouldStoreNewWishList_whenUserIsNew() {
        service.createWishList(user);
        verify(wishListProvider).getObject(user);
        assertTrue(service.hasWishList(user));
    }

    @Test
    void deleteWishList_shouldRemoveWishList_whenUserHasOne() {
        service.createWishList(user);
        service.deleteWishList(user);
        assertFalse(service.hasWishList(user));
    }

    @Test
    void deleteWishList_shouldDoNothing_whenWishListDoesNotExist() {
        User newUser = new User(UUID.nameUUIDFromBytes("no-wishlist-user".getBytes()), "no-wishlist-user");

        when(wishListProvider.getObject(newUser)).thenReturn(mockWishList);
        when(mockWishList.size()).thenReturn(0);

        assertFalse(service.hasWishList(newUser));
        service.deleteWishList(newUser);
        assertFalse(service.hasWishList(newUser));
        assertEquals(0, service.getWishListSize(newUser));

        /** Verifies that the provider was not called for the original user
         * (the one used in other tests). This ensures isolation and avoids
         * test pollution.
         */
        verify(wishListProvider, never()).getObject(user);
    }

    @Test
    void addItem_shouldReturnTrue_whenItemIsNew() {
        when(mockWishList.addItem(mockItem)).thenReturn(true);
        service.createWishList(user);
        boolean added = service.addItem(user, mockItem);
        assertTrue(added);
        verify(mockWishList).addItem(mockItem);
    }

    @Test
    void addItem_shouldReturnFalse_whenItemAlreadyExists() {
        when(mockWishList.addItem(mockItem)).thenReturn(true).thenReturn(false);
        service.createWishList(user);
        assertTrue(service.addItem(user, mockItem));
        assertFalse(service.addItem(user, mockItem));
        verify(mockWishList, times(2)).addItem(mockItem);
    }

    @Test
    void getItems_shouldReturnStoredItems_whenWishListExists() {
        Set<Item> mockItems = Set.of(mock(Item.class));
        when(mockWishList.getItems()).thenReturn(mockItems);
        service.createWishList(user);
        Set<Item> result = service.getItems(user);
        assertEquals(1, result.size());
        verify(mockWishList).getItems();
    }

    @Test
    void getItemsByCategory_shouldReturnFilteredItems_whenCategoryMatches() {
        Set<Item> filtered = Set.of(mock(Item.class));
        when(mockWishList.filterByCategory(Category.EVENT)).thenReturn(filtered);
        service.createWishList(user);
        Set<Item> result = service.getItemsByCategory(user, Category.EVENT);
        assertEquals(1, result.size());
        verify(mockWishList).filterByCategory(Category.EVENT);
    }

    @Test
    void removeItemById_shouldReturnTrue_whenItemExists() {
        UUID itemId = UUID.randomUUID();
        when(mockWishList.removeItem(itemId)).thenReturn(true);
        service.createWishList(user);
        boolean removed = service.removeItemById(user, itemId);
        assertTrue(removed);
        verify(mockWishList).removeItem(itemId);
    }

    @Test
    void removeItem_shouldReturnTrue_whenItemExists() {
        when(mockWishList.removeItem(mockItem)).thenReturn(true);
        service.createWishList(user);
        boolean removed = service.removeItem(user, mockItem);
        assertTrue(removed);
        verify(mockWishList).removeItem(mockItem);
    }

    @Test
    void containsItem_shouldReturnTrue_whenItemIsPresent() {
        UUID itemId = UUID.randomUUID();
        when(mockWishList.containsItem(itemId)).thenReturn(true);
        service.createWishList(user);
        assertTrue(service.containsItem(user, itemId));
        verify(mockWishList).containsItem(itemId);
    }

    @Test
    void getWishListSize_shouldReturnItemCount_whenItemsExist() {
        when(mockWishList.size()).thenReturn(2);
        service.createWishList(user);
        assertEquals(2, service.getWishListSize(user));
        verify(mockWishList).size();
    }

    @Test
    void clearWishList_shouldRemoveAllItems_whenWishListIsNotEmpty() {
        service.createWishList(user);
        service.clearWishList(user);
        verify(mockWishList).clear();
    }

    @Test
    void getItems_shouldCreateWishList_whenNoneExistsForUser() {
        User newUser = new User(UUID.nameUUIDFromBytes("lazy-user".getBytes()), "lazy-user");
        when(wishListProvider.getObject(newUser)).thenReturn(mockWishList);
        when(mockWishList.getItems()).thenReturn(Set.of());
        Set<Item> items = service.getItems(newUser);
        assertNotNull(items);
        assertTrue(items.isEmpty());
        verify(wishListProvider).getObject(newUser);
        verify(mockWishList).getItems();
    }
}
