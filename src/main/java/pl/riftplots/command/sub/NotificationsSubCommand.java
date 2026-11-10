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
        return pl.riftplots.config.Texts.raw("text.notifications.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.notifications.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.notifications.ta-komenda-wymaga-gracza"));
            return true;
        }
        int page;
        try {
            if (args.length > 1) throw new IllegalArgumentException();
            page = args.length == 0 ? 0 : BillingArguments.positiveInt(args[0]) - 1;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.notifications.uzycie", getUsage()));
            return true;
        }
        player.sendMessage(pl.riftplots.config.Texts.component("text.notifications.powiadomienia-strona", (page + 1)));
        var notices = notifications.getStore().history(player.getUniqueId(), page);
        if (notices.isEmpty())
            player.sendMessage(pl.riftplots.config.Texts.component("text.notifications.brak-powiadomien-na-tej-stronie"));
        for (var notice : notices) {
            player.sendMessage(pl.riftplots.config.Texts.component("text.notifications.line", notice.created(), (notice.delivered() == null ? pl.riftplots.config.Texts.raw("text.notifications.oczekujace") : pl.riftplots.config.Texts.raw("text.notifications.wysane", notice.delivered()))));
            player.sendMessage(AdventureUtil.parse(notice.message()));
        }
        return true;
    }
}
