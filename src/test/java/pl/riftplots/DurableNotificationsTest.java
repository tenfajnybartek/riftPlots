package pl.riftplots;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.service.notification.NotificationStore;
import pl.riftplots.service.io.OrderedIoQueue;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class DurableNotificationsTest {
    @TempDir
    Path directory;

    @Test
    void messagesAndDispatchReceiptsSurviveRestart() {
        UUID player = UUID.randomUUID();
        var store = new NotificationStore(directory.toFile(), 30, 20);
        store.initialize();
        String id = store.enqueue(player, "Refund is waiting").id();
        var restarted = new NotificationStore(directory.toFile(), 30, 20);
        restarted.initialize();
        assertEquals(id, restarted.pending(player).getFirst().id());
        restarted.delivered(player, id);
        var again = new NotificationStore(directory.toFile(), 30, 20);
        again.initialize();
        assertTrue(again.pending(player).isEmpty());
        assertNotNull(again.history(player, 0).getFirst().delivered());
    }

    @Test
    void orderedIoPreservesSubmissionOrderAndReportsFailures() {
        List<Integer> completed = Collections.synchronizedList(new ArrayList<>());
        try (var queue = new OrderedIoQueue()) {
            List<CompletableFuture<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int value = i;
                tasks.add(queue.submit(() -> {
                    completed.add(value);
                    return value;
                }));
            }
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
            assertEquals(java.util.stream.IntStream.range(0, 100).boxed().toList(), completed);
            assertThrows(java.util.concurrent.CompletionException.class, () -> queue.submit(() -> {
                throw new IllegalStateException("Write failed");
            }).join());
            assertTrue(queue.lastFailure().contains("Write failed"));
        }
    }
}
