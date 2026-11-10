package pl.riftplots.service.transaction;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.storage.yaml.YamlDataFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Durable debts are independent of whether the former tenant may still enter a plot.
 */
public final class RefundService {
    public enum Status {PENDING, PAID, REVIEW_REQUIRED, CANCELLED}

    public record Refund(String id, String key, String kind, String plotId, String releaseId,
                         UUID player, BigDecimal amount, String reason, Status status,
                         Instant created, Instant nextAttempt, int attempts) {
    }

    private final File directory;
    private final OperationJournal journal;
    private final VaultHook vault;
    private final Map<String, Refund> refunds = new LinkedHashMap<>();
    private Predicate<Refund> eligible = refund -> true;
    private java.util.function.Consumer<Refund> receipt = refund -> {
    };

    public RefundService(File dataFolder, OperationJournal journal, VaultHook vault) {
        this.directory = new File(dataFolder, "data/refunds");
        this.journal = journal;
        this.vault = vault;
    }

    public synchronized void initialize() {
        refunds.clear();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            if (directory.exists() && !directory.isDirectory())
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.refund-service.invalid-refund-directory"));
            return;
        }
        for (File file : files) {
            YamlConfiguration yaml = YamlDataFile.load(file);
            if (yaml.getInt("format", 1) != 1)
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.refund-service.unsupported-refund-format", file));
            Refund refund = new Refund(yaml.getString("id"), yaml.getString("key"), yaml.getString("kind"),
                    yaml.getString("plot"), yaml.getString("release"), UUID.fromString(yaml.getString("player")),
                    new BigDecimal(yaml.getString("amount")), yaml.getString("reason"), Status.valueOf(yaml.getString("status")),
                    Instant.ofEpochMilli(yaml.getLong("created")), Instant.ofEpochMilli(yaml.getLong("next-attempt")), yaml.getInt("attempts"));
            if (!file.getName().equals(refund.id() + ".yml"))
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.refund-service.invalid-refund-id"));
            refunds.put(refund.id(), refund);
        }
    }

    public void setEligibility(Predicate<Refund> eligible) {
        this.eligible = Objects.requireNonNull(eligible);
    }

    public void setReceipt(java.util.function.Consumer<Refund> receipt) {
        this.receipt = Objects.requireNonNull(receipt);
    }

    public synchronized List<Refund> list(UUID player) {
        return refunds.values().stream().filter(r -> player == null || player.equals(r.player()))
                .sorted(Comparator.comparing(Refund::created).reversed()).toList();
    }

    public synchronized void prepare(String key, String kind, String plotId, String releaseId,
                                     UUID player, BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) return;
        if (player == null || !pl.riftplots.util.Money.isSafe(amount))
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.refund-service.invalid-refund"));
        String id = OperationJournal.idFor(key);
        if (refunds.containsKey(id)) return;
        save(new Refund(id, key, kind, plotId, releaseId, player, amount, reason, Status.PENDING,
                Instant.now(), Instant.EPOCH, 0));
    }

    public synchronized boolean retry(String id) {
        Refund refund = refunds.get(id);
        if (refund == null || refund.status() == Status.PAID || refund.status() == Status.CANCELLED)
            return refund != null;
        if (!journal.isWriteAllowed() || journal.isRunning() || !eligible.test(refund)) return false;
        String operationKey = "refund:" + refund.id();
        var previous = journal.latest(operationKey);
        if (previous.isPresent() && previous.get().status() == OperationJournal.Status.COMPLETED) {
            save(withStatus(refund, Status.PAID));
            return true;
        }
        if (previous.isPresent() && previous.get().status() == OperationJournal.Status.REVIEW_REQUIRED) {
            save(withStatus(refund, Status.REVIEW_REQUIRED));
            return false;
        }
        // A manual cancellation is an explicit reconciliation, not permission to pay an uncertain call again.
        if (refund.status() == Status.REVIEW_REQUIRED) return false;
        boolean paid = journal.execute("REFUND", operationKey, refund.plotId(), refund.player(), refund.amount(), false, () -> {
            if (!vault.deposit(Bukkit.getOfflinePlayer(refund.player()), refund.amount())) return false;
            receipt.accept(refund);
            save(withStatus(refund, Status.PAID));
            return true;
        });
        if (!paid) {
            OperationJournal.Status operationStatus = journal.latest(operationKey).orElseThrow().status();
            int attempts = refund.attempts() + 1;
            save(new Refund(refund.id(), refund.key(), refund.kind(), refund.plotId(), refund.releaseId(), refund.player(),
                    refund.amount(), refund.reason(), operationStatus == OperationJournal.Status.REVIEW_REQUIRED
                    ? Status.REVIEW_REQUIRED : Status.PENDING, refund.created(),
                    Instant.now().plusSeconds(Math.min(pl.riftplots.config.Settings.longValue("refunds.retry-maximum-seconds"),
                            (long) (pl.riftplots.config.Settings.longValue("refunds.retry-initial-seconds") * Math.pow(
                                    pl.riftplots.config.Settings.decimal("refunds.retry-multiplier"), Math.min(30, attempts - 1))))), attempts));
        }
        return paid;
    }

    /**
     * Records an operator's verified outcome. This method never calls the economy provider.
     */
    public synchronized void resolve(String id, Status status, String operator, String reason) {
        Refund refund = refunds.get(id);
        if (!journal.isWriteAllowed() || journal.isRunning())
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.refund-service.rozliczenia-sa-wstrzymane"));
        if (refund == null || refund.status() != Status.REVIEW_REQUIRED || status == Status.REVIEW_REQUIRED
                || reason == null || reason.isBlank())
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.refund-service.wymagany-zwrot-do-uzgodnienia-stan-koncowy-i"));
        var operation = journal.latest("refund:" + id);
        if (operation.isPresent() && operation.get().status() == OperationJournal.Status.REVIEW_REQUIRED) {
            journal.resolve(operation.get().id(), status == Status.PAID ? OperationJournal.Status.COMPLETED : OperationJournal.Status.CANCELLED, operator, reason);
        }
        if (status == Status.PENDING && operation.isPresent() && operation.get().status() == OperationJournal.Status.COMPLETED)
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.refund-service.potwierdzona-wypata-nie-moze-zostac-ponowiona"));
        save(new Refund(refund.id(), refund.key(), refund.kind(), refund.plotId(), refund.releaseId(), refund.player(), refund.amount(),
                pl.riftplots.config.Texts.raw("text.refund-service.uzgodniono-przez", refund.reason(), operator, reason), status, refund.created(), Instant.EPOCH, refund.attempts()));
    }

    public synchronized void retryPending() {
        for (Refund refund : new ArrayList<>(refunds.values())) {
            if (refund.status() != Status.PAID && refund.status() != Status.CANCELLED && !refund.nextAttempt().isAfter(Instant.now())) {
                try {
                    retry(refund.id());
                } catch (RuntimeException e) {
                    Logger.getLogger("riftPlots").log(Level.SEVERE, pl.riftplots.config.Texts.raw("text.refund-service.zwrot-wymaga-sprawdzenia", refund.id()), e);
                }
            }
        }
    }

    public synchronized void cancelPrepared(String key) {
        Refund refund = refunds.get(OperationJournal.idFor(key));
        if (refund != null && refund.status() == Status.PENDING) save(withStatus(refund, Status.CANCELLED));
    }

    private Refund withStatus(Refund refund, Status status) {
        return new Refund(refund.id(), refund.key(), refund.kind(), refund.plotId(), refund.releaseId(), refund.player(),
                refund.amount(), refund.reason(), status, refund.created(), refund.nextAttempt(), refund.attempts());
    }

    private void save(Refund refund) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("format", 1);
        yaml.set("id", refund.id());
        yaml.set("key", refund.key());
        yaml.set("kind", refund.kind());
        yaml.set("plot", refund.plotId());
        yaml.set("release", refund.releaseId());
        yaml.set("player", refund.player().toString());
        yaml.set("amount", refund.amount().toPlainString());
        yaml.set("reason", refund.reason());
        yaml.set("status", refund.status().name());
        yaml.set("created", refund.created().toEpochMilli());
        yaml.set("next-attempt", refund.nextAttempt().toEpochMilli());
        yaml.set("attempts", refund.attempts());
        try {
            YamlDataFile.save(yaml, new File(directory, refund.id() + ".yml"));
        } catch (IOException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.refund-service.nie-mozna-zapisac-zobowiazania", refund.id()), e);
        }
        refunds.put(refund.id(), refund);
    }
}
