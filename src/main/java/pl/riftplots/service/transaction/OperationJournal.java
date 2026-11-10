package pl.riftplots.service.transaction;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.storage.yaml.YamlDataFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Backend-independent write-ahead journal. An uncertain Vault call is never replayed.
 */
public final class OperationJournal {
    public enum Status {RUNNING, COMPLETED, CANCELLED, REVIEW_REQUIRED}

    public record Step(String action, UUID player, BigDecimal amount, String state) {
    }

    public record Entry(String id, String key, String kind, String plotId, UUID actor,
                        BigDecimal amount, Instant created, Instant updated, Status status,
                        String error, boolean blocksPlot, List<Step> steps) {
        public Entry {
            steps = List.copyOf(steps);
        }
    }

    private final File directory;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final ThreadLocal<String> current = new ThreadLocal<>();
    private final Set<String> deferred = new HashSet<>();
    private java.util.function.BooleanSupplier writeAllowed = () -> true;

    public void setWriteAllowed(java.util.function.BooleanSupplier allowed) {
        writeAllowed = allowed;
    }

    public boolean isWriteAllowed() {
        return writeAllowed.getAsBoolean();
    }

    private long lastCreated;

    public OperationJournal(File dataFolder) {
        directory = new File(dataFolder, "data/operations");
    }

    public synchronized void initialize() {
        entries.clear();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            if (directory.exists() && !directory.isDirectory())
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.operation-journal.invalid-journal-directory"));
            return;
        }
        for (File file : files) {
            YamlConfiguration yaml = YamlDataFile.load(file);
            if (yaml.getInt("format", 1) != 1)
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.operation-journal.unsupported-journal-format", file));
            List<Step> steps = new ArrayList<>();
            for (Map<?, ?> row : yaml.getMapList("steps")) {
                steps.add(new Step(row.get("action").toString(), UUID.fromString(row.get("player").toString()),
                        new BigDecimal(row.get("amount").toString()), row.get("state").toString()));
            }
            String actor = yaml.getString("actor");
            Entry entry = new Entry(yaml.getString("id"), yaml.getString("key"), yaml.getString("kind"),
                    yaml.getString("plot"), actor != null ? UUID.fromString(actor) : null,
                    new BigDecimal(yaml.getString("amount", "0")), Instant.ofEpochMilli(yaml.getLong("created")),
                    Instant.ofEpochMilli(yaml.getLong("updated")), Status.valueOf(yaml.getString("status")),
                    yaml.getString("error", ""), yaml.getBoolean("blocks-plot", true), steps);
            if (!file.getName().equals(entry.id() + ".yml"))
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.operation-journal.invalid-journal-id", file));
            entries.put(entry.id(), entry);
            lastCreated = Math.max(lastCreated, entry.created().toEpochMilli());
            if (entry.status() == Status.RUNNING) {
                update(entry, Status.REVIEW_REQUIRED, pl.riftplots.config.Texts.raw("text.operation-journal.operacja-przerwana-restartem-sprawdz-zapisane-etapy-przed"), steps);
            }
        }
    }

    public boolean isRunning() {
        return current.get() != null;
    }

    public String currentId() {
        return current.get();
    }

    public synchronized void deferCurrent() {
        if (!isRunning())
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.operation-journal.no-active-operation"));
        deferred.add(currentId());
    }

    public synchronized void finishDeferred(String id, boolean success) {
        Entry entry = entries.get(id);
        if (entry == null || entry.status() != Status.RUNNING) return;
        deferred.remove(id);
        update(entry, success ? Status.COMPLETED : Status.CANCELLED, success ? "" : pl.riftplots.config.Texts.raw("text.operation-journal.operacja-asynchroniczna-odrzucona"), entry.steps());
    }

    public synchronized Optional<Entry> find(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public synchronized Optional<Entry> latest(String key) {
        return entries.values().stream().filter(e -> e.key().equals(key))
                .max(Comparator.comparing(Entry::created).thenComparing(Entry::id));
    }

    public synchronized List<Entry> pending() {
        return entries.values().stream().filter(e -> e.status() == Status.REVIEW_REQUIRED || e.status() == Status.RUNNING).toList();
    }

    public synchronized List<Entry> history(UUID player, String plotId, int page, int size) {
        return entries.values().stream()
                .filter(e -> player == null || player.equals(e.actor()) || e.steps().stream().anyMatch(s -> player.equals(s.player())))
                .filter(e -> plotId == null || plotId.equalsIgnoreCase(e.plotId()))
                .sorted(Comparator.comparing(Entry::created).reversed().thenComparing(Entry::id))
                .skip((long) Math.max(0, page) * size).limit(size).toList();
    }

    public synchronized boolean isBlocked(String plotId) {
        return isBlockedExcept(plotId, null);
    }

    public synchronized boolean isBlockedExcept(String plotId, String allowedOperation) {
        return plotId != null && entries.values().stream().anyMatch(e -> plotId.equalsIgnoreCase(e.plotId())
                && !e.id().equals(allowedOperation)
                && e.blocksPlot() && (e.status() == Status.RUNNING || e.status() == Status.REVIEW_REQUIRED));
    }

    public synchronized void setCurrentAmount(BigDecimal amount) {
        if (!isRunning()) return;
        if (!pl.riftplots.util.Money.isSafe(amount))
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.operation-journal.nieprawidowa-kwota-koncowa"));
        Entry old = entries.get(currentId());
        Entry updated = new Entry(old.id(), old.key(), old.kind(), old.plotId(), old.actor(), amount, old.created(), Instant.now(), old.status(), old.error(), old.blocksPlot(), old.steps());
        persist(updated);
        entries.put(updated.id(), updated);
    }

    public boolean execute(String kind, String key, String plotId, UUID actor, BigDecimal amount, BooleanSupplier action) {
        return execute(kind, key, plotId, actor, amount, true, action);
    }

    public synchronized boolean execute(String kind, String key, String plotId, UUID actor, BigDecimal amount,
                                        boolean blocksPlot, BooleanSupplier action) {
        if (!isWriteAllowed()) return false;
        if (amount != null && !pl.riftplots.util.Money.isSafe(amount)) return false;
        List<Entry> attempts = entries.values().stream().filter(e -> e.key().equals(key)).toList();
        if (attempts.stream().anyMatch(e -> e.status() != Status.CANCELLED) || (blocksPlot && isBlocked(plotId)))
            return false;
        String parent = current.get();
        String id = idFor(key + ":attempt:" + attempts.size());
        Instant created = Instant.ofEpochMilli(lastCreated = Math.max(System.currentTimeMillis(), lastCreated + 1));
        Entry entry = new Entry(id, key, kind, plotId, actor, amount != null ? amount : BigDecimal.ZERO,
                created, created, Status.RUNNING, "", blocksPlot, List.of());
        persist(entry);
        entries.put(id, entry);
        current.set(id);
        try {
            boolean result = action.getAsBoolean();
            Entry latest = entries.get(id);
            if (result && deferred.contains(id)) return true;
            if (latest.status() == Status.COMPLETED || latest.status() == Status.CANCELLED)
                return result && latest.status() == Status.COMPLETED;
            boolean unresolved = latest.steps().stream().anyMatch(s -> s.state().equals("IN_FLIGHT"));
            Status status = result && !unresolved ? Status.COMPLETED
                    : !unresolved && isBalanced(latest.steps()) ? Status.CANCELLED : Status.REVIEW_REQUIRED;
            update(latest, status, status == Status.REVIEW_REQUIRED ? pl.riftplots.config.Texts.raw("text.operation-journal.operacja-nie-zakonczya-sie-uzgodnij-patnosci-i") : "", latest.steps());
            return result && status == Status.COMPLETED;
        } catch (RuntimeException | Error failure) {
            deferred.remove(id);
            Entry latest = entries.get(id);
            try {
                update(latest, Status.REVIEW_REQUIRED, failure.getClass().getSimpleName() + ": " + failure.getMessage(), latest.steps());
            } catch (RuntimeException journalFailure) {
                failure.addSuppressed(journalFailure);
            }
            throw failure;
        } finally {
            if (parent == null) current.remove();
            else current.set(parent);
        }
    }

    public synchronized boolean effect(String action, UUID player, BigDecimal amount, BooleanSupplier call) {
        if (!isRunning()) return call.getAsBoolean();
        Entry entry = entries.get(current.get());
        List<Step> steps = new ArrayList<>(entry.steps());
        steps.add(new Step(action, player, amount, "IN_FLIGHT"));
        update(entry, Status.RUNNING, "", steps);
        boolean success = call.getAsBoolean();
        steps.set(steps.size() - 1, new Step(action, player, amount, success ? "APPLIED" : "DECLINED"));
        update(entries.get(current.get()), Status.RUNNING, "", steps);
        return success;
    }

    public synchronized void resolve(String id, Status status, String operator, String reason) {
        Entry entry = entries.get(id);
        if (entry == null || entry.status() != Status.REVIEW_REQUIRED
                || (status != Status.COMPLETED && status != Status.CANCELLED) || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.operation-journal.wymagane-operacja-do-uzgodnienia-stan-koncowy-i"));
        }
        update(entry, status, pl.riftplots.config.Texts.raw("text.operation-journal.uzgodniono-recznie-przez", operator, reason), entry.steps());
    }

    public static String idFor(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private boolean isBalanced(List<Step> steps) {
        Map<UUID, BigDecimal> totals = new HashMap<>();
        for (Step step : steps) {
            if (step.state().equals("APPLIED")) totals.merge(step.player(),
                    step.action().equals("WITHDRAW") ? step.amount().negate() : step.amount(), BigDecimal::add);
        }
        return totals.values().stream().allMatch(amount -> amount.signum() == 0);
    }

    private void update(Entry entry, Status status, String error, List<Step> steps) {
        Entry updated = new Entry(entry.id(), entry.key(), entry.kind(), entry.plotId(), entry.actor(), entry.amount(),
                entry.created(), Instant.now(), status, error, entry.blocksPlot(), steps);
        // Keep the block in memory even if persisting a post-payment stage fails.
        entries.put(updated.id(), updated);
        persist(updated);
    }

    private void persist(Entry entry) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("format", 1);
        yaml.set("id", entry.id());
        yaml.set("key", entry.key());
        yaml.set("kind", entry.kind());
        yaml.set("plot", entry.plotId());
        yaml.set("actor", entry.actor() != null ? entry.actor().toString() : null);
        yaml.set("amount", entry.amount().toPlainString());
        yaml.set("created", entry.created().toEpochMilli());
        yaml.set("updated", entry.updated().toEpochMilli());
        yaml.set("status", entry.status().name());
        yaml.set("error", entry.error());
        yaml.set("blocks-plot", entry.blocksPlot());
        yaml.set("steps", entry.steps().stream().map(s -> Map.of("action", s.action(), "player", s.player().toString(),
                "amount", s.amount().toPlainString(), "state", s.state())).toList());
        try {
            YamlDataFile.save(yaml, new File(directory, entry.id() + ".yml"));
        } catch (IOException e) {
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.operation-journal.nie-mozna-zapisac-dziennika-operacji", entry.id()), e);
        }
    }
}
