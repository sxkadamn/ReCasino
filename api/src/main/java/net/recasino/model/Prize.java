package net.recasino.model;

import org.bukkit.Material;

public final class Prize {

    private final String key;
    private final String displayName;
    private final Material material;
    private final int weight;
    private final double multiplier;

    public Prize(String key, String displayName, Material material, int weight, double multiplier) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.weight = weight;
        this.multiplier = multiplier;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getWeight() {
        return weight;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
