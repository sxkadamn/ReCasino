package net.recasino.model;

public enum CurrencyType {
    MONEY("Монеты"),
    RILLIK("Риллики");

    private final String displayName;

    CurrencyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CurrencyType fromName(String name) {
        if (name == null) {
            return MONEY;
        }

        for (CurrencyType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return MONEY;
    }
}
