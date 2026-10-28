package com.techno.economy.clan;

import java.util.UUID;

public class ClanMember {

    private final UUID playerUuid;
    private String playerName;
    private ClanRank rank;
    private final long joinedAt;

    public ClanMember(UUID playerUuid, String playerName, ClanRank rank, long joinedAt) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.rank = rank != null ? rank : ClanRank.RECRUIT;
        this.joinedAt = joinedAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public ClanRank getRank() {
        return rank;
    }

    public void setRank(ClanRank rank) {
        if (rank != null) {
            this.rank = rank;
        }
    }

    public long getJoinedAt() {
        return joinedAt;
    }
}
