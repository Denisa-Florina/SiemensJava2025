package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private List<Item> processedItems = new ArrayList<>();

    /**
     * Using AtomicInteger ensures thread-safe increments of the processedCount variable
     * This avoids race conditions and improve performance by using non-blocking atomic operations,
     * which are more efficient than synchronized blocks when updating simple counters in concurrent environments.
     */
    private AtomicInteger  processedCount = new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Explanations:
     * In the original implementation, each thread from the pool was accessing
     *  and modifying the variable "processedCount" and the list "processedItems".
     *  This introduced a high risk of race conditions, so synchronization was necessary
     *  to ensure thread-safe access to these shared resources.
     *
     * Another issue was that the threads were started but never joined. As a result,
     *  "return processedItems;" was executed before the threads had completed their execution.
     *
     * Another issue was that the catch in the lambda function swallowed the exception. Now I rethrow it.
     *
     * Another small issue I encountered was about the "processedItems" list. Since the service
     * is instantiated only once during the application's lifecycle, the list continued to grow
     * with each request to "/api/items/process", appending all the items from the DB on every call.
     * Why would I want as a client to receive a list with duplicate of the same items?
     * To fix this, the list is now reinitialized on each method call.
     */

    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        processedItems.clear();

        List<Long> itemIds = itemRepository.findAllIds();

        /**
         * A list to store the CompletableFutures representing each asynchronous task,
         * so then we can wait for all of them to complete before returning "processedItems".
         */
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Long id : itemIds) {
            CompletableFuture <Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);

                    Item item = itemRepository.findById(id).orElse(null);
                    if (item == null) {
                        return;
                    }

                    // Atomic increment
                    processedCount.incrementAndGet();

                    item.setStatus("PROCESSED");
                    itemRepository.save(item);

                    // Synchronized block to ensure thread-safe access when adding items to the list
                    synchronized (processedItems) {
                        processedItems.add(item);
                    }

                } catch (InterruptedException e) {
                    System.out.println("Error when processing the Items (" + e.getMessage() + ")");

                    // The ExceptionHandlerController will catch this error and will respond to the client.
                    throw new RuntimeException("Error when processing the Items (" + e.getMessage() + ")");
                } catch (Exception e) {
                    //Catch any other possible error
                    System.out.println("Error when processing the Items (" + e.getMessage() + ")");

                    // The ExceptionHandlerController will catch this error and will respond to the client.
                    throw new RuntimeException("Error when processing the Items (" + e.getMessage() + ")");
                }
            }, executor);

            // Store the asynchronous task for later computations
            futures.add(future);
        }

        // Using allOf I convert the list of futures in a "single" future. When this future completes I return the list.
        return  CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(done -> processedItems);
    }
}