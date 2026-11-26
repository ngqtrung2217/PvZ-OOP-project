package com.pvz.builder;

/** How a map sends zombies. */
public enum MapMode {
    /** Fixed scripted waves; won by clearing the last wave. */
    CAMPAIGN,
    /** Random escalating spawn from spawn_spec.json, for maxTurns turns or endless. */
    CUSTOM
}
