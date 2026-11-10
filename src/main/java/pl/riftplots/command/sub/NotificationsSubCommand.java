package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.BillingArguments;
import pl.riftplots.command.SubCommand;
import pl.riftplots.service.notification.NotificationService;
import pl.riftplots.util.AdventureUtil;

public final class NotificationsSubCommand implements SubCommand {
    private final NotificationService notifications;

    public NotificationsSubCommand(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override
    public String getName() {
        return "notifications";
    }

    @Override
    public String getPermission() {
        return "riftplots.user.history";
    }

    @Override
    public String getUsage() {
        return "plots notifications [strona]";
    }

    @Override
    public String getDescription() {
        return "Pokazuje zapisane powiadomienia i datę wysłania przez serwer.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda wymaga gracza.");
            return true;
        }
        int page;
        try {
            if (args.length > 1) throw new IllegalArgumentException();
            page = args.length == 0 ? 0 : BillingArguments.positiveInt(args[0]) - 1;
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Użycie: /" + getUsage());
            return true;
        }
        player.sendMessage("Powiadomienia — strona " + (page + 1));
        var notices = notifications.getStore().history(player.getUniqueId(), page);
        if (notices.isEmpty()) player.sendMessage("Brak powiadomień na tej stronie.");
        for (var notice : notices) {
            player.sendMessage(net.kyori.adventure.text.Component.text(notice.created() + " | "
                    + (notice.delivered() == null ? "oczekujące" : "wysłane " + notice.delivered())));
            player.sendMessage(AdventureUtil.parse(notice.message()));
        }
        return true;
    }
}
