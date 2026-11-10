package pl.riftplots.storage.repository;

import pl.riftplots.domain.payment.TransactionRecord;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository {
    void save(TransactionRecord record);

    List<TransactionRecord> findByPlotId(String plotId);

    List<TransactionRecord> findByPlayer(UUID playerUuid);

    List<TransactionRecord> findAll(int limit);

    default List<TransactionRecord> findByPlotId(String plotId, int page, int size) {
        return findByPlotId(plotId).stream().skip((long) Math.max(0, page) * size).limit(size).toList();
    }

    default List<TransactionRecord> findByPlayer(UUID player, int page, int size) {
        return findByPlayer(player).stream().skip((long) Math.max(0, page) * size).limit(size).toList();
    }

    default List<TransactionRecord> findAll(int page, int size) {
        return findAll(Integer.MAX_VALUE).stream().skip((long) Math.max(0, page) * size).limit(size).toList();
    }
}
