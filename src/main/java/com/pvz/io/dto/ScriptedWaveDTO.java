package com.pvz.io.dto;

import java.util.List;

/** The JSON shape of one scripted campaign wave. See {@link MapConfigDTO}. */
public class ScriptedWaveDTO {
    public boolean hugeWave;
    public List<SpawnDTO> spawns;

    /** One zombie in a wave. Wrapper types so a missing lane is not read as lane 0. */
    public static class SpawnDTO {
        public String zombieType;
        public Integer lane;
        /** Seconds from the start of the wave. */
        public Double spawnDelay;
    }
}
