package com.visitscotland.wishlist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitscotland.wishlist.domain.model.Category;
import com.visitscotland.wishlist.dto.ItemRequest;

/**
 * Integration tests for WishListController.
 * Verifies endpoint behavior, status codes, and basic request/response flow.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@SpringBootTest
@AutoConfigureMockMvc
class WishListControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID itemId;

    @BeforeEach
    void setup() {
        // Arrange: Generate fresh UUIDs for user and item to ensure test isolation
        userId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }

    // Creates a new wish list and expects 201 Created
    @Test
    void createWishlist_shouldReturnCreated() throws Exception {
        // Act: Perform an HTTP POST to /wishlist/{userId} to trigger wish list creation
        mockMvc.perform(post("/wishlist/{userId}", userId))
               // Assert: Expect HTTP 201 Created to confirm successful resource creation
               .andExpect(status().isCreated());
    }

 // Deletes an existing wish list and expects 200 OK
    @Test
    void deleteWishlist_shouldReturnOk() throws Exception {

        // Arrange: Create a wish list for the user via HTTP POST
        // This ensures the resource exists before deletion
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Act: Perform HTTP DELETE to remove the user's wish list
        mockMvc.perform(delete("/wishlist/{userId}", userId))
               // Assert: Expect HTTP 200 OK to confirm successful deletion
               .andExpect(status().isOk());
    }

    @Test
    void getWishList_shouldReturnOkWithItems() throws Exception {
        // Arrange: Use a stable string-based user ID for deterministic UUID generation
        String userIdString = "test-user";
        UUID expectedUserId = UUID.nameUUIDFromBytes(userIdString.getBytes());

        // Arrange: Create a new wish list for the user via HTTP POST
        mockMvc.perform(post("/wishlist/{userId}", userIdString))
               .andExpect(status().isCreated());

        // Arrange: Construct an item request with valid fields
        ItemRequest request = new ItemRequest();
        request.setId(UUID.randomUUID());
        request.setTitle("Sample Item");
        request.setCategory(Category.EVENT);

        // Act: Add the item to the user's wish list via HTTP POST
        mockMvc.perform(post("/wishlist/{userId}/item", userIdString)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated());

        // Act: Retrieve the user's wish list via HTTP GET
        mockMvc.perform(get("/wishlist/{userId}", userIdString))
               // Assert: Expect HTTP 200 OK for successful retrieval
               .andExpect(status().isOk())
               // Assert: Validate that the returned userId matches the expected UUID
               .andExpect(jsonPath("$.userId").value(expectedUserId.toString()))
               // Assert: Validate that the first item has the correct title and category
               .andExpect(jsonPath("$.items[0].title").value("Sample Item"))
               .andExpect(jsonPath("$.items[0].category").value("EVENT"));
    }

    @Test
    void getWishList_shouldFilterByCategory() throws Exception {
        // Arrange: Use a stable string-based user ID for deterministic UUID generation
        String userIdString = "category-user";
        UUID expectedUserId = UUID.nameUUIDFromBytes(userIdString.getBytes());

        // Arrange: Create a new wish list for the user
        mockMvc.perform(post("/wishlist/{userId}", userIdString))
               .andExpect(status().isCreated());

        // Arrange: Construct and add an EVENT item to the wish list
        ItemRequest eventItem = new ItemRequest();
        eventItem.setId(UUID.randomUUID());
        eventItem.setTitle("Event Item");
        eventItem.setCategory(Category.EVENT);

        mockMvc.perform(post("/wishlist/{userId}/item", userIdString)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(eventItem)))
               .andExpect(status().isCreated());

        // Arrange: Construct and add an ATTRACTION item to the wish list
        ItemRequest attractionItem = new ItemRequest();
        attractionItem.setId(UUID.randomUUID());
        attractionItem.setTitle("Attraction Item");
        attractionItem.setCategory(Category.ATTRACTION);

        mockMvc.perform(post("/wishlist/{userId}/item", userIdString)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(attractionItem)))
               .andExpect(status().isCreated());

        // Act: Retrieve the wish list filtered by category = EVENT
        mockMvc.perform(get("/wishlist/{userId}", userIdString)
               .param("category", "EVENT"))
               // Assert: Expect HTTP 200 OK for successful retrieval
               .andExpect(status().isOk())
               // Assert: Validate that the returned userId matches the expected UUID
               .andExpect(jsonPath("$.userId").value(expectedUserId.toString()))
               // Assert: Validate that only one item is returned
               .andExpect(jsonPath("$.items.length()").value(1))
               // Assert: Validate that the returned item matches the EVENT item
               .andExpect(jsonPath("$.items[0].title").value("Event Item"))
               .andExpect(jsonPath("$.items[0].category").value("EVENT"));
    }

    // Attempts to retrieve a non-existent wish list and expects 404
    @Test
    void getWishList_shouldReturnNotFoundIfMissing() throws Exception {
        // Act: Perform HTTP GET on /wishlist/{userId} without prior creation
        // This simulates a lookup for a missing or uninitialized resource
        mockMvc.perform(get("/wishlist/{userId}", userId))
               // Assert: Expect HTTP 404 Not Found to confirm not found
               .andExpect(status().isNotFound());
    }

    // Adds a valid item and expects 201 Created
    @Test
    void addItem_shouldReturnCreated() throws Exception {
        // Arrange: Create a new wish list for the user to ensure the item has a target container
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Arrange: Construct a valid item request with all required fields
        ItemRequest request = new ItemRequest();
        request.setId(itemId);                          // Unique item identifier
        request.setTitle("Test Item");                  // Human-readable label
        request.setCategory(Category.EVENT);            // Domain-specific classification
        request.setDate(LocalDate.now());               // Temporal context for the item
        request.setMetadata(Map.of("source", "test"));  // Arbitrary key-value metadata

        // Act: Submit the item to the user's wish list via HTTP POST
        mockMvc.perform(post("/wishlist/{userId}/item", userId)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
               // Assert: Expect HTTP 201 Created to confirm successful item registration
               .andExpect(status().isCreated());
    }

    // Adds the same item twice and expects 409 Conflict on second attempt
    @Test
    void addItem_shouldReturnConflictIfDuplicate() throws Exception {
        // Arrange: Create a new wish list for the user
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Arrange: Construct a valid item request with a fixed ID and category
        ItemRequest request = new ItemRequest();
        request.setId(itemId);                          // Fixed UUID to simulate duplication
        request.setTitle("Duplicate Item");             // Arbitrary label
        request.setCategory(Category.ATTRACTION);       // Domain classification

        // Arrange: Serialize the request payload once for reuse
        String payload = objectMapper.writeValueAsString(request);

        // Act: Submit the item for the first time — should succeed
        mockMvc.perform(post("/wishlist/{userId}/item", userId)
               .contentType(MediaType.APPLICATION_JSON)
               .content(payload))
               .andExpect(status().isCreated());

        // Act: Submit the same item again — should trigger conflict
        mockMvc.perform(post("/wishlist/{userId}/item", userId)
               .contentType(MediaType.APPLICATION_JSON)
               .content(payload))
               // Assert: Expect HTTP 409 Conflict to confirm duplicate detection
               .andExpect(status().isConflict());
    }

    // Removes an existing item by ID and expects 200 OK
    @Test
    void removeItemById_shouldReturnOkIfFound() throws Exception {
        // Arrange: Create a new wish list for the user
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Arrange: Construct a valid item request with a known ID and category
        ItemRequest request = new ItemRequest();
        request.setId(itemId);                          // Deterministic item ID for traceability
        request.setTitle("Removable Item");             // Human-readable label
        request.setCategory(Category.ACCOMMODATION);    // Domain classification

        // Act: Add the item to the user's wish list
        mockMvc.perform(post("/wishlist/{userId}/item", userId)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated());

        // Act: Remove the item by ID via HTTP DELETE
        mockMvc.perform(delete("/wishlist/{userId}/item/{itemId}", userId, itemId))
               // Assert: Expect HTTP 200 OK to confirm successful removal
               .andExpect(status().isOk());
    }

    // Attempts to remove a non-existent item by ID and expects 404
    @Test
    void removeItemById_shouldReturnNotFoundIfMissing() throws Exception {
        // Arrange: Create a new wish list for the user
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Act: Attempt to delete an item that was never added
        mockMvc.perform(delete("/wishlist/{userId}/item/{itemId}", userId, itemId))
               // Assert: Expect HTTP 404 Not Found to confirm proper error signaling
               .andExpect(status().isNotFound());
    }

    // Removes an existing item using full payload and expects 200 OK
    @Test
    void removeItemByPayload_shouldReturnOkIfFound() throws Exception {
        // Arrange: Create a new wish list for the user
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Arrange: Construct a valid item request with known ID and category
        ItemRequest request = new ItemRequest();
        request.setId(itemId);                          // Deterministic item ID for traceability
        request.setTitle("Payload Item");               // Human-readable label
        request.setCategory(Category.EVENT);            // Domain classification

        // Act: Add the item to the user's wish list via POST
        mockMvc.perform(post("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated());

        // Act: Remove the item using full payload via DELETE
        mockMvc.perform(delete("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               // Assert: Expect HTTP 200 OK to confirm successful removal
               .andExpect(status().isOk());
    }

    // Attempts to remove a non-existent item using payload and expects 404
    @Test
    void removeItemByPayload_shouldReturnNotFoundIfMissing() throws Exception {
        // Arrange: Create a new wish list for the user to ensure the container exists
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Arrange: Construct an item request that was never added to the wish list
        ItemRequest request = new ItemRequest();
        request.setId(itemId);                          // Deterministic ID for traceability
        request.setTitle("Missing Item");               // Arbitrary label
        request.setCategory(Category.EVENT);            // Domain classification

        // Act: Attempt to remove the item using full payload via DELETE
        mockMvc.perform(delete("/wishlist/{userId}/item", userId)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
               // Assert: Expect HTTP 404 Not Found to confirm proper error signaling
               .andExpect(status().isNotFound());
    }
    
    // Adds an item with no ID in the request and expects 201 Created
    @Test
    void addItem_shouldGenerateId_whenRequestHasNoId() throws Exception {
        // Arrange: Create a new wish list for the user
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        // Arrange: Construct an item request with no ID (null)
        ItemRequest request = new ItemRequest();
        request.setId(null);                            // Explicitly omit ID to trigger fallback
        request.setTitle("Auto-ID Item");               // Valid title
        request.setCategory(Category.EVENT);            // Valid category
        request.setDate(LocalDate.now());               // Optional but valid
        request.setMetadata(Map.of("source", "test"));  // Optional metadata

        // Act: Submit the item via POST
        mockMvc.perform(post("/wishlist/{userId}/item", userId)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
               // Assert: Expect HTTP 201 Created to confirm successful fallback and registration
               .andExpect(status().isCreated());
    }
}
