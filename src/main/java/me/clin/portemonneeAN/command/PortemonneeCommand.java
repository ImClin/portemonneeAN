package me.clin.portemonneeAN.command;

import me.clin.portemonneeAN.PortemonneeAN;
import me.clin.portemonneeAN.wallet.WalletManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Een simpele testcommand om portemonnees uit te delen tijdens ontwikkeling.
 */
public final class PortemonneeCommand implements CommandExecutor, TabCompleter {

    private final PortemonneeAN plugin;

    public PortemonneeCommand(PortemonneeAN plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Gebruik: /" + label + " give [speler] | /" + label + " admingui"));
            return true;
        }

        if (args[0].equalsIgnoreCase("admingui")) {
            return handleAdminGui(sender, label);
        }

        if (!args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Onbekende subcommand. Gebruik: /" + label + " give [speler] | /" + label + " admingui"));
            return true;
        }

        WalletManager walletManager = plugin.getWalletManager();

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Speler niet gevonden."));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Geef een speler op."));
            return true;
        }

        ItemStack baseItem = new ItemStack(Material.NETHER_STAR);
        ItemStack wallet = walletManager.createWalletItem(baseItem);

        target.getInventory().addItem(wallet).values().forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));

        sender.sendMessage(Component.text("Portemonnee uitgedeeld aan " + target.getName() + "."));
        if (target != sender) {
            target.sendMessage(Component.text("Je hebt een testportemonnee ontvangen."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if ("give".startsWith(args[0].toLowerCase())) {
                options.add("give");
            }
            if ("admingui".startsWith(args[0].toLowerCase())) {
                options.add("admingui");
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(player.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }

    private boolean handleAdminGui(CommandSender sender, String label) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Alleen spelers kunnen dit command gebruiken."));
            return true;
        }
        if (!player.hasPermission("portemonnee.admin")) {
            player.sendMessage(Component.text("Je hebt geen toestemming voor dit command."));
            return true;
        }

        plugin.getAdminMenu().open(player);
        return true;
    }
}