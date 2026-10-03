package com.sofoste.thorcompanion;

public record CommandResult(Action action, String payload, int timerMinutes) {
    public enum Action { REPLY, SAVE_NOTE, START_TIMER, SHOW_STATUS }

    public static CommandResult reply(String message) {
        return new CommandResult(Action.REPLY, message, 0);
    }
}
