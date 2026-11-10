package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.city.CityTransaction;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.math.BigDecimal;
import java.util.*;

public class AdminBudgetSubCommand implements SubCommand {
    private final CityBudgetService budgetService;

    public AdminBudgetSubCommand(CityBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Override
    public String getName() {
        return "budget";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_BUDGET;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-budget.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-budget.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "balance".equalsIgnoreCase(args[0])) {
            BigDecimal bal = budgetService.getBalance();
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-budget.budzet-miejski"));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-budget.aktualne-saldo-miasta", bal));
            return true;
        }

        if ("history".equalsIgnoreCase(args[0])) {
            List<CityTransaction> list = budgetService.getRecentTransactions(pl.riftplots.config.Settings.integer("paging.budget"));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-budget.ostatnie-operacje-budzetu-miasta"));
            if (list.isEmpty()) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-budget.brak-historii-operacji"));
                return true;
            }
            for (CityTransaction tx : list) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-budget.saldo", TimeUtil.formatInstant(tx.getTimestamp()), tx.getType(), tx.getAmount(), tx.getBalanceAfter(), tx.getDescription()));
            }
            return true;
        }

        if ("add".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-budget.uzycie-plots-admin-budget-powod", args[0].toLowerCase()));
                return true;
            }
            BigDecimal amount;
            try {
                amount = pl.riftplots.util.Money.parse(args[1]);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-budget.kwota-musi-byc-wieksza-od-zera"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-budget.podana-kwota-jest-nieprawidowa"));
                return true;
            }

            if ("withdraw".equalsIgnoreCase(args[0])) {
                amount = amount.negate();
            }

            String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : pl.riftplots.config.Texts.raw("text.admin-budget.korekta-administracyjna");
            UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;

            budgetService.adjustBalance(amount, opUuid, reason);
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-budget.zaktualizowano-budzet-miasta-nowe-saldo", budgetService.getBalance()));
            return true;
        }

        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-budget.uzycie", getUsage()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("balance", "add", "withdraw", "history");
        } else if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0]))) {
            return pl.riftplots.config.Settings.strings("tab-completion.budget-amounts");
        }
        return Collections.emptyList();
    }
}
