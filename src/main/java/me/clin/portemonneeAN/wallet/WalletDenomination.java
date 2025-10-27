package me.clin.portemonneeAN.wallet;

import org.bukkit.Material;

/**
 * Beschrijft de mogelijke coupures voor uitbetaling.
 */
public enum WalletDenomination {

    EURO_500(500, "500", Material.GHAST_TEAR),
    EURO_200(200, "200", Material.DIAMOND),
    EURO_100(100, "100", Material.REDSTONE),
    EURO_50(50, "50", Material.EMERALD),
    EURO_20(20, "20", Material.COAL),
    EURO_10(10, "10", Material.IRON_INGOT),
    EURO_5(5, "5", Material.QUARTZ),
    EURO_1(1, "1", Material.GOLD_INGOT);

    private final int value;
    private final String configKey;
    private final Material fallbackMaterial;

    WalletDenomination(int value, String configKey, Material fallbackMaterial) {
        this.value = value;
        this.configKey = configKey;
        this.fallbackMaterial = fallbackMaterial;
    }

    public int getValue() {
        return value;
    }

    public String getConfigPath() {
        return "cash-items." + configKey;
    }

    public Material getFallbackMaterial() {
        return fallbackMaterial;
    }
}
