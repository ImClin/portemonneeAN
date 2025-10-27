package me.clin.portemonneeAN;

import me.clin.portemonneeAN.command.PortemonneeCommand;
import me.clin.portemonneeAN.config.WalletSettings;
import me.clin.portemonneeAN.wallet.CashAdminMenu;
import me.clin.portemonneeAN.wallet.WalletDenomination;
import me.clin.portemonneeAN.wallet.WalletListener;
import me.clin.portemonneeAN.wallet.WalletManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class PortemonneeAN extends JavaPlugin {

    private WalletSettings settings;
    private WalletManager walletManager;
    private CashAdminMenu adminMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadComponents();

        this.adminMenu = new CashAdminMenu(this);
        getServer().getPluginManager().registerEvents(new WalletListener(walletManager), this);
        getServer().getPluginManager().registerEvents(adminMenu, this);

        PluginCommand command = Objects.requireNonNull(getCommand("portemonnee"), "Command portemonnee niet gevonden in plugin.yml");
        PortemonneeCommand executor = new PortemonneeCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadComponents();
    }

    private void loadComponents() {
        WalletSettings newSettings = WalletSettings.from(getConfig());
        Map<WalletDenomination, ItemStack> templates = loadCashTemplates();

        if (walletManager == null) {
            this.walletManager = new WalletManager(this, newSettings, templates);
        } else {
            this.walletManager.reload(newSettings, templates);
        }

        this.settings = newSettings;
    }

    private Map<WalletDenomination, ItemStack> loadCashTemplates() {
        Map<WalletDenomination, ItemStack> templates = new EnumMap<>(WalletDenomination.class);
        for (WalletDenomination denomination : WalletDenomination.values()) {
            ItemStack stack = getConfig().getItemStack(denomination.getConfigPath());
            if (stack != null) {
                templates.put(denomination, stack.clone());
            }
        }
        return templates;
    }

    public WalletManager getWalletManager() {
        return walletManager;
    }

    public WalletSettings getSettings() {
        return settings;
    }

    public CashAdminMenu getAdminMenu() {
        return adminMenu;
    }

}
