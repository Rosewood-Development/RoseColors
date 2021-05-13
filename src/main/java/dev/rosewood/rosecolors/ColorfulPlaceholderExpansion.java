package dev.rosewood.rosecolors;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class ColorfulPlaceholderExpansion extends PlaceholderExpansion {

    private final RoseColors plugin;

    public ColorfulPlaceholderExpansion(RoseColors plugin) {
        this.plugin = plugin;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        return HexUtils.colorify(params);
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return this.plugin.getName().toLowerCase();
    }

    @Override
    public String getAuthor() {
        return String.join(", ", this.plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

}
