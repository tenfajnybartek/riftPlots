package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.util.AdventureUtil;

public class HelpSubCommand implements SubCommand {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_HELP;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.help.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.help.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.system-dziaek-riftplots"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-list-twoje-dziaki-oraz-nieruchomosci-na"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-info-szczegoowe-informacje-o-dziace"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-buy-zakup-przygotowanej-nieruchomosci"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-pay-okresy-opac-podatek-za-dziake"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-extend-okresy-przeduz-waznosc-dziaki-z"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-history-historia-opat-i-transakcji"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-trust-dodaj-wspouzytkownika-do-dziaki"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-untrust-usun-wspouzytkownika"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-members-lista-czonkow-nieruchomosci"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-abandon-porzuc-dziake-i-odbierz-czesciowy"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-gui-otworz-panel-graficzny-dziaek"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-border-dziaka-pokaz-granice-3d-przez"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-rent-info-umowa-kaucja-i-automatyczne"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-auction-history-historia-licytacji"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-notifications-strona-zapisane-powiadomienia"));

        if (sender.hasPermission(PermissionConfiguration.ADMIN_BASE)) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.help.plots-admin-zaawansowane-komendy-administracyjne"));
        }
        return true;
    }
}
