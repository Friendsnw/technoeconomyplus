package com.techno.economy.clan;

import java.util.UUID;

public class ClanWar {

    public enum WarStatus {
        PENDING,
        ACTIVE,
        ENDED
    }

    private final String warId;
    private final String challengerClan;
    private final String targetClan;
    private final long startTime;
    private final long endTime;
    private int challengerKills;
    private int targetKills;
    private String winnerClan;
    private WarStatus status;

    public ClanWar(String warId, String challengerClan, String targetClan, long startTime, long endTime, int challengerKills, int targetKills, String winnerClan, WarStatus status) {
        this.warId = warId != null ? warId : UUID.randomUUID().toString();
        this.challengerClan = challengerClan;
        this.targetClan = targetClan;
        this.startTime = startTime;
        this.endTime = endTime;
        this.challengerKills = challengerKills;
        this.targetKills = targetKills;
        this.winnerClan = winnerClan;
        this.status = status != null ? status : WarStatus.PENDING;
    }

    public ClanWar(String challengerClan, String targetClan, long durationMillis) {
        this(UUID.randomUUID().toString(), challengerClan, targetClan, System.currentTimeMillis(), System.currentTimeMillis() + durationMillis, 0, 0, null, WarStatus.ACTIVE);
    }

    public String getWarId() {
        return warId;
    }

    public String getChallengerClan() {
        return challengerClan;
    }

    public String getTargetClan() {
        return targetClan;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getChallengerKills() {
        return challengerKills;
    }

    public void addChallengerKill() {
        this.challengerKills++;
    }

    public int getTargetKills() {
        return targetKills;
    }

    public void addTargetKill() {
        this.targetKills++;
    }

    public String getWinnerClan() {
        return winnerClan;
    }

    public void setWinnerClan(String winnerClan) {
        this.winnerClan = winnerClan;
    }

    public WarStatus getStatus() {
        return status;
    }

    public void setStatus(WarStatus status) {
        this.status = status;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    public boolean involves(String clanName) {
        return challengerClan.equalsIgnoreCase(clanName) || targetClan.equalsIgnoreCase(clanName);
    }

    public String getOpponent(String clanName) {
        if (challengerClan.equalsIgnoreCase(clanName)) return targetClan;
        if (targetClan.equalsIgnoreCase(clanName)) return challengerClan;
        return null;
    }
}
