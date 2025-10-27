package me.clin.portemonneeAN.wallet;

import me.clin.portemonneeAN.config.WalletSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Core logica voor de portemonnee items.
 */
public final class WalletManager {

    private final Random random = new Random();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final DecimalFormat numberFormat;
    private WalletSettings settings;
    private final EnumMap<WalletDenomination, ItemStack> cashTemplates = new EnumMap<>(WalletDenomination.class);

    private final NamespacedKey walletKey;
    private final NamespacedKey amountKey;
    private final NamespacedKey revealedKey;
    private final NamespacedKey claimedKey;
    private final NamespacedKey uniqueKey;

    public WalletManager(Plugin plugin, WalletSettings settings, Map<WalletDenomination, ItemStack> templates) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("nl-NL"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        this.numberFormat = new DecimalFormat("#,##0", symbols);

        this.settings = settings;
        setCashTemplates(templates);

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

        List<ItemStack> cashItems = createCashStacks(amount);
        distributeCash(player, cashItems);

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
        };

        if (!Component.empty().equals(message)) {
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

    private List<ItemStack> createCashStacks(int amount) {
        Deque<ItemStack> stacks = new ArrayDeque<>();
        int remaining = amount;
        for (WalletDenomination denomination : WalletDenomination.values()) {
            while (remaining >= denomination.getValue()) {
                stacks.addLast(createCashItem(denomination));
                remaining -= denomination.getValue();
            }
        }
        return new ArrayList<>(stacks);
    }

    private ItemStack createCashItem(WalletDenomination denomination) {
        ItemStack template = cashTemplates.get(denomination);
        if (template != null) {
            return template.clone();
        }
        return createFallbackItem(denomination);
    }

    private ItemStack createFallbackItem(WalletDenomination denomination) {
        ItemStack item = new ItemStack(denomination.getFallbackMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("€" + denomination.getValue() + " biljet", NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("Configureer via /portemonnee admingui", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void reload(WalletSettings newSettings, Map<WalletDenomination, ItemStack> templates) {
        this.settings = newSettings;
        setCashTemplates(templates);
    }

    public void setCashTemplates(Map<WalletDenomination, ItemStack> templates) {
        cashTemplates.clear();
        if (templates == null || templates.isEmpty()) {
            return;
        }
        for (Map.Entry<WalletDenomination, ItemStack> entry : templates.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            cashTemplates.put(entry.getKey(), entry.getValue().clone());
        }
    }

    public ItemStack getCashTemplate(WalletDenomination denomination) {
        ItemStack template = cashTemplates.get(denomination);
        return template == null ? null : template.clone();
    }

    private void distributeCash(Player player, List<ItemStack> cashItems) {
        if (cashItems.isEmpty()) {
            return;
        }
        ItemStack[] array = cashItems.toArray(new ItemStack[0]);
        var leftover = player.getInventory().addItem(array);
        if (!leftover.isEmpty()) {
            World world = player.getWorld();
            Location location = player.getLocation();
            for (ItemStack stack : leftover.values()) {
                world.dropItemNaturally(location, stack);
            }
        }
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

    public record ClaimResult(Status status, int amount, String formattedAmount) {
        enum Status {
            SUCCESS,
            NOT_WALLET,
            NOT_REVEALED,
            ALREADY_CLAIMED
        }

        static ClaimResult claimed(int amount, String formattedAmount) {
            return new ClaimResult(Status.SUCCESS, amount, formattedAmount);
        }

        static ClaimResult notWallet() {
            return new ClaimResult(Status.NOT_WALLET, 0, "");
        }

        static ClaimResult notRevealed() {
            return new ClaimResult(Status.NOT_REVEALED, 0, "");
        }

        static ClaimResult alreadyClaimed() {
            return new ClaimResult(Status.ALREADY_CLAIMED, 0, "");
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public Status getStatus() {
            return status;
        }
    }
}