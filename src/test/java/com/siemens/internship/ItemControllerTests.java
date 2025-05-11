package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
public class ItemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProcessItems_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {

        when(itemService.processItemsAsync()).thenThrow(new RuntimeException("Test exception"));

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Test exception"));
    }

    @Test
    void testProcessItems_shouldReturnOk_whenNoError() throws Exception {
        Item processedItem = new Item(1L, "nume", "Descriere", "PROCESSED", "denisa@gmail.com");
        when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(List.of(processedItem)));

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }

    @Test
    void testGetAllItems_shouldReturnOk() throws Exception {
        Item item1 = new Item(1L, "nume1", "Descriere", "status", "denisa1@gmail.com");
        Item item2 = new Item(2L, "nume2", "Descriere", "status", "denisa2@gmail.com");

        when(itemService.findAll()).thenReturn(List.of(item1, item2));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].name").value("nume2"));
    }
    @Test
    void testGetItemById_shouldReturnOk_whenFound() throws Exception {
        Item item = new Item(1L, "nume1", "Descriere", "status", "denisa@gmail.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("nume1"));
    }

    @Test
    void testGetItemById_shouldReturnNotFound_whenNotFound() throws Exception {
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateItem_shouldReturnOk_whenValid() throws Exception {
        Item item = new Item(null, "nume1", "Descriere", "status", "denisa@gmail.com");
        Item savedItem = new Item(1L, "nume1", "Descriere", "status", "denisa@gmail.com");

        when(itemService.save(any())).thenReturn(savedItem);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("nume1"));
    }

    @Test
    void testCreateItem_shouldBadRequest_whenInvalidName() throws Exception {
        Item invalidItem = new Item(null, "", "Descriere", "status", "denisa@gmail.com");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateItem_shouldBadRequest_whenInvalidEmail() throws Exception {
        Item invalidItem = new Item(null, "nume", "Descriere", "status", "denisagmail.com");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateItem_shouldReturnOk_whenFound() throws Exception {
        Item item = new Item(1L, "nume", "Descriere", "status", "denisa@gmail.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(item));
        when(itemService.save(any())).thenReturn(item);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("nume"));
    }

    @Test
    void testUpdateItem_shouldReturnNotFound_whenNotFound() throws Exception {
        Item item = new Item(1L, "nume", "Descriere", "status", "denisa@gmail.com");
        when(itemService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteItem_shouldReturnNoContent_whenNoError() throws Exception {
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }
}