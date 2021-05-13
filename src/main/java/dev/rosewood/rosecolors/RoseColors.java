package dev.rosewood.rosecolors;

import dev.rosewood.rosecolors.HexUtils.Gradient;
import dev.rosewood.rosecolors.HexUtils.Rainbow;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * @author Esophose
 */
public class RoseColors extends JavaPlugin implements Listener {

    private static final Pattern HEX = Pattern.compile("#([A-Fa-f0-9]){6}");
    private static final Pattern SAT_LIG = Pattern.compile("(\\d*\\.?\\d+)(:\\d*\\.?\\d+)?");

    private List<String> motds;
    private int motdIndex;
    private long motdDuration;
    private BukkitTask motdTask;

    @Override
    public void onEnable() {
        this.reload();

        PluginCommand gradientCommand = this.getCommand("gradient");
        if (gradientCommand != null)
            gradientCommand.setExecutor(this);

        PluginCommand rainbowCommand = this.getCommand("rainbow");
        if (rainbowCommand != null)
            rainbowCommand.setExecutor(this);

        PluginCommand parseColorsCommand = this.getCommand("parsecolors");
        if (parseColorsCommand != null)
            parseColorsCommand.setExecutor(this);

        PluginCommand reloadCommand = this.getCommand("rosecolorsreload");
        if (reloadCommand != null)
            reloadCommand.setExecutor(this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            new ColorfulPlaceholderExpansion(this).register();

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void reload() {
        this.saveDefaultConfig();
        this.reloadConfig();

        this.motds = this.getConfig().getStringList("motds").stream().map(x -> x.replaceAll(Pattern.quote("%n"), "\n")).collect(Collectors.toList());
        this.motdIndex = this.getConfig().getBoolean("change-motd") ? 0 : -1;
        this.motdDuration = this.getConfig().getLong("motd-duration");

        if (this.motdTask != null)
            this.motdTask.cancel();

        if (!this.motds.isEmpty() && this.motdIndex != -1) {
            this.motdTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                this.motdIndex = (this.motdIndex + 1) % this.motds.size();
            }, this.motdDuration, this.motdDuration);
        }
    }

    @EventHandler
    public void onMotd(ServerListPingEvent event) {
        if (this.motdIndex != -1)
            event.setMotd(HexUtils.colorify(this.motds.get(this.motdIndex)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rosecolors.use")) {
            HexUtils.sendMessage(sender, this.getMessage("no-permission"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("gradient")) {
            if (args.length < 2) {
                HexUtils.sendMessage(sender, this.getMessage("gradient-usage"));
                return true;
            }

            String[] split = args[0].split(":");
            if (split.length < 2) {
                HexUtils.sendMessage(sender, this.getMessage("gradient-min"));
                return true;
            }

            for (String hex : split) {
                Matcher matcher = HEX.matcher(hex);
                if (!matcher.find() || matcher.group().length() != hex.length()) {
                    HexUtils.sendMessage(sender, this.getMessage("gradient-invalid"));
                    return true;
                }
            }

            String value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            // Send hex codes
            List<Color> hexCodes = Arrays.stream(split).map(Color::decode).collect(Collectors.toList());
            Gradient gradient = new Gradient(hexCodes, value.length());

            StringBuilder hexMessage = new StringBuilder();
            for (char c : value.toCharArray()) {
                Color color = gradient.next();
                if (Character.isWhitespace(c))
                    continue;

                String hex = '#' + Integer.toHexString(color.getRGB()).substring(2);
                hexMessage.append(hex).append('#').append("&r").append(hex).append(hex.substring(1)).append(' ');
            }

            HexUtils.sendMessage(sender, "<g:" + args[0] + ">" + value);
            HexUtils.sendMessage(sender, hexMessage.toString());
        } else if (command.getName().equalsIgnoreCase("rainbow")) {
            if (args.length < 1) {
                HexUtils.sendMessage(sender, this.getMessage("rainbow-usage"));
                return true;
            }

            float saturation = 1.0F;
            float lightness = 1.0F;

            String value;
            String paramValues = null;
            if (SAT_LIG.matcher(args[0]).matches()) {
                try {
                    paramValues = args[0];
                    String[] params;
                    if (paramValues.contains(":")) {
                        params = paramValues.split(":");
                    } else {
                        params = new String[] { paramValues };
                    }

                    saturation = Float.parseFloat(params[0]);
                    if (params.length > 1)
                        lightness = Float.parseFloat(params[1]);
                } catch (NumberFormatException ex) {
                    HexUtils.sendMessage(sender, this.getMessage("rainbow-usage"));
                    return true;
                }

                value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            } else {
                value = String.join(" ", args);
            }

            // Send hex codes
            Rainbow rainbow = new Rainbow(value.length(), saturation, lightness);

            StringBuilder hexMessage = new StringBuilder();
            for (char c : value.toCharArray()) {
                Color color = rainbow.next();
                if (Character.isWhitespace(c))
                    continue;

                String hex = '#' + Integer.toHexString(color.getRGB()).substring(2);
                hexMessage.append(hex).append('#').append("&r").append(hex).append(hex.substring(1)).append(' ');
            }

            if (paramValues != null) {
                HexUtils.sendMessage(sender, "<r:" + paramValues + ">" + value);
            } else {
                HexUtils.sendMessage(sender, "<r>" + value);
            }

            HexUtils.sendMessage(sender, hexMessage.toString());
        } else if (command.getName().equalsIgnoreCase("parsecolors")) {
            HexUtils.sendMessage(sender, String.join(" ", args));
        } else if (command.getName().equalsIgnoreCase("rosecolorsreload")) {
            this.reload();
            HexUtils.sendMessage(sender, this.getMessage("reloaded"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    public String getMessage(String key) {
        return this.getConfig().getString("prefix") + this.getConfig().getString(key);
    }

}
