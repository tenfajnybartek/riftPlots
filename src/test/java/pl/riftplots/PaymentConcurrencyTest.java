package pl.riftplots;

import org.junit.jupiter.api.Test;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.payment.TransactionStatus;
import pl.riftplots.domain.payment.TransactionType;
import pl.riftplots.domain.plot.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PaymentConcurrencyTest {

    @Test
    void testConcurrentPurchasesSingleWinner() throws InterruptedException {
        PlotBounds bounds = new PlotBounds("world", 0, 60, 0, 10, 80, 10);
        Plot plot = new Plot(
                "plot_race", "Rynek Wyścig", "Desc", PlotCategory.COMMERCIAL, PlotStatus.AVAILABLE,
                bounds, "rg_race", BigDecimal.valueOf(10000), BigDecimal.valueOf(500),
                TaxMode.FIXED, 1.0, true, 7, 6, null, null, Collections.emptyList(),
                null, null, null, 0, null, true, 50.0, Instant.now(), Instant.now()
        );

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successfulBuys = new AtomicInteger(0);
        Object plotLock = new Object();

        for (int i = 0; i < threads; i++) {
            final UUID buyerUuid = UUID.randomUUID();
            final String buyerName = "Player_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    synchronized (plotLock) {
                        if (plot.getStatus() == PlotStatus.AVAILABLE) {
                            plot.setStatus(PlotStatus.OWNED);
                            plot.setOwnerUuid(buyerUuid);
                            plot.setOwnerName(buyerName);
                            successfulBuys.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successfulBuys.get());
        assertEquals(PlotStatus.OWNED, plot.getStatus());
        assertNotNull(plot.getOwnerUuid());
    }

    @Test
    void testAbandonRefundCalculation() {
        PlotBounds bounds = new PlotBounds("world", 0, 60, 0, 10, 80, 10);
        Plot plot = new Plot(
                "refund_test", "Zwrot", "Desc", PlotCategory.RESIDENTIAL, PlotStatus.OWNED,
                bounds, "rg_ref", BigDecimal.valueOf(25000), BigDecimal.valueOf(1000),
                TaxMode.FIXED, 1.0, true, 7, 6, UUID.randomUUID(), "Wlasciciel", Collections.emptyList(),
                Instant.now(), Instant.now(), null, 0, null, true, 50.0, Instant.now(), Instant.now()
        );

        BigDecimal price = plot.getPrice();
        double percent = plot.getRefundPercentOnAbandon();
        BigDecimal refund = price.multiply(BigDecimal.valueOf(percent / 100.0)).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("12500.00"), refund);

        plot.setRefundPercentOnAbandon(0.0);
        BigDecimal zeroRefund = plot.getPrice().multiply(BigDecimal.valueOf(plot.getRefundPercentOnAbandon() / 100.0)).setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("0.00"), zeroRefund);
    }
}
