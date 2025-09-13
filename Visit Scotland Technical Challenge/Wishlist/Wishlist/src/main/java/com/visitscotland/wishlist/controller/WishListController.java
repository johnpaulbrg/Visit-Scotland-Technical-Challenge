package com.visitscotland.wishlist.controller;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.visitscotland.wishlist.domain.model.Category;
import com.visitscotland.wishlist.domain.model.Item;
import com.visitscotland.wishlist.domain.model.User;
import com.visitscotland.wishlist.dto.ItemRequest;
import com.visitscotland.wishlist.dto.ItemResponse;
import com.visitscotland.wishlist.dto.WishListResponse;
import com.visitscotland.wishlist.service.WishListService;

/**
 * REST controller for managing wish lists.
 * Uses domain-safe User objects derived from path variables.
 *
 * @author John Paul Brogan
 * @date 2025-09-10
 */
@RestController
@RequestMapping("/wishlist")
public final class WishListController
{
    private static final Logger log = LoggerFactory.getLogger(WishListController.class);
    private static final String MSG_WISHLIST_NOT_FOUND = "Wish list not found";
    private static final String MSG_ITEM_ALREADY_EXISTS = "Item already exists";
    private static final String MSG_ITEM_NOT_FOUND_PREFIX = "Item not found: ";

    @Autowired
    private WishListService wishListService;

    @Autowired
    private HttpServletRequest request;

    private User resolveUser(String userId) {
        UUID id = UUID.nameUUIDFromBytes(userId.getBytes());
        return new User(id, userId);
    }

    private void logRequest(String methodName, User user) {
        log.info("Request [{}] from {} to {} for user {}", methodName,
                 request.getRemoteAddr(), request.getRequestURI(), user.getId());
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Void> create(@PathVariable String userId) {
        User user = resolveUser(userId);
        logRequest("create", user);

        if (wishListService.hasWishList(user)) {
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        wishListService.createWishList(user);
        URI location = URI.create("/wishlist/" + userId);
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        User user = resolveUser(userId);
        logRequest("delete", user);

        if (!wishListService.hasWishList(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WISHLIST_NOT_FOUND);
        }

        wishListService.clearWishList(user);
        wishListService.deleteWishList(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WishListResponse> get(
            @PathVariable String userId,
            @RequestParam(required = false) Category category) {
        User user = resolveUser(userId);
        logRequest("get", user);

        if (!wishListService.hasWishList(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WISHLIST_NOT_FOUND);
        }

        Set<Item> items = (category == null)
                ? wishListService.getItems(user)
                : wishListService.getItemsByCategory(user, category);

        List<ItemResponse> response = items.stream()
                                           .map(ItemResponse::from)
                                           .collect(Collectors.toList());

        return ResponseEntity.ok(new WishListResponse(user.getId(), response));
    }

    @PostMapping("/{userId}/item")
    public ResponseEntity<Void> addItem(
            @PathVariable String userId,
            @Valid @RequestBody ItemRequest requestBody) {
        User user = resolveUser(userId);
        logRequest("addItem", user);

        if (!wishListService.hasWishList(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WISHLIST_NOT_FOUND);
        }

        Item item = new Item(
            requestBody.getId() != null ? requestBody.getId() : UUID.randomUUID(),
            requestBody.getTitle(),
            requestBody.getCategory(),
            requestBody.getDescription(),
            requestBody.getImage(),
            requestBody.getDate(),
            requestBody.getMetadata()
        );

        boolean added = wishListService.addItem(user, item);
        if (!added) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_ITEM_ALREADY_EXISTS);
        }

        URI location = URI.create("/wishlist/" + userId + "/item/" + item.getId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{userId}/item/{itemId}")
    public ResponseEntity<Void> removeItemById(
            @PathVariable String userId,
            @PathVariable UUID itemId) {
        User user = resolveUser(userId);
        logRequest("removeItemById", user);

        if (!wishListService.hasWishList(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WISHLIST_NOT_FOUND);
        }

        boolean removed = wishListService.removeItemById(user, itemId);
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_ITEM_NOT_FOUND_PREFIX + itemId);
        }

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/item")
    public ResponseEntity<Void> removeItemByPayload(
            @PathVariable String userId,
            @Valid @RequestBody ItemRequest requestBody) {
        User user = resolveUser(userId);
        logRequest("removeItemByPayload", user);

        if (!wishListService.hasWishList(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WISHLIST_NOT_FOUND);
        }

        Item item = new Item(
            requestBody.getId(),
            requestBody.getTitle(),
            requestBody.getCategory(),
            requestBody.getDescription(),
            requestBody.getImage(),
            requestBody.getDate(),
            requestBody.getMetadata()
        );

        boolean removed = wishListService.removeItem(user, item);
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_ITEM_NOT_FOUND_PREFIX + item.getId());
        }

        return ResponseEntity.ok().build();
    }
}
