package com.siemens.internship;

import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.siemens.internship.model.Item;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTests {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void testProcessItemsAsync_shouldRunCorrectly() throws Exception {

        //Mock the methods of the repository
        List <Long> itemIds = List.of(1L, 2L);
        Item item1 = new Item(1L, "nume1", "descriere1", "status", "denisa1@gmail.com");
        Item item2 = new Item(2L, "nume2", "descrirere2", "status", "denisa2@gmail.com");

        when(itemRepository.findAllIds()).thenReturn(itemIds);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(items -> items.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();

        List<Item> processed = future.join();

        assertEquals(2, processed.size());
        assertTrue(processed.stream().allMatch(i -> "PROCESSED".equals(i.getStatus())));
        verify(itemRepository, times(2)).save(any(Item.class));
    }


    /**
     *  All the bellow tests are mainly just for the coverage :))
     */
    @Test
    void testFindAll() {
        List<Item> mockItems = List.of(
                new Item(1L, "nume1", "descriere", "status", "denisa1@gmail.com"),
                new Item(2L, "nume2", "descriere", "status", "denisa1@gmail.com")
        );

        when(itemRepository.findAll()).thenReturn(mockItems);

        List<Item> result = itemService.findAll();
        assertEquals(2, result.size());
        assertEquals("nume1", result.get(0).getName());
    }

    @Test
    void testFindById_shouldExist() {
        Item item = new Item(1L, "nume", "desc", "status", "denisa@gmail.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("nume", result.get().getName());
    }

    @Test
    void testFindById_shouldNotExist() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(1L);
        assertFalse(result.isPresent());
    }

    @Test
    void testSave() {
        Item item = new Item(null, "nume", "desc", "status", "denisa@gmail.com");
        Item saved = new Item(1L, "nume", "desc", "status", "denisa@gmail.com");

        when(itemRepository.save(item)).thenReturn(saved);

        Item result = itemService.save(item);
        assertNotNull(result.getId());
        assertEquals("nume", result.getName());
    }

    @Test
    void testDeleteById_shouldDelete() {
        itemService.deleteById(1L);
        verify(itemRepository, times(1)).deleteById(1L);
    }

}