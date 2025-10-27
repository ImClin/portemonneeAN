package me.clin.portemonneeAN.wallet;

import me.clin.portemonneeAN.PortemonneeAN;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Simpele beheerinterface om de cash items te configureren.
 */
public final class CashAdminMenu implements Listener {

    private static final WalletDenomination[] ORDER = {
            WalletDenomination.EURO_500,
            WalletDenomination.EURO_200,
            WalletDenomination.EURO_100,
            WalletDenomination.EURO_50,
            WalletDenomination.EURO_20,
            WalletDenomination.EURO_10,
            WalletDenomination.EURO_5,
            WalletDenomination.EURO_1
    };
    private static final int INVENTORY_SIZE = 9;

    private final PortemonneeAN plugin;

    public CashAdminMenu(PortemonneeAN plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new CashAdminHolder(), INVENTORY_SIZE, Component.text("Portemonnee biljetten"));
        populateInventory(inventory);
        inventory.setItem(INVENTORY_SIZE - 1, createInfoItem());

        player.openInventory(inventory);
        player.sendMessage(Component.text("Plaats de Minetopia biljetten in de slots van hoog naar laag (500 → 1).", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Leeg slot = coupure verwijderen. Sluit de GUI om op te slaan.", NamedTextColor.GRAY));
    }

    private void populateInventory(Inventory inventory) {
        for (int slot = 0; slot < ORDER.length; slot++) {
            WalletDenomination denomination = ORDER[slot];
            ItemStack template = plugin.getWalletManager().getCashTemplate(denomination);
            if (template != null) {
                inventory.setItem(slot, template);
            }
        }
    }

    private ItemStack createInfoItem() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Instructies", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("Slots 1-8: biljetten 500 → 1", NamedTextColor.GRAY),
                    Component.text("Laat leeg om te verwijderen", NamedTextColor.GRAY)
            ));
            paper.setItemMeta(meta);
        }
        return paper;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CashAdminHolder)) {
            return;
        }
        if (event.getRawSlot() == INVENTORY_SIZE - 1) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CashAdminHolder)) {
            return;
        }

        Inventory inventory = event.getInventory();
        Map<WalletDenomination, ItemStack> templates = new EnumMap<>(WalletDenomination.class);

        for (int slot = 0; slot < ORDER.length; slot++) {
            WalletDenomination denomination = ORDER[slot];
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                templates.put(denomination, stack.clone());
                plugin.getConfig().set(denomination.getConfigPath(), stack.clone());
            } else {
                plugin.getConfig().set(denomination.getConfigPath(), null);
            }
        }

        plugin.saveConfig();
        plugin.getWalletManager().setCashTemplates(templates);

        if (event.getPlayer() instanceof Player player) {
            player.sendMessage(Component.text("Portemonnee biljetten opgeslagen.", NamedTextColor.GREEN));
        }
    }

    private static final class CashAdminHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
