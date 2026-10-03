package com.sofoste.thorcompanion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionStoreTest {
    @TempDir Path temporaryDirectory;

    @Test
    void settingsSurviveAReload() throws Exception {
        CompanionStore store = new CompanionStore(temporaryDirectory);
        store.saveSettings(new CompanionSettings("fr", "ORBIT-7", true));

        assertEquals(new CompanionSettings("fr", "ORBIT-7", true), store.loadSettings());
    }

    @Test
    void newestNotesAppearFirst() throws Exception {
        CompanionStore store = new CompanionStore(temporaryDirectory);
        store.appendNote("First transmission");
        store.appendNote("Second transmission");

        assertEquals("Second transmission", store.readNotes().get(0));
        assertEquals(2, store.readNotes().size());
    }

    @Test
    void journalIncludesTheEvent() throws Exception {
        CompanionStore store = new CompanionStore(temporaryDirectory);
        store.appendJournal("Signal acquired");

        assertTrue(store.readJournal().get(0).endsWith("Signal acquired"));
    }
}
