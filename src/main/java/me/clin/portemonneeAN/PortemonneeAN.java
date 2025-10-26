package me.clin.portemonneeAN;

import me.clin.portemonneeAN.command.PortemonneeCommand;
import me.clin.portemonneeAN.config.WalletSettings;
import me.clin.portemonneeAN.wallet.WalletListener;
import me.clin.portemonneeAN.wallet.WalletManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class PortemonneeAN extends JavaPlugin {

    private WalletSettings settings;
    private WalletManager walletManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadComponents();

        getServer().getPluginManager().registerEvents(new WalletListener(walletManager), this);

        PluginCommand command = Objects.requireNonNull(getCommand("portemonnee"), "Command portemonnee niet gevonden in plugin.yml");
        PortemonneeCommand executor = new PortemonneeCommand(walletManager);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadComponents();
    }

    private void loadComponents() {
        this.settings = WalletSettings.from(getConfig());
        this.walletManager = new WalletManager(this, settings);
    }

    public WalletManager getWalletManager() {
        return walletManager;
    }

    public WalletSettings getSettings() {
        return settings;
    }

}
