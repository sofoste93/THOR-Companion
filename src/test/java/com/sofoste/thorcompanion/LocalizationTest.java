package com.sofoste.thorcompanion;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LocalizationTest {
    @Test
    void allLanguagesContainTheSameNonEmptyKeys() {
        ResourceBundle english = bundle("en");
        for (String language : new String[]{"fr", "de"}) {
            ResourceBundle translated = bundle(language);
            assertEquals(english.keySet(), translated.keySet());
            english.keySet().forEach(key -> assertFalse(translated.getString(key).isBlank(), key));
        }
    }

    private ResourceBundle bundle(String language) {
        return ResourceBundle.getBundle("com.sofoste.thorcompanion.Messages", Locale.forLanguageTag(language));
    }
}
