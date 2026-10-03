package com.sofoste.thorcompanion;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandEngineTest {
    private final CommandEngine engine = new CommandEngine();
    private final ResourceBundle text = ResourceBundle.getBundle(
            "com.sofoste.thorcompanion.Messages", Locale.ENGLISH);

    @Test
    void turnsNoteCommandIntoAStorageAction() {
        CommandResult result = engine.execute("/note Check oxygen", text);

        assertEquals(CommandResult.Action.SAVE_NOTE, result.action());
        assertEquals("Check oxygen", result.payload());
    }

    @Test
    void acceptsTimerInsideSafeRange() {
        CommandResult result = engine.execute("/timer 25", text);

        assertEquals(CommandResult.Action.START_TIMER, result.action());
        assertEquals(25, result.timerMinutes());
    }

    @Test
    void rejectsInvalidTimerInsteadOfStartingIt() {
        CommandResult result = engine.execute("/timer 900", text);

        assertEquals(CommandResult.Action.REPLY, result.action());
        assertTrue(result.payload().contains("1 to 180"));
    }

    @Test
    void respondsToNaturalGreetingOffline() {
        assertEquals(CommandResult.Action.REPLY, engine.execute("Bonjour THOR", text).action());
    }
}
