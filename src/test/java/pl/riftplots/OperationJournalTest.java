package pl.riftplots;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.service.transaction.OperationJournal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OperationJournalTest {
    @TempDir
    Path directory;

    private OperationJournal journal() {
        var journal = new OperationJournal(directory.toFile());
        journal.initialize();
        return journal;
    }

    @Test
    void completedOperationCannotChargeTwice() {
        var journal = journal();
        var calls = new AtomicInteger();
        UUID player = UUID.randomUUID();
        assertTrue(journal.execute("PURCHASE", "buy:1", "plot", player, BigDecimal.TEN,
                () -> journal.effect("WITHDRAW", player, BigDecimal.TEN, () -> {
                    calls.incrementAndGet();
                    return true;
                })));
        var restarted = journal();
        assertFalse(restarted.execute("PURCHASE", "buy:1", "plot", player, BigDecimal.TEN,
                () -> {
                    calls.incrementAndGet();
                    return true;
                }));
        assertEquals(1, calls.get());
        assertEquals(OperationJournal.Status.COMPLETED, restarted.latest("buy:1").orElseThrow().status());
    }

    @Test
    void unknownWalletResultSurvivesRestartAndIsNeverRetried() {
        var journal = journal();
        UUID player = UUID.randomUUID();
        var calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> journal.execute("TRANSFER", "sale:1", "plot", player, BigDecimal.TEN,
                () -> journal.effect("WITHDRAW", player, BigDecimal.TEN, () -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("Connection interrupted after debit");
                })));
        var restarted = journal();
        var entry = restarted.pending().getFirst();
        assertEquals("IN_FLIGHT", entry.steps().getFirst().state());
        assertTrue(restarted.isBlocked("plot"));
        assertFalse(restarted.execute("TRANSFER", "sale:2", "plot", player, BigDecimal.TEN,
                () -> {
                    calls.incrementAndGet();
                    return true;
                }));
        assertEquals(1, calls.get());
    }

    @Test
    void failedDomainCommitKeepsAppliedPaymentEvidence() {
        var journal = journal();
        UUID player = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> journal.execute("PURCHASE", "buy:2", "plot", player, BigDecimal.TEN, () -> {
            journal.effect("WITHDRAW", player, BigDecimal.TEN, () -> true);
            throw new IllegalStateException("Database write failed");
        }));
        var restarted = journal();
        var entry = restarted.latest("buy:2").orElseThrow();
        assertEquals(OperationJournal.Status.REVIEW_REQUIRED, entry.status());
        assertEquals("APPLIED", entry.steps().getFirst().state());
        restarted.resolve(entry.id(), OperationJournal.Status.COMPLETED, "Admin", "Ownership repaired after checking wallet");
        assertFalse(restarted.isBlocked("plot"));
        assertFalse(restarted.execute("PURCHASE", "buy:2", "plot", player, BigDecimal.TEN, () -> fail("Cannot replay reconciled debit")));
    }

    @Test
    void declinedPaymentMayBeRetriedButOldAttemptIsKept() {
        var journal = journal();
        UUID player = UUID.randomUUID();
        assertFalse(journal.execute("TAX", "tax:1", "plot", player, BigDecimal.TEN,
                () -> journal.effect("WITHDRAW", player, BigDecimal.TEN, () -> false)));
        assertTrue(journal.execute("TAX", "tax:1", "plot", player, BigDecimal.TEN,
                () -> journal.effect("WITHDRAW", player, BigDecimal.TEN, () -> true)));
        assertEquals(2, journal.history(player, "plot", 0, 20).size());
    }

    @Test
    void journalFailureBeforePaymentCallsNoWallet() throws Exception {
        Files.createDirectories(directory.resolve("data"));
        Files.writeString(directory.resolve("data/operations"), "not a directory");
        var journal = new OperationJournal(directory.toFile());
        var calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> journal.execute("TAX", "tax:2", "plot", UUID.randomUUID(), BigDecimal.TEN,
                () -> {
                    calls.incrementAndGet();
                    return true;
                }));
        assertEquals(0, calls.get());
    }

    @Test
    void deferredResetIsUncertainAfterRestartUntilReconciled() {
        var journal = journal();
        assertTrue(journal.execute("ABANDON", "abandon:1", "plot", UUID.randomUUID(), BigDecimal.TEN, () -> {
            journal.deferCurrent();
            return true;
        }));
        assertEquals(OperationJournal.Status.RUNNING, journal.latest("abandon:1").orElseThrow().status());
        assertEquals(OperationJournal.Status.REVIEW_REQUIRED, journal().latest("abandon:1").orElseThrow().status());
    }
}
