package pl.riftplots.command.admin;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.service.transaction.*;

import java.util.*;

public final class AdminTransactionsSubCommand implements SubCommand {
    private final OperationJournal journal;
    private final RefundService refunds;

    public AdminTransactionsSubCommand(OperationJournal journal, RefundService refunds) {
        this.journal = journal;
        this.refunds = refunds;
    }

    public String getName() {
        return "transactions";
    }

    public String getPermission() {
        return "riftplots.admin.transactions";
    }

    public String getUsage() {
        return "plots admin transactions pending|inspect <id>|refunds|retry <refund_id>|resolve <id> COMPLETED|CANCELLED confirm <powód>|refund-resolve <id> PAID|PENDING|CANCELLED confirm <powód>";
    }

    public String getDescription() {
        return "Pokazuje i uzgadnia trwałe operacje finansowe.";
    }

    public boolean execute(CommandSender sender, String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("pending")) {
                sender.sendMessage(Component.text("Operacje wymagające sprawdzenia: " + journal.pending().size()));
                journal.pending().stream().limit(30).forEach(e -> sender.sendMessage(Component.text(e.id() + " | " + e.kind() + " | " + e.plotId() + " | " + e.status())));
            } else if (args[0].equalsIgnoreCase("inspect") && args.length == 2) {
                var entry = journal.find(args[1]).orElseThrow(() -> new IllegalArgumentException("Nie ma takiej operacji."));
                sender.sendMessage(Component.text(entry.id() + " | " + entry.kind() + " | " + entry.status() + " | " + entry.amount()));
                sender.sendMessage(Component.text("Działka: " + entry.plotId() + "; utworzono: " + entry.created() + "; " + entry.error()));
                entry.steps().forEach(s -> sender.sendMessage(Component.text(s.action() + " " + s.player() + " " + s.amount() + " " + s.state())));
            } else if (args[0].equalsIgnoreCase("refunds")) {
                refunds.list(null).stream().filter(r -> r.status() != RefundService.Status.PAID && r.status() != RefundService.Status.CANCELLED)
                        .limit(30).forEach(r -> sender.sendMessage(Component.text(r.id() + " | " + r.player() + " | " + r.amount() + " | " + r.status() + " | " + r.reason())));
            } else if (args[0].equalsIgnoreCase("retry") && args.length == 2) {
                sender.sendMessage(Component.text(refunds.retry(args[1]) ? "Zwrot rozliczony." : "Zwrot oczekuje lub wymaga uzgodnienia. Sprawdź inspect."));
            } else if (args[0].equalsIgnoreCase("resolve") && args.length >= 5 && args[3].equalsIgnoreCase("confirm")) {
                journal.resolve(args[1], OperationJournal.Status.valueOf(args[2].toUpperCase(Locale.ROOT)), sender.getName(), String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                refunds.retryPending();
                sender.sendMessage(Component.text("Zapisano ręczne uzgodnienie. Żadna płatność z operacji nie została powtórzona."));
            } else if (args[0].equalsIgnoreCase("refund-resolve") && args.length >= 5 && args[3].equalsIgnoreCase("confirm")) {
                refunds.resolve(args[1], RefundService.Status.valueOf(args[2].toUpperCase(Locale.ROOT)), sender.getName(), String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                sender.sendMessage(Component.text("Zapisano uzgodnienie zwrotu. PENDING zezwala na kolejną próbę; ta komenda nie wypłaca pieniędzy."));
            } else
                sender.sendMessage(Component.text("Użycie: /" + getUsage() + ". Przed resolve sprawdź portfele i własność."));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(e.getMessage()));
        }
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? List.of("pending", "inspect", "refunds", "retry", "resolve", "refund-resolve") : List.of();
    }
}
