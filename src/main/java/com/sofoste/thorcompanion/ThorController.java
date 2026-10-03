package com.sofoste.thorcompanion;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public final class ThorController {
    private static final String BUNDLE = "com.sofoste.thorcompanion.Messages";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label clockLabel;
    @FXML private Label callsignLabel;
    @FXML private Label signalLabel;
    @FXML private Label systemTitle;
    @FXML private Label cpuLabel;
    @FXML private Label memoryLabel;
    @FXML private Label runtimeLabel;
    @FXML private Label cpuValue;
    @FXML private Label memoryValue;
    @FXML private Label runtimeValue;
    @FXML private Label companionTitle;
    @FXML private Label companionHint;
    @FXML private Label notesTitle;
    @FXML private Label notesHint;
    @FXML private Label notesArchiveTitle;
    @FXML private Label notesArchiveHint;
    @FXML private Label timerTitle;
    @FXML private Label timerValue;
    @FXML private Label journalTitle;
    @FXML private Label settingsTitle;
    @FXML private Label languageLabel;
    @FXML private Label callsignSettingLabel;
    @FXML private Label privacyTitle;
    @FXML private Label privacyText;
    @FXML private Label helpTitle;
    @FXML private Label helpIntro;
    @FXML private Label commandsTitle;
    @FXML private Label commandsText;
    @FXML private Label dataTitle;
    @FXML private Label dataPath;
    @FXML private Label aboutText;
    @FXML private Label footerStatus;
    @FXML private Label signalDot;
    @FXML private ProgressBar cpuBar;
    @FXML private ProgressBar memoryBar;
    @FXML private Tab bridgeTab;
    @FXML private Tab notesTab;
    @FXML private Tab journalTab;
    @FXML private Tab settingsTab;
    @FXML private Tab helpTab;
    @FXML private ListView<String> chatList;
    @FXML private ListView<String> notesList;
    @FXML private ListView<String> notesArchiveList;
    @FXML private ListView<String> journalList;
    @FXML private TextField commandInput;
    @FXML private TextField noteInput;
    @FXML private TextField callsignInput;
    @FXML private ChoiceBox<String> languageChoice;
    @FXML private CheckBox reducedMotionCheck;
    @FXML private Button sendButton;
    @FXML private Button addNoteButton;
    @FXML private Button cancelTimerButton;
    @FXML private Button saveSettingsButton;
    @FXML private Button openDataButton;

    private final CompanionStore store = new CompanionStore();
    private final CommandEngine commandEngine = new CommandEngine();
    private final TelemetryService telemetryService = new TelemetryService();
    private Timeline telemetryTimeline;
    private Timeline clockTimeline;
    private Timeline countdownTimeline;
    private FadeTransition signalPulse;
    private ResourceBundle text;
    private CompanionSettings settings;
    private int remainingSeconds;
    private TelemetryService.TelemetrySnapshot lastTelemetry;

    @FXML
    private void initialize() {
        languageChoice.getItems().setAll("English", "Français", "Deutsch");
        settings = store.loadSettings();
        callsignInput.setText(settings.callsign());
        reducedMotionCheck.setSelected(settings.reducedMotion());
        languageChoice.setValue(languageName(settings.language()));
        applyLanguage(settings.language());
        refreshLocalData();
        startClock();
        startTelemetry();
        configureSignalPulse();
        addThorMessage(text.getString("welcomeMessage"));
        commandInput.requestFocus();
    }

    @FXML
    private void sendCommand() {
        String input = commandInput.getText().strip();
        if (!input.isEmpty()) chatList.getItems().add("YOU  ›  " + input);
        CommandResult result = commandEngine.execute(input, text);
        commandInput.clear();

        switch (result.action()) {
            case REPLY -> addThorMessage(result.payload());
            case SAVE_NOTE -> saveNote(result.payload(), true);
            case START_TIMER -> startTimer(result.timerMinutes(), result.payload());
            case SHOW_STATUS -> addThorMessage(statusMessage(result.payload()));
        }
        scrollChat();
    }

    @FXML
    private void addNote() {
        String note = noteInput.getText().strip();
        if (note.isEmpty()) return;
        saveNote(note, false);
        noteInput.clear();
    }

    private void saveNote(String note, boolean fromCommand) {
        try {
            store.appendNote(note);
            store.appendJournal(text.getString("journalNoteSaved") + ": " + note);
            refreshLocalData();
            footerStatus.setText(text.getString("noteSavedStatus"));
            if (fromCommand) addThorMessage(text.getString("noteSavedReply"));
        } catch (IOException exception) {
            showError(text.getString("storageError"), exception.getMessage());
        }
    }

    private void startTimer(int minutes, String acceptedMessage) {
        if (countdownTimeline != null) countdownTimeline.stop();
        remainingSeconds = minutes * 60;
        updateTimerLabel();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateTimerLabel();
            if (remainingSeconds <= 0) {
                countdownTimeline.stop();
                addThorMessage(text.getString("timerComplete"));
                footerStatus.setText(text.getString("timerComplete"));
                tryJournal(text.getString("journalTimerComplete"));
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        addThorMessage(String.format(acceptedMessage, minutes));
        tryJournal(String.format(text.getString("journalTimerStarted"), minutes));
    }

    @FXML
    private void cancelTimer() {
        if (countdownTimeline != null) countdownTimeline.stop();
        remainingSeconds = 0;
        updateTimerLabel();
        footerStatus.setText(text.getString("timerCancelled"));
    }

    @FXML
    private void saveSettings() {
        String language = languageCode(languageChoice.getValue());
        settings = new CompanionSettings(language, callsignInput.getText(), reducedMotionCheck.isSelected());
        try {
            store.saveSettings(settings);
            store.appendJournal(text.getString("journalSettingsSaved"));
            applyLanguage(language);
            configureSignalPulse();
            refreshLocalData();
            footerStatus.setText(text.getString("settingsSaved"));
        } catch (IOException exception) {
            showError(text.getString("storageError"), exception.getMessage());
        }
    }

    @FXML
    private void openDataFolder() {
        try {
            Files.createDirectories(CompanionStore.defaultDirectory());
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop integration unavailable");
            Desktop.getDesktop().open(CompanionStore.defaultDirectory().toFile());
        } catch (IOException exception) {
            showError(text.getString("openFolderError"), CompanionStore.defaultDirectory().toString());
        }
    }

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.ZERO, event ->
                clockLabel.setText(LocalDateTime.now().format(CLOCK))), new KeyFrame(Duration.seconds(1)));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void startTelemetry() {
        telemetryTimeline = new Timeline(new KeyFrame(Duration.ZERO, event -> updateTelemetry()),
                new KeyFrame(Duration.seconds(1.5)));
        telemetryTimeline.setCycleCount(Timeline.INDEFINITE);
        telemetryTimeline.play();
    }

    private void updateTelemetry() {
        lastTelemetry = telemetryService.sample();
        cpuBar.setProgress(lastTelemetry.cpu());
        memoryBar.setProgress(lastTelemetry.memory());
        cpuValue.setText(percent(lastTelemetry.cpu()));
        memoryValue.setText(percent(lastTelemetry.memory()));
        long hours = lastTelemetry.uptime().toHours();
        long minutes = lastTelemetry.uptime().toMinutesPart();
        runtimeValue.setText("%02dh %02dm".formatted(hours, minutes));
    }

    private String statusMessage(String prefix) {
        if (lastTelemetry == null) updateTelemetry();
        return "%s CPU %s · RAM %s · %s %s".formatted(prefix, percent(lastTelemetry.cpu()),
                percent(lastTelemetry.memory()), text.getString("runtimeShort"), runtimeValue.getText());
    }

    private void configureSignalPulse() {
        if (signalPulse != null) signalPulse.stop();
        signalDot.setOpacity(1);
        if (settings.reducedMotion()) return;
        signalPulse = new FadeTransition(Duration.seconds(1.2), signalDot);
        signalPulse.setFromValue(1);
        signalPulse.setToValue(0.35);
        signalPulse.setAutoReverse(true);
        signalPulse.setCycleCount(Timeline.INDEFINITE);
        signalPulse.play();
    }

    private void applyLanguage(String language) {
        text = ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag(language));
        callsignLabel.setText(text.getString("callsignPrefix") + " " + normalizedCallsign());
        signalLabel.setText(text.getString("signalOnline"));
        systemTitle.setText(text.getString("systemTitle"));
        cpuLabel.setText(text.getString("cpu"));
        memoryLabel.setText(text.getString("memory"));
        runtimeLabel.setText(text.getString("runtime"));
        companionTitle.setText(text.getString("companionTitle"));
        companionHint.setText(text.getString("companionHint"));
        notesTitle.setText(text.getString("notesTitle"));
        notesHint.setText(text.getString("notesHint"));
        notesArchiveTitle.setText(text.getString("notesArchiveTitle"));
        notesArchiveHint.setText(text.getString("notesArchiveHint"));
        timerTitle.setText(text.getString("timerTitle"));
        journalTitle.setText(text.getString("journalTitle"));
        settingsTitle.setText(text.getString("settingsTitle"));
        languageLabel.setText(text.getString("language"));
        callsignSettingLabel.setText(text.getString("callsign"));
        privacyTitle.setText(text.getString("privacyTitle"));
        privacyText.setText(text.getString("privacyText"));
        helpTitle.setText(text.getString("helpTitle"));
        helpIntro.setText(text.getString("helpIntro"));
        commandsTitle.setText(text.getString("commandsTitle"));
        commandsText.setText(text.getString("commandsText"));
        dataTitle.setText(text.getString("dataTitle"));
        aboutText.setText(text.getString("aboutText"));
        bridgeTab.setText(text.getString("bridgeTab"));
        notesTab.setText(text.getString("notesTab"));
        journalTab.setText(text.getString("journalTab"));
        settingsTab.setText(text.getString("settingsTab"));
        helpTab.setText(text.getString("helpTab"));
        commandInput.setPromptText(text.getString("commandPrompt"));
        noteInput.setPromptText(text.getString("notePrompt"));
        sendButton.setText(text.getString("send"));
        addNoteButton.setText(text.getString("addNote"));
        cancelTimerButton.setText(text.getString("cancelTimer"));
        reducedMotionCheck.setText(text.getString("reducedMotion"));
        saveSettingsButton.setText(text.getString("saveSettings"));
        openDataButton.setText(text.getString("openData"));
        footerStatus.setText(text.getString("readyStatus"));
    }

    private void refreshLocalData() {
        notesList.getItems().setAll(store.readNotes());
        notesArchiveList.getItems().setAll(store.readNotes());
        journalList.getItems().setAll(store.readJournal());
        dataPath.setText(CompanionStore.defaultDirectory().toAbsolutePath().toString());
    }

    private void addThorMessage(String message) {
        chatList.getItems().add("THOR ›  " + message);
    }

    private void scrollChat() {
        if (!chatList.getItems().isEmpty()) chatList.scrollTo(chatList.getItems().size() - 1);
    }

    private void updateTimerLabel() {
        timerValue.setText("%02d:%02d".formatted(remainingSeconds / 60, remainingSeconds % 60));
    }

    private void tryJournal(String event) {
        try {
            store.appendJournal(event);
            refreshLocalData();
        } catch (IOException ignored) {
            footerStatus.setText(text.getString("storageError"));
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private String normalizedCallsign() {
        String value = callsignInput.getText() == null ? settings.callsign() : callsignInput.getText().strip();
        return value.isBlank() ? "Commander" : value;
    }

    private String languageName(String code) {
        return switch (code) { case "fr" -> "Français"; case "de" -> "Deutsch"; default -> "English"; };
    }

    private String languageCode(String name) {
        return switch (name) { case "Français" -> "fr"; case "Deutsch" -> "de"; default -> "en"; };
    }

    private String percent(double value) {
        return Math.round(value * 100) + "%";
    }

    void loadDemoData() {
        callsignLabel.setText(text.getString("callsignPrefix") + " ORBIT-7");
        chatList.getItems().setAll(
                "THOR ›  " + text.getString("welcomeMessage"),
                "YOU  ›  /status",
                "THOR ›  CPU 18% · RAM 42% · LINK STABLE",
                "YOU  ›  /timer 25",
                "THOR ›  " + String.format(text.getString("timerAccepted"), 25));
        notesList.getItems().setAll("Review mission checklist", "Send Neptune transmission");
        journalList.getItems().setAll("2026-10-03 20:26:04  Signal acquired", "2026-10-03 20:25:31  THOR online");
        remainingSeconds = 24 * 60 + 37;
        updateTimerLabel();
        cpuBar.setProgress(.18);
        memoryBar.setProgress(.42);
        cpuValue.setText("18%");
        memoryValue.setText("42%");
        runtimeValue.setText("02h 17m");
    }

    void shutdown() {
        if (telemetryTimeline != null) telemetryTimeline.stop();
        if (clockTimeline != null) clockTimeline.stop();
        if (countdownTimeline != null) countdownTimeline.stop();
        if (signalPulse != null) signalPulse.stop();
    }
}
