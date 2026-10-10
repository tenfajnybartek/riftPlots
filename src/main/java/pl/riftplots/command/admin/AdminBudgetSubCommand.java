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
        return "plots admin budget <balance|add <kwota> [powód]|withdraw <kwota> [powód]|history>";
    }

    @Override
    public String getDescription() {
        return "Zarządzanie wirtualnym budżetem miejskim.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "balance".equalsIgnoreCase(args[0])) {
            BigDecimal bal = budgetService.getBalance();
            AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== BUDŻET MIEJSKI ===</b></gradient>");
            AdventureUtil.sendMessage(sender, "<gray>Aktualne saldo miasta: </gray><green><b>" + bal + " $</b></green>");
            return true;
        }

        if ("history".equalsIgnoreCase(args[0])) {
            List<CityTransaction> list = budgetService.getRecentTransactions(15);
            AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== OSTATNIE OPERACJE BUDŻETU MIASTA ===</b></gradient>");
            if (list.isEmpty()) {
                AdventureUtil.sendMessage(sender, "<gray>Brak historii operacji.</gray>");
                return true;
            }
            for (CityTransaction tx : list) {
                AdventureUtil.sendMessage(sender, "<gray>[" + TimeUtil.formatInstant(tx.getTimestamp()) + "]</gray> <gold>" + tx.getType() + "</gold> <yellow>" + tx.getAmount() + " $</yellow> <gray>(Saldo: " + tx.getBalanceAfter() + " $, " + tx.getDescription() + ")</gray>");
            }
            return true;
        }

        if ("add".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage("§cUżycie: /plots admin budget " + args[0].toLowerCase() + " <kwota> [powód]");
                return true;
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(args[1]);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    sender.sendMessage("§cKwota musi być większa od zera!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cPodana kwota jest nieprawidłowa!");
                return true;
            }

            if ("withdraw".equalsIgnoreCase(args[0])) {
                amount = amount.negate();
            }

            String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Korekta administracyjna";
            UUID opUuid = sender instanceof Player p ? p.getUniqueId() : null;

            budgetService.adjustBalance(amount, opUuid, reason);
            sender.sendMessage("§aZaktualizowano budżet miasta. Nowe saldo: " + budgetService.getBalance() + " $.");
            return true;
        }

        sender.sendMessage("§cUżycie: /" + getUsage());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("balance", "add", "withdraw", "history");
        } else if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "withdraw".equalsIgnoreCase(args[0]))) {
            return List.of("1000", "5000", "10000", "50000", "100000");
        }
        return Collections.emptyList();
    }
}
