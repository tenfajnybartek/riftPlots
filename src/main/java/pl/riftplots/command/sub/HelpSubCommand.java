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
        return "plots help";
    }

    @Override
    public String getDescription() {
        return "Wyświetla listę dostępnych komend systemu działek.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== SYSTEM DZIAŁEK RIFTPLOTS ===</b></gradient>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots list</yellow> <gray>- Twoje działki oraz nieruchomości na sprzedaż</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots info <działka></yellow> <gray>- Szczegółowe informacje o działce</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots buy <działka></yellow> <gray>- Zakup przygotowanej nieruchomości</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots pay <działka> [okresy]</yellow> <gray>- Opłać podatek za działkę</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots extend <działka> [okresy]</yellow> <gray>- Przedłuż ważność działki z góry</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots history <działka></yellow> <gray>- Historia opłat i transakcji</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots trust <gracz> <działka></yellow> <gray>- Dodaj współużytkownika do działki</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots untrust <gracz> <działka></yellow> <gray>- Usuń współużytkownika</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots members <działka></yellow> <gray>- Lista członków nieruchomości</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots abandon <działka></yellow> <gray>- Porzuć działkę i odbierz częściowy zwrot</gray>");
        AdventureUtil.sendMessage(sender, "<yellow>/plots gui</yellow> <gray>- Otwórz panel graficzny działek</gray>");

        if (sender.hasPermission(PermissionConfiguration.ADMIN_BASE)) {
            AdventureUtil.sendMessage(sender, "<red>/plots admin</red> <gray>- Zaawansowane komendy administracyjne</gray>");
        }
        return true;
    }
}
