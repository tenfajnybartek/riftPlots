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
        return pl.riftplots.config.Texts.raw("text.admin-transactions.usage");
    }

    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-transactions.description");
    }

    public boolean execute(CommandSender sender, String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("pending")) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.operacje-wymagajace-sprawdzenia", journal.pending().size()));
                journal.pending().stream().limit(pl.riftplots.config.Settings.integer("paging.pending-transactions")).forEach(e -> sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.line", e.id(), e.kind(), e.plotId(), e.status())));
            } else if (args[0].equalsIgnoreCase("inspect") && args.length == 2) {
                var entry = journal.find(args[1]).orElseThrow(() -> new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.admin-transactions.nie-ma-takiej-operacji")));
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.line", entry.id(), entry.kind(), entry.status(), entry.amount()));
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.dziaka-utworzono", entry.plotId(), entry.created(), entry.error()));
                entry.steps().forEach(s -> sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.line-66ed3c0", s.action(), s.player(), s.amount(), s.state())));
            } else if (args[0].equalsIgnoreCase("refunds")) {
                refunds.list(null).stream().filter(r -> r.status() != RefundService.Status.PAID && r.status() != RefundService.Status.CANCELLED)
                        .limit(pl.riftplots.config.Settings.integer("paging.pending-refunds")).forEach(r -> sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.line-31563b0", r.id(), r.player(), r.amount(), r.status(), r.reason())));
            } else if (args[0].equalsIgnoreCase("retry") && args.length == 2) {
                sender.sendMessage(Component.text(refunds.retry(args[1]) ? pl.riftplots.config.Texts.raw("text.admin-transactions.zwrot-rozliczony") : pl.riftplots.config.Texts.raw("text.admin-transactions.zwrot-oczekuje-lub-wymaga-uzgodnienia-sprawdz-inspect")));
            } else if (args[0].equalsIgnoreCase("resolve") && args.length >= 5 && args[3].equalsIgnoreCase("confirm")) {
                journal.resolve(args[1], OperationJournal.Status.valueOf(args[2].toUpperCase(Locale.ROOT)), sender.getName(), String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                refunds.retryPending();
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.zapisano-reczne-uzgodnienie-zadna-patnosc-z-operacji"));
            } else if (args[0].equalsIgnoreCase("refund-resolve") && args.length >= 5 && args[3].equalsIgnoreCase("confirm")) {
                refunds.resolve(args[1], RefundService.Status.valueOf(args[2].toUpperCase(Locale.ROOT)), sender.getName(), String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.zapisano-uzgodnienie-zwrotu-pending-zezwala-na-kolejna"));
            } else
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-transactions.uzycie-przed-resolve-sprawdz-portfele-i-wasnosc", getUsage()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(e.getMessage()));
        }
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? List.of("pending", "inspect", "refunds", "retry", "resolve", "refund-resolve") : List.of();
    }
}
