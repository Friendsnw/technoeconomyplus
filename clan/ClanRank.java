package com.techno.economy.clan;

public enum ClanRank {
    LEADER(5),
    CO_LEADER(4),
    OFFICER(3),
    MEMBER(2),
    RECRUIT(1);

    private final int weight;

    ClanRank(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isAtLeast(ClanRank other) {
        return other != null && this.weight >= other.weight;
    }

    public boolean isHigherThan(ClanRank other) {
        return other != null && this.weight > other.weight;
    }

    public ClanRank getPromoteTarget() {
        return switch (this) {
            case RECRUIT -> MEMBER;
            case MEMBER -> OFFICER;
            case OFFICER -> CO_LEADER;
            default -> null;
        };
    }

    public ClanRank getNextHigher() {
        return getPromoteTarget();
    }

    public ClanRank getDemoteTarget() {
        return switch (this) {
            case CO_LEADER -> OFFICER;
            case OFFICER -> MEMBER;
            case MEMBER -> RECRUIT;
            default -> null;
        };
    }

    public ClanRank getNextLower() {
        return getDemoteTarget();
    }

    public static ClanRank fromString(String str) {
        if (str == null || str.isBlank()) return RECRUIT;
        try {
            return ClanRank.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RECRUIT;
        }
    }
}
