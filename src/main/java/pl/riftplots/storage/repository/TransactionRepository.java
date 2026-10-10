package pl.riftplots.storage.repository;

import pl.riftplots.domain.payment.TransactionRecord;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository {
    void save(TransactionRecord record);
    List<TransactionRecord> findByPlotId(String plotId);
    List<TransactionRecord> findByPlayer(UUID playerUuid);
    List<TransactionRecord> findAll(int limit);
}
