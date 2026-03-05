package com.entity_enums.centralised_enums;

public enum ClientTier {
    TIER_1_PLATINUM("Platinum", 1),
    TIER_2_GOLD("Gold", 2),
    TIER_3_SILVER("Silver", 3),
    TIER_4_BRONZE("Bronze", 4);

    private final String displayName;
    private final int level;

    ClientTier(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public boolean isLowerPriorityThan(ClientTier other) {
        return this.level > other.level;
    }

    public boolean isHigherPriorityThan(ClientTier other) {
        return this.level < other.level;
    }
}
