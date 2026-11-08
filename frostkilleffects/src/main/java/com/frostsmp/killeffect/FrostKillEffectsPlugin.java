package com.frostsmp.killeffect;

import com.frostsmp.killeffect.command.KillEffectCommand;
import com.frostsmp.killeffect.command.SafeCommandExecutor;
import com.frostsmp.killeffect.config.KillEffectConfig;
import com.frostsmp.killeffect.listener.ClaimListener;
import com.frostsmp.killeffect.listener.KillEffectListener;
import com.frostsmp.killeffect.listener.SelectGuiListener;
import com.frostsmp.killeffect.service.KillEffectService;
import com.frostsmp.killeffect.storage.PlayerEffectStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostKillEffectsPlugin extends JavaPlugin {

    private KillEffectConfig effectConfig;
    private PlayerEffectStorage storage;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        effectConfig = new KillEffectConfig(this);
        com.frostsmp.killeffect.item.KillEffectItem.init(this);
        effectConfig.load();

        storage = new PlayerEffectStorage(this);
        storage.load();

        KillEffectService effects = new KillEffectService(effectConfig, storage);
        getServer().getPluginManager().registerEvents(new ClaimListener(this, effectConfig, storage), this);
        getServer().getPluginManager().registerEvents(new KillEffectListener(effects), this);
        getServer().getPluginManager().registerEvents(new SelectGuiListener(effectConfig, storage), this);

        KillEffectCommand command = new KillEffectCommand(this, effectConfig, storage);
        PluginCommand killeffect = getCommand("killeffect");
        if (killeffect != null) {
            killeffect.setExecutor(new SafeCommandExecutor(this, command));
            killeffect.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'killeffect' missing from plugin.yml!");
        }

        getLogger().info("FrostKillEffects enabled (" + effectConfig.effects().size() + " effects).");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.save();
        }
    }

    public void reloadAll() {
        if (storage != null) {
            storage.save();
        }
        effectConfig.load();
        storage.load();
    }
}
