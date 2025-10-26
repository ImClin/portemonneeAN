package me.clin.portemonneeAN;

import me.clin.portemonneeAN.command.PortemonneeCommand;
import me.clin.portemonneeAN.config.WalletSettings;
import me.clin.portemonneeAN.wallet.WalletListener;
import me.clin.portemonneeAN.wallet.WalletManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class PortemonneeAN extends JavaPlugin {

    private WalletSettings settings;
    private Economy economy;
    private WalletManager walletManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Er is geen Vault-economie gevonden. PortemonneeAN wordt uitgeschakeld.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadComponents();

        getServer().getPluginManager().registerEvents(new WalletListener(walletManager), this);

        PluginCommand command = Objects.requireNonNull(getCommand("portemonnee"), "Command portemonnee niet gevonden in plugin.yml");
        PortemonneeCommand executor = new PortemonneeCommand(walletManager);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (economy == null) {
            getLogger().warning("Economy niet beschikbaar; laad de plugin opnieuw wanneer Vault beschikbaar is.");
            return;
        }
        loadComponents();
    }

    private void loadComponents() {
        this.settings = WalletSettings.from(getConfig());
        this.walletManager = new WalletManager(this, settings, economy);
    }

    public WalletManager getWalletManager() {
        return walletManager;
    }

    public WalletSettings getSettings() {
        return settings;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return false;
        }

        this.economy = registration.getProvider();
        return this.economy != null;
    }
}
