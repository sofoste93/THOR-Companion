package com.sofoste.thorcompanion;

public record CompanionSettings(String language, String callsign, boolean reducedMotion) {
    public static CompanionSettings defaults() {
        return new CompanionSettings("en", "Commander", false);
    }
}
