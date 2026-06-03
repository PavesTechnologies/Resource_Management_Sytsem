package com.entity_enums.centralised_enums;

public enum PriorityLevel {
    LOW(4, "Bronze"),
    MEDIUM(3, "Silver"), 
    HIGH(2, "Gold"),
    CRITICAL(1, "Platinum");

    private final int level;
    private final String displayName;

    PriorityLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }
}
