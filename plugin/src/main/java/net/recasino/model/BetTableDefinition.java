package net.recasino.model;

import org.bukkit.Material;

public final class BetTableDefinition {

    private final String id;
    private final String displayName;
    private final Material material;
    private final double maxBet;

    public BetTableDefinition(String id, String displayName, Material material, double maxBet) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.maxBet = maxBet;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public double getMaxBet() {
        return maxBet;
    }
}
