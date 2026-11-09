package pl.riftplots.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.storage.repository.PlayerProfileRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class AdminSetOwnerSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotSignService signService;
    private final PlayerProfileRepository profileRepository;
    private final MessageConfiguration messageConfig;

    public AdminSetOwnerSubCommand(PlotManager plotManager, PlotSignService signService, PlayerProfileRepository profileRepository, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.signService = signService;
        this.profileRepository = profileRepository;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "setowner";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SETOWNER;
    }

    @Override
    public String getUsage() {
        return "plots admin setowner <id> <gracz|none>";
    }

    @Override
    public String getDescription() {
        return "Ustawia lub usuwa właściciela danej działki.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        String id = args[0].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(id);
        if (opt.isEmpty()) {
            sender.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", id)));
            return true;
        }

        Plot plot = opt.get();
        if (plot.getStatus() == PlotStatus.RESETTING || plot.isAuctionActive() || plot.isRented()) {
            sender.sendMessage(messageConfig.get("transfer.not-ready"));
            return true;
        }
        String targetName = args[1];

        if ("none".equalsIgnoreCase(targetName) || "null".equalsIgnoreCase(targetName)) {
            plot.clearOwnership();
            plot.setStatus(PlotStatus.AVAILABLE);
            plot.setOwnerUuid(null);
            plot.setOwnerName(null);
            plot.clearMembers();
            plot.setPurchaseDate(null);
            plot.setNextTaxPayment(null);
            plot.setGracePeriodExpiry(null);
            plotManager.savePlot(plot);
            signService.updateSignsForPlot(plot);

            sender.sendMessage(messageConfig.get("admin.owner-set", Map.of(
                    "plot_id", id,
                    "owner", "Brak (Udostępniona do zakupu)"
            )));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null) {
            sender.sendMessage(messageConfig.get("members.player-not-found", Map.of("target", targetName)));
            return true;
        }

        plot.setStatus(PlotStatus.OWNED);
        plot.setOwnerUuid(target.getUniqueId());
        plot.setOwnerName(target.getName());
        plot.clearMembers();
        plot.clearHomeLocation();
        plot.setAdvancePeriodsPaid(0);
        Instant now = Instant.now();
        plot.setPurchaseDate(now);
        plot.setNextTaxPayment(now.plus(Duration.ofDays(plot.getBillingPeriodDays())));
        plot.setGracePeriodExpiry(null);
        plotManager.savePlot(plot);

        Optional<PlotPlayerProfile> optProfile = profileRepository.findById(target.getUniqueId());
        PlotPlayerProfile profile = optProfile.orElseGet(() -> new PlotPlayerProfile(
                target.getUniqueId(), target.getName(), now, now, new HashSet<>()
        ));
        profile.addOwnedPlot(plot.getId());
        profileRepository.save(profile);

        signService.updateSignsForPlot(plot);

        sender.sendMessage(messageConfig.get("admin.owner-set", Map.of(
                "plot_id", id,
                "owner", target.getName()
        )));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            List<String> list = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).toList());
            list.add("none");
            return list.stream().filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
