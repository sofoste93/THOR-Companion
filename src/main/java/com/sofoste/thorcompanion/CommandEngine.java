package com.sofoste.thorcompanion;

import java.util.Locale;
import java.util.ResourceBundle;

/** Parses offline commands without depending on JavaFX, which keeps it easy to learn and test. */
public final class CommandEngine {
    public CommandResult execute(String rawInput, ResourceBundle text) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) return CommandResult.reply(text.getString("emptyCommand"));

        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.equals("/help")) return CommandResult.reply(text.getString("commandHelp"));
        if (lower.equals("/status")) {
            return new CommandResult(CommandResult.Action.SHOW_STATUS, text.getString("statusReply"), 0);
        }
        if (lower.startsWith("/note")) {
            String note = input.substring(Math.min(5, input.length())).trim();
            return note.isEmpty()
                    ? CommandResult.reply(text.getString("noteUsage"))
                    : new CommandResult(CommandResult.Action.SAVE_NOTE, note, 0);
        }
        if (lower.startsWith("/timer")) return parseTimer(input, text);
        if (lower.matches(".*\\b(hello|hi|bonjour|salut|hallo)\\b.*")) {
            return CommandResult.reply(text.getString("greetingReply"));
        }
        if (lower.contains("mission") || lower.contains("signal")) {
            return CommandResult.reply(text.getString("missionReply"));
        }
        return CommandResult.reply(text.getString("unknownReply"));
    }

    private CommandResult parseTimer(String input, ResourceBundle text) {
        String value = input.substring(Math.min(6, input.length())).trim();
        try {
            int minutes = Integer.parseInt(value);
            if (minutes < 1 || minutes > 180) throw new NumberFormatException();
            return new CommandResult(CommandResult.Action.START_TIMER, text.getString("timerAccepted"), minutes);
        } catch (NumberFormatException exception) {
            return CommandResult.reply(text.getString("timerUsage"));
        }
    }
}
