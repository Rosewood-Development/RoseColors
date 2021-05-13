package dev.rosewood.rosecolors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class RoseColorsBungee extends Plugin implements Listener {

    private List<String> motds;
    private int motdIndex;
    private long motdDuration;
    private ScheduledTask motdTask;

    @Override
    public void onEnable() {
        this.reloadConfig();
        this.getProxy().getPluginManager().registerCommand(this, new BungeeReloadCommand());
        this.getProxy().getPluginManager().registerListener(this, this);
    }

    private void reloadConfig() {
        if (!this.getDataFolder().exists())
            this.getDataFolder().mkdirs();

        File file = new File(this.getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream inputStream = this.getResourceAsStream("bungee-config.yml")) {
                Files.copy(inputStream, file.toPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            this.motds = config.getStringList("motds").stream().map(x -> x.replaceAll(Pattern.quote("%n"), "\n")).collect(Collectors.toList());
            this.motdIndex = config.getBoolean("change-motd") ? 0 : -1;
            this.motdDuration = config.getLong("motd-duration");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.motdTask != null)
            this.motdTask.cancel();

        if (!this.motds.isEmpty() && this.motdIndex != -1) {
            this.motdTask = this.getProxy().getScheduler().schedule(this, () -> {
                this.motdIndex = (this.motdIndex + 1) % this.motds.size();
            }, this.motdDuration, this.motdDuration, TimeUnit.MILLISECONDS);
        }
    }

    @EventHandler
    public void onServerListPing(ProxyPingEvent event) {
        if (this.motdIndex != -1) {
            ServerPing response = event.getResponse();
            response.setDescription(HexUtils.colorify(this.motds.get(this.motdIndex)));
        }
    }

    private class BungeeReloadCommand extends Command {

        public BungeeReloadCommand() {
            super("rosecolorsreload", "rosecolors.use");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            RoseColorsBungee.this.reloadConfig();
            sender.sendMessage(HexUtils.colorify("&7[<g:#8A2387:#E94057:#F27121>Rose<rainbow>Colors&7] <r:0.25>Reloaded the config.yml on the proxy."));
        }

    }

}
