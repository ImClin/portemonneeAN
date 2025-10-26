package me.clin.portemonneeAN.wallet;

import me.clin.portemonneeAN.config.WalletSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Core logica voor de portemonnee items.
 */
public final class WalletManager {

    private final WalletSettings settings;
    private final Economy economy;
    private final Random random = new Random();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final DecimalFormat numberFormat;

    private final NamespacedKey walletKey;
    private final NamespacedKey amountKey;
    private final NamespacedKey revealedKey;
    private final NamespacedKey claimedKey;
    private final NamespacedKey uniqueKey;

    public WalletManager(Plugin plugin, WalletSettings settings, Economy economy) {
        this.settings = settings;
        this.economy = economy;

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("nl-NL"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        this.numberFormat = new DecimalFormat("#,##0", symbols);

        this.walletKey = new NamespacedKey(plugin, "wallet");
        this.amountKey = new NamespacedKey(plugin, "wallet_amount");
        this.revealedKey = new NamespacedKey(plugin, "wallet_revealed");
        this.claimedKey = new NamespacedKey(plugin, "wallet_claimed");
        this.uniqueKey = new NamespacedKey(plugin, "wallet_id");
    }

    public ItemStack createWalletItem(ItemStack baseStack) {
        ItemStack item = baseStack.clone();
        item.setAmount(1);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(walletKey, PersistentDataType.BYTE, (byte) 1);
        container.set(revealedKey, PersistentDataType.BYTE, (byte) 0);
        container.remove(amountKey);
        container.remove(claimedKey);
        container.set(uniqueKey, PersistentDataType.STRING, UUID.randomUUID().toString());

        meta.displayName(deserialize(settings.getDisplayName()));
        meta.lore(deserialize(settings.getUnrevealedLore()));

        item.setItemMeta(meta);
        return item;
    }

    public boolean isWallet(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(walletKey, PersistentDataType.BYTE);
    }

    public boolean isRevealed(ItemStack item) {
        if (!isWallet(item)) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(revealedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    public boolean isClaimed(ItemStack item) {
        if (!isWallet(item)) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(claimedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    public RevealResult reveal(Player player, ItemStack item, EquipmentSlot slot) {
        if (!isWallet(item)) {
            return RevealResult.notWallet();
        }

        ItemStack copy = item.clone();
        ItemMeta meta = Objects.requireNonNull(copy.getItemMeta());
        PersistentDataContainer container = meta.getPersistentDataContainer();

        int amount = randomAmount();
        container.set(amountKey, PersistentDataType.INTEGER, amount);
        container.set(revealedKey, PersistentDataType.BYTE, (byte) 1);

        if (!container.has(uniqueKey, PersistentDataType.STRING)) {
            container.set(uniqueKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        String formattedAmount = formatAmount(amount);
        meta.lore(deserialize(settings.getRevealedLore(formattedAmount)));
        copy.setItemMeta(meta);

        replaceItemInHand(player, slot, copy);

        player.sendMessage(deserialize(settings.getRevealMessage(formattedAmount)));

        String readyMessage = settings.getReadyToClaimMessage();
        if (!readyMessage.isEmpty()) {
            player.sendMessage(deserialize(readyMessage));
        }

        return RevealResult.success(amount, formattedAmount);
    }

    public ClaimResult claim(Player player, ItemStack item, EquipmentSlot slot) {
        if (!isWallet(item)) {
            return ClaimResult.notWallet();
        }

        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.getOrDefault(claimedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1) {
            return ClaimResult.alreadyClaimed();
        }

        Integer amount = container.get(amountKey, PersistentDataType.INTEGER);
        if (amount == null) {
            return ClaimResult.notRevealed();
        }

        if (economy == null) {
            return ClaimResult.economyUnavailable();
        }

        EconomyResponse response = economy.depositPlayer(player, amount.doubleValue());
        if (response == null) {
            return ClaimResult.economyError("Onbekende fout");
        }
        if (!response.transactionSuccess()) {
            String error = response.errorMessage == null ? "" : response.errorMessage;
            return ClaimResult.economyError(error);
        }

        container.set(claimedKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        removeItemFromHand(player, slot);

        String formattedAmount = formatAmount(amount);
        player.sendMessage(deserialize(settings.getClaimedMessage(formattedAmount)));

        return ClaimResult.claimed(amount, formattedAmount);
    }

    public void sendClaimFailureMessage(Player player, ClaimResult result) {
        Component message = switch (result.getStatus()) {
            case SUCCESS, NOT_WALLET -> Component.empty();
            case NOT_REVEALED -> deserialize(settings.getNotRevealedMessage());
            case ALREADY_CLAIMED -> deserialize(settings.getAlreadyClaimedMessage());
            case ECONOMY_UNAVAILABLE -> deserialize(settings.getEconomyUnavailableMessage());
            case ECONOMY_ERROR -> {
                String reason = result.errorMessage();
                if (reason == null || reason.isBlank()) {
                    yield deserialize(settings.getClaimFailedMessage());
                }
                yield deserialize(settings.getEconomyErrorMessage(reason));
            }
        };

        if (!message.equals(Component.empty())) {
            player.sendMessage(message);
        }
    }

    private void replaceItemInHand(Player player, EquipmentSlot slot, ItemStack newItem) {
        switch (slot) {
            case HAND -> player.getInventory().setItemInMainHand(newItem);
            case OFF_HAND -> player.getInventory().setItemInOffHand(newItem);
            default -> {
                // geen actie
            }
        }
    }

    private void removeItemFromHand(Player player, EquipmentSlot slot) {
        switch (slot) {
            case HAND -> player.getInventory().setItemInMainHand(null);
            case OFF_HAND -> player.getInventory().setItemInOffHand(null);
            default -> {
                // geen actie
            }
        }
    }

    private int randomAmount() {
        int min = settings.getMinAmount();
        int max = settings.getMaxAmount();
        if (max <= min) {
            return min;
        }
        return random.nextInt((max - min) + 1) + min;
    }

    private String formatAmount(int amount) {
        return numberFormat.format(amount);
    }

    private Component deserialize(String text) {
        return serializer.deserialize(Objects.requireNonNullElse(text, ""));
    }

    private List<Component> deserialize(List<String> lines) {
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(deserialize(line));
        }
        return components;
    }

    public record RevealResult(boolean success, int amount, String formattedAmount, Status status) {
        enum Status {
            SUCCESS,
            NOT_WALLET
        }

        static RevealResult success(int amount, String formattedAmount) {
            return new RevealResult(true, amount, formattedAmount, Status.SUCCESS);
        }

        static RevealResult notWallet() {
            return new RevealResult(false, 0, "", Status.NOT_WALLET);
        }
    }

    public record ClaimResult(Status status, int amount, String formattedAmount, String errorMessage) {
        enum Status {
            SUCCESS,
            NOT_WALLET,
            NOT_REVEALED,
            ALREADY_CLAIMED,
            ECONOMY_UNAVAILABLE,
            ECONOMY_ERROR
        }

        static ClaimResult claimed(int amount, String formattedAmount) {
            return new ClaimResult(Status.SUCCESS, amount, formattedAmount, "");
        }

        static ClaimResult notWallet() {
            return new ClaimResult(Status.NOT_WALLET, 0, "", "");
        }

        static ClaimResult notRevealed() {
            return new ClaimResult(Status.NOT_REVEALED, 0, "", "");
        }

        static ClaimResult alreadyClaimed() {
            return new ClaimResult(Status.ALREADY_CLAIMED, 0, "", "");
        }

        static ClaimResult economyUnavailable() {
            return new ClaimResult(Status.ECONOMY_UNAVAILABLE, 0, "", "");
        }

        static ClaimResult economyError(String reason) {
            return new ClaimResult(Status.ECONOMY_ERROR, 0, "", reason == null ? "" : reason);
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public Status getStatus() {
            return status;
        }
    }
}