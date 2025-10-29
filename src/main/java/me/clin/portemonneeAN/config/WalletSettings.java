package me.clin.portemonneeAN.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of the portemonnee configuratie.
 */
public final class WalletSettings {

    private final int minAmount;
    private final int maxAmount;
    private final Material baseMaterial;
    private final String itemsAdderItemId;
    private final String displayName;
    private final List<String> unrevealedLore;
    private final List<String> revealedLore;
    private final String revealMessage;
    private final String readyToClaimMessage;
    private final String claimedMessage;
    private final String claimFailedMessage;
    private final String commandFailedMessage;
    private final String alreadyClaimedMessage;
    private final String notRevealedMessage;

    private WalletSettings(
            int minAmount,
            int maxAmount,
            Material baseMaterial,
            String itemsAdderItemId,
            String displayName,
            List<String> unrevealedLore,
            List<String> revealedLore,
            String revealMessage,
            String readyToClaimMessage,
            String claimedMessage,
            String claimFailedMessage,
            String commandFailedMessage,
            String alreadyClaimedMessage,
            String notRevealedMessage
    ) {
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.baseMaterial = baseMaterial;
        this.itemsAdderItemId = itemsAdderItemId;
        this.displayName = displayName;
        this.unrevealedLore = unrevealedLore;
        this.revealedLore = revealedLore;
        this.revealMessage = revealMessage;
        this.readyToClaimMessage = readyToClaimMessage;
        this.claimedMessage = claimedMessage;
        this.claimFailedMessage = claimFailedMessage;
        this.commandFailedMessage = commandFailedMessage;
        this.alreadyClaimedMessage = alreadyClaimedMessage;
        this.notRevealedMessage = notRevealedMessage;
    }

    public static WalletSettings from(FileConfiguration config) {
        int min = config.getInt("wallet.min-amount", 1);
        int max = config.getInt("wallet.max-amount", 1000);

        if (min < 0) {
            min = 0;
        }
        if (max < min) {
            max = min;
        }

        Material baseMaterial = parseMaterial(config.getString("wallet.base.material", "PAPER"));
        String itemsAdderItemId = config.getString("wallet.base.itemsadder-id", "");
        if (itemsAdderItemId != null) {
            itemsAdderItemId = itemsAdderItemId.trim();
        }

        String displayName = color(config.getString("wallet.display-name", "&6Portemonnee"));

        List<String> unrevealedLore = color(config.getStringList("wallet.unrevealed-lore"));
        List<String> revealedLore = color(config.getStringList("wallet.revealed-lore"));

        String reveal = color(config.getString("messages.reveal", "&9Je bekijkt de portemonnee en ziet &e€{amount}&9."));
        String ready = color(config.getString("messages.ready-to-claim", "&7Klik nogmaals met de portemonnee om het geld te claimen."));
        String claimed = color(config.getString("messages.claimed", "&aJe hebt &e€{amount}&a geclaimd uit de portemonnee."));
        String failed = color(config.getString("messages.claim-failed", "&cHet is op dit moment niet gelukt om te claimen. Neem contact op met een administrator."));
        String commandFailed = color(config.getString("messages.command-failed", "&cHet uitvoeren van de uitbetalingscommand is mislukt."));
        String alreadyClaimed = color(config.getString("messages.already-claimed", "&cDeze portemonnee is al geclaimd."));
        String notRevealed = color(config.getString("messages.not-revealed", "&cJe moet eerst het bedrag bekijken voordat je kunt claimen."));

        return new WalletSettings(
                min,
                max,
                baseMaterial,
                itemsAdderItemId,
                displayName,
                unrevealedLore,
                revealedLore,
                reveal,
                ready,
                claimed,
                failed,
                commandFailed,
                alreadyClaimed,
                notRevealed
        );
    }

    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-orx])", Pattern.CASE_INSENSITIVE);

    private static Material parseMaterial(String name) {
        if (name == null || name.isBlank()) {
            return Material.PAPER;
        }
        Material material = Material.matchMaterial(name.trim(), true);
        return material != null ? material : Material.PAPER;
    }

    private static String color(String input) {
        String text = Objects.requireNonNullElse(input, "");
        if (text.isEmpty()) {
            return "";
        }

        Matcher matcher = COLOR_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "§" + matcher.group(1));
        }
        matcher.appendTail(buffer);
        return translateHex(buffer.toString());
    }

    private static List<String> color(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> coloured = new ArrayList<>(list.size());
        for (String entry : list) {
            coloured.add(color(entry));
        }
        return Collections.unmodifiableList(coloured);
    }

    private static String translateHex(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '&' && i + 7 < chars.length && chars[i + 1] == '#') {
                String hex = new String(chars, i + 2, 6);
                if (hex.matches("[0-9A-Fa-f]{6}")) {
                    builder.append('§').append('x');
                    for (char hexChar : hex.toCharArray()) {
                        builder.append('§').append(Character.toLowerCase(hexChar));
                    }
                    i += 7;
                    continue;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public Material getBaseMaterial() {
        return baseMaterial;
    }

    public String getItemsAdderItemId() {
        return itemsAdderItemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRevealedLore(String formattedAmount) {
        if (revealedLore.isEmpty()) {
            return revealedLore;
        }
        return revealedLore.stream()
                .map(line -> line.replace("{amount}", formattedAmount))
                .collect(Collectors.toList());
    }

    public List<String> getUnrevealedLore() {
        return unrevealedLore;
    }

    public String getRevealMessage(String formattedAmount) {
        return revealMessage.replace("{amount}", formattedAmount);
    }

    public String getReadyToClaimMessage() {
        return readyToClaimMessage;
    }

    public String getClaimedMessage(String formattedAmount) {
        return claimedMessage.replace("{amount}", formattedAmount);
    }

    public String getClaimFailedMessage() {
        return claimFailedMessage;
    }

    public String getCommandFailedMessage() {
        return commandFailedMessage;
    }

    public String getAlreadyClaimedMessage() {
        return alreadyClaimedMessage;
    }

    public String getNotRevealedMessage() {
        return notRevealedMessage;
    }
}