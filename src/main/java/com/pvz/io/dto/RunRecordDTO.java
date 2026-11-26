package com.pvz.io.dto;

/**
 * The JSON shape of one record in {@code ~/.pvz/records.json}. Plain data, checked when
 * {@code RecordRepository} turns it into a {@code RunRecord}.
 */
public class RunRecordDTO {
    public Integer startingSun;
    /** null means endless (or a campaign level). */
    public Integer maxTurns;
    public Integer turnsReached;
    public Double survivalSeconds;
    /** "WON" or "LOST". */
    public String outcome;
    public Integer kills;
    public Integer drowned;
    public Integer score;
    /** ISO-8601 instant, for example "2026-11-27T10:15:30Z". */
    public String playedAt;
}
