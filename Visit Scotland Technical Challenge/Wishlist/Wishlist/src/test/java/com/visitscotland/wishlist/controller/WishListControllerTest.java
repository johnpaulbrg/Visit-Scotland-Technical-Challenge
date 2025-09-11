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
        userId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }

    // Creates a new wish list and expects 201 Created
    @Test
    void createWishlist_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());
    }

    // Deletes an existing wish list and expects 200 OK
    @Test
    void deleteWishlist_shouldReturnOk() throws Exception {
        
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        mockMvc.perform(delete("/wishlist/{userId}", userId))
               .andExpect(status().isOk());
    }

    @Test
    void getWishList_shouldReturnOkWithItems() throws Exception {
        // Use a stable string user ID
        String userIdString = "test-user";
        UUID expectedUserId = UUID.nameUUIDFromBytes(userIdString.getBytes());

        // Create the wish list
        mockMvc.perform(post("/wishlist/{userId}", userIdString))
               .andExpect(status().isCreated());

        // Add an item
        ItemRequest request = new ItemRequest();
        request.setId(UUID.randomUUID());
        request.setTitle("Sample Item");
        request.setCategory(Category.EVENT);

        mockMvc.perform(post("/wishlist/{userId}/item", userIdString)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated());

        // Retrieve the wish list
        mockMvc.perform(get("/wishlist/{userId}", userIdString))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(expectedUserId.toString()))
               .andExpect(jsonPath("$.items[0].title").value("Sample Item"))
               .andExpect(jsonPath("$.items[0].category").value("EVENT"));
    }
    
    @Test
    void getWishList_shouldFilterByCategory() throws Exception {
        // Use a stable string user ID
        String userIdString = "category-user";
        UUID expectedUserId = UUID.nameUUIDFromBytes(userIdString.getBytes());

        // Create the wish list
        mockMvc.perform(post("/wishlist/{userId}", userIdString))
               .andExpect(status().isCreated());

        // Add an EVENT item
        ItemRequest eventItem = new ItemRequest();
        eventItem.setId(UUID.randomUUID());
        eventItem.setTitle("Event Item");
        eventItem.setCategory(Category.EVENT);

        mockMvc.perform(post("/wishlist/{userId}/item", userIdString)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventItem)))
               .andExpect(status().isCreated());

        // Add an ATTRACTION item
        ItemRequest attractionItem = new ItemRequest();
        attractionItem.setId(UUID.randomUUID());
        attractionItem.setTitle("Attraction Item");
        attractionItem.setCategory(Category.ATTRACTION);

        mockMvc.perform(post("/wishlist/{userId}/item", userIdString)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attractionItem)))
               .andExpect(status().isCreated());

        // Retrieve only EVENT items
        mockMvc.perform(get("/wishlist/{userId}", userIdString)
                .param("category", "EVENT"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(expectedUserId.toString()))
               .andExpect(jsonPath("$.items.length()").value(1))
               .andExpect(jsonPath("$.items[0].title").value("Event Item"))
               .andExpect(jsonPath("$.items[0].category").value("EVENT"));
    }
    
    // Attempts to retrieve a non-existent wish list and expects 404
    @Test
    void getWishList_shouldReturnNotFoundIfMissing() throws Exception {
        mockMvc.perform(get("/wishlist/{userId}", userId))
               .andExpect(status().isNotFound());
    }

    // Adds a valid item and expects 201 Created
    @Test
    void addItem_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        ItemRequest request = new ItemRequest();
        request.setId(itemId);
        request.setTitle("Test Item");
        request.setCategory(Category.EVENT);
        request.setDate(LocalDate.now());
        request.setMetadata(Map.of("source", "test"));

        mockMvc.perform(post("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated());
    }

    // Adds the same item twice and expects 409 Conflict on second attempt
    @Test
    void addItem_shouldReturnConflictIfDuplicate() throws Exception {
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        ItemRequest request = new ItemRequest();
        request.setId(itemId);
        request.setTitle("Duplicate Item");
        request.setCategory(Category.ATTRACTION);

        String payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
               .andExpect(status().isCreated());

        mockMvc.perform(post("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
               .andExpect(status().isConflict());
    }

    // Removes an existing item by ID and expects 200 OK
    @Test
    void removeItemById_shouldReturnOkIfFound() throws Exception {
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        ItemRequest request = new ItemRequest();
        request.setId(itemId);
        request.setTitle("Removable Item");
        request.setCategory(Category.ACCOMMODATION);

        mockMvc.perform(post("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated());

        mockMvc.perform(delete("/wishlist/{userId}/item/{itemId}", userId, itemId))
               .andExpect(status().isOk());
    }

    // Attempts to remove a non-existent item by ID and expects 404
    @Test
    void removeItemById_shouldReturnNotFoundIfMissing() throws Exception {
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        mockMvc.perform(delete("/wishlist/{userId}/item/{itemId}", userId, itemId))
               .andExpect(status().isNotFound());
    }

    // Removes an existing item using full payload and expects 200 OK
    @Test
    void removeItemByPayload_shouldReturnOkIfFound() throws Exception {
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        ItemRequest request = new ItemRequest();
        request.setId(itemId);
        request.setTitle("Payload Item");
        request.setCategory(Category.EVENT);

        mockMvc.perform(post("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated());

        mockMvc.perform(delete("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk());
    }

    // Attempts to remove a non-existent item using payload and expects 404
    @Test
    void removeItemByPayload_shouldReturnNotFoundIfMissing() throws Exception {
        mockMvc.perform(post("/wishlist/{userId}", userId))
               .andExpect(status().isCreated());

        ItemRequest request = new ItemRequest();
        request.setId(itemId);
        request.setTitle("Missing Item");
        request.setCategory(Category.EVENT);

        mockMvc.perform(delete("/wishlist/{userId}/item", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isNotFound());
    }
}
