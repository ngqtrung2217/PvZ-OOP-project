package com.pvz.io.dto;

import java.util.List;

/**
 * The JSON shape of a map file (README section 3.10), field for field.
 * <p>
 * A DTO is plain data with no rules, so Gson can fill it from any file. The values are
 * checked only when {@code MapSerializer} turns it into a {@code GameMap} through the
 * builder. Keeping this separate from {@code GameMap} also means renaming a field there
 * does not break maps players have already saved.
 * <p>
 * Wrapper types ({@code Integer}) are used where a missing value must be told apart from
 * zero.
 */
public class MapConfigDTO {
    public String name;
    /** "CUSTOM" or "CAMPAIGN"; written for readers, checked against the waves on load. */
    public String mode;
    /** Tile type names, [row][col]. */
    public String[][] tiles;
    public Integer startingSun;
    public List<String> allowedPlants;
    /** null or missing means endless. Campaign maps leave it out. */
    public Integer maxTurns;
    /** Campaign maps only; custom maps leave it out. */
    public List<ScriptedWaveDTO> waves;
}
