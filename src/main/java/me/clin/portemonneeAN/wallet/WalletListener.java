package me.clin.portemonneeAN.wallet;

import me.clin.portemonneeAN.wallet.WalletManager.ClaimResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Koppelt spelerinteracties aan de walletlogica.
 */
public final class WalletListener implements Listener {

    private final WalletManager walletManager;

    public WalletListener(WalletManager walletManager) {
        this.walletManager = walletManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> handleRightClick(event);
            default -> {
                // andere acties negeren we
            }
        }
    }

    private void handleRightClick(PlayerInteractEvent event) {
        EquipmentSlot originalSlot = event.getHand();
        if (originalSlot == null) {
            return;
        }

        // Beperk tot main/offhand om dubbele triggers te voorkomen.
        if (originalSlot != EquipmentSlot.HAND && originalSlot != EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        WalletClick click = resolveWalletClick(player, event.getAction(), originalSlot);
        if (click == null) {
            return;
        }

        ItemStack item = click.item();
        EquipmentSlot slot = click.slot();

        event.setCancelled(true);

        if (!walletManager.isRevealed(item)) {
            walletManager.reveal(player, item, slot);
            return;
        }

        ClaimResult result = walletManager.claim(player, item, slot);
        if (!result.isSuccess()) {
            walletManager.sendClaimFailureMessage(player, result);
        }
    }

    private WalletClick resolveWalletClick(Player player, Action action, EquipmentSlot slot) {
        ItemStack candidate = getItem(player, slot);
        if (walletManager.isWallet(candidate)) {
            return new WalletClick(candidate, slot);
        }

        if (action == Action.RIGHT_CLICK_AIR && slot == EquipmentSlot.HAND) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (walletManager.isWallet(offHand)) {
                return new WalletClick(offHand, EquipmentSlot.OFF_HAND);
            }
        }

        return null;
    }

    private ItemStack getItem(Player player, EquipmentSlot slot) {
        if (slot == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItemInMainHand();
    }

    private record WalletClick(ItemStack item, EquipmentSlot slot) {
    }
}