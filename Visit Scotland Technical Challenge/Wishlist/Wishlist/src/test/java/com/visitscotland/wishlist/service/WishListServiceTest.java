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
        // Arrange: Create a deterministic UUID for reproducibility across test runs
        UUID id = UUID.nameUUIDFromBytes("test-user".getBytes());
        user = new User(id, "test-user");

        // Arrange: Mock provider behavior to return a predefined mocked wish list for the test user
        when(wishListProvider.getObject(user)).thenReturn(mockWishList);

        // Arrange: Instantiate service under test with the mocked wishlist dependency
        service = new WishListService(wishListProvider);
    }

    @Test
    void createWishList_shouldStoreNewWishList_whenUserIsNew() {
        // Act: Invoke method under test to create a wish list for a new user
        service.createWishList(user);

        // Assert: Verify that the provider was queried for the user's wish list
        verify(wishListProvider).getObject(user);

        // Assert: Confirm that the service now recognizes the user as having a wish list
        assertTrue(service.hasWishList(user));
    }

    @Test
    void deleteWishList_shouldRemoveWishList_whenUserHasOne() {
        // Arrange: Ensure the user has a wish list before deletion
        service.createWishList(user);

        // Act: Invoke deletion logic for the user's wish list
        service.deleteWishList(user);

        // Assert: Confirm that the wish list is no longer present for the user
        assertFalse(service.hasWishList(user));
    }

    @Test
    void deleteWishList_shouldDoNothing_whenWishListDoesNotExist() {
        // Arrange: Create a deterministic user with no wish list
        User newUser = new User(UUID.nameUUIDFromBytes("no-wishlist-user".getBytes()), "no-wishlist-user");

        // Arrange: Mock provider to return an empty wish list for this user
        when(wishListProvider.getObject(newUser)).thenReturn(mockWishList);
        when(mockWishList.size()).thenReturn(0);

        // Assert (precondition): Confirm that the service does not recognize a wish list for this user
        assertFalse(service.hasWishList(newUser));

        // Act: Attempt to delete a non-existent wish list
        service.deleteWishList(newUser);

        // Assert: Confirm that deletion had no effect—wish list still absent
        assertFalse(service.hasWishList(newUser));

        // Assert: Confirm that the wish list size remains zero
        assertEquals(0, service.getWishListSize(newUser));

        /** 
         * Assert: Ensure isolation by verifying that the provider was never queried 
         * for the original test user. This guards against test pollution and confirms 
         * that the test scope is strictly limited to `newUser`.
         */
        verify(wishListProvider, never()).getObject(user);
    }

    @Test
    void addItem_shouldReturnTrue_whenItemIsNew() {
        // Arrange: Mock wish list behavior to simulate successful addition of a new item
        when(mockWishList.addItem(mockItem)).thenReturn(true);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act: Attempt to add a new item to the user's wish list
        boolean added = service.addItem(user, mockItem);

        // Assert: Confirm that the item was reported as successfully added
        assertTrue(added);

        // Assert: Verify that the wish list's addItem method was invoked with the correct item
        verify(mockWishList).addItem(mockItem);
    }

    @Test
    void addItem_shouldReturnFalse_whenItemAlreadyExists() {
        // Arrange: Mock wish list behavior to simulate first-time success, then rejection on duplicate
        when(mockWishList.addItem(mockItem)).thenReturn(true).thenReturn(false);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act & Assert: First addition should succeed
        assertTrue(service.addItem(user, mockItem));

        // Act & Assert: Second addition of same item should fail (already exists)
        assertFalse(service.addItem(user, mockItem));

        // Assert: Verify that addItem was called exactly twice with the same item
        verify(mockWishList, times(2)).addItem(mockItem);
    }

    @Test
    void getItems_shouldReturnStoredItems_whenWishListExists() {
        // Arrange: Create a mock item set with one item to simulate stored wish list contents
        Set<Item> mockItems = Set.of(mock(Item.class));

        // Arrange: Stub wish list to return the mock item set when queried
        when(mockWishList.getItems()).thenReturn(mockItems);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act: Retrieve items from the user's wish list
        Set<Item> result = service.getItems(user);

        // Assert: Confirm that the returned set contains exactly one item
        assertEquals(1, result.size());

        // Assert: Verify that the wish list's getItems method was invoked
        verify(mockWishList).getItems();
    }

    @Test
    void getItemsByCategory_shouldReturnFilteredItems_whenCategoryMatches() {
        // Arrange: Create a mock item set to simulate filtered results for the given category
        Set<Item> filtered = Set.of(mock(Item.class));

        // Arrange: Stub wish list to return the filtered set when queried by category
        when(mockWishList.filterByCategory(Category.EVENT)).thenReturn(filtered);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act: Retrieve items from the user's wish list filtered by category
        Set<Item> result = service.getItemsByCategory(user, Category.EVENT);

        // Assert: Confirm that the returned set contains exactly one item
        assertEquals(1, result.size());

        // Assert: Verify that the wish list's filterByCategory method was invoked with the correct category
        verify(mockWishList).filterByCategory(Category.EVENT);
    }

    @Test
    void removeItemById_shouldReturnTrue_whenItemExists() {
        // Arrange: Generate a deterministic item ID to simulate an existing item
        UUID itemId = UUID.randomUUID();

        // Arrange: Stub wish list behavior to simulate successful removal of the item
        when(mockWishList.removeItem(itemId)).thenReturn(true);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act: Attempt to remove the item by ID from the user's wish list
        boolean removed = service.removeItemById(user, itemId);

        // Assert: Confirm that the item was reported as successfully removed
        assertTrue(removed);

        // Assert: Verify that the wish list's removeItem method was invoked with the correct ID
        verify(mockWishList).removeItem(itemId);
    }

    @Test
    void removeItem_shouldReturnTrue_whenItemExists() {
        // Arrange: Stub wish list behavior to simulate successful removal of the given item
        when(mockWishList.removeItem(mockItem)).thenReturn(true);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act: Attempt to remove the item from the user's wish list
        boolean removed = service.removeItem(user, mockItem);

        // Assert: Confirm that the item was reported as successfully removed
        assertTrue(removed);

        // Assert: Verify that the wish list's removeItem method was invoked with the correct item
        verify(mockWishList).removeItem(mockItem);
    }

    @Test
    void containsItem_shouldReturnTrue_whenItemIsPresent() {
        // Arrange: Generate a deterministic item ID to simulate a known item
        UUID itemId = UUID.randomUUID();

        // Arrange: Stub wish list behavior to simulate presence of the item
        when(mockWishList.containsItem(itemId)).thenReturn(true);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act & Assert: Confirm that the service reports the item as present
        assertTrue(service.containsItem(user, itemId));

        // Assert: Verify that the wish list's containsItem method was invoked with the correct ID
        verify(mockWishList).containsItem(itemId);
    }

    @Test
    void getWishListSize_shouldReturnItemCount_whenItemsExist() {
        // Arrange: Stub wish list behavior to simulate a list containing two items
        when(mockWishList.size()).thenReturn(2);

        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act: Query the service for the size of the user's wish list
        int size = service.getWishListSize(user);

        // Assert: Confirm that the reported size matches the mocked value
        assertEquals(2, size);

        // Assert: Verify that the wish list's size method was invoked
        verify(mockWishList).size();
    }

    @Test
    void clearWishList_shouldRemoveAllItems_whenWishListIsNotEmpty() {
        // Arrange: Ensure the user has an initialized wish list
        service.createWishList(user);

        // Act: Clear all items from the user's wish list
        service.clearWishList(user);

        // Assert: Verify that the wish list's clear method was invoked
        verify(mockWishList).clear();
    }

    @Test
    void getItems_shouldCreateWishList_whenNoneExistsForUser() {
        // Arrange: Create a deterministic user who has no pre-existing wish list
        User newUser = new User(UUID.nameUUIDFromBytes("lazy-user".getBytes()), "lazy-user");

        // Arrange: Stub provider to return a mock wish list when queried for this user
        when(wishListProvider.getObject(newUser)).thenReturn(mockWishList);

        // Arrange: Stub wish list to return an empty item set
        when(mockWishList.getItems()).thenReturn(Set.of());

        // Act: Retrieve items for the user, triggering implicit wish list creation
        Set<Item> items = service.getItems(newUser);

        // Assert: Confirm that the returned item set is non-null and empty
        assertNotNull(items);
        assertTrue(items.isEmpty());

        // Assert: Verify that the provider was queried for the new user
        verify(wishListProvider).getObject(newUser);

        // Assert: Verify that the wish list's getItems method was invoked
        verify(mockWishList).getItems();
    }
}
