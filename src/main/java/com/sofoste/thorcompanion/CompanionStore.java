package com.sofoste.thorcompanion;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Stores only local text files and replaces settings atomically to avoid partial writes. */
public final class CompanionStore {
    private static final DateTimeFormatter JOURNAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path directory;

    public CompanionStore() {
        this(defaultDirectory());
    }

    CompanionStore(Path directory) {
        this.directory = directory;
    }

    public static Path defaultDirectory() {
        return Path.of(System.getProperty("user.home"), ".thor-companion");
    }

    public CompanionSettings loadSettings() {
        Path file = directory.resolve("settings.properties");
        if (!Files.exists(file)) return CompanionSettings.defaults();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return new CompanionSettings(
                    supportedLanguage(properties.getProperty("language", "en")),
                    properties.getProperty("callsign", "Commander").strip(),
                    Boolean.parseBoolean(properties.getProperty("reducedMotion", "false")));
        } catch (IOException exception) {
            return CompanionSettings.defaults();
        }
    }

    public void saveSettings(CompanionSettings settings) throws IOException {
        Files.createDirectories(directory);
        Properties properties = new Properties();
        properties.setProperty("language", supportedLanguage(settings.language()));
        properties.setProperty("callsign", settings.callsign().isBlank() ? "Commander" : settings.callsign().strip());
        properties.setProperty("reducedMotion", Boolean.toString(settings.reducedMotion()));
        Path temporary = Files.createTempFile(directory, "settings-", ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            properties.store(writer, "THOR Companion local settings");
        }
        Path destination = directory.resolve("settings.properties");
        try {
            Files.move(temporary, destination,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void appendNote(String note) throws IOException {
        appendLine("notes.txt", note.strip());
    }

    public void appendJournal(String event) throws IOException {
        appendLine("journal.log", JOURNAL_TIME.format(LocalDateTime.now()) + "  " + event.strip());
    }

    public List<String> readNotes() {
        return readLines("notes.txt", 100);
    }

    public List<String> readJournal() {
        return readLines("journal.log", 100);
    }

    private void appendLine(String filename, String line) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(filename), line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private List<String> readLines(String filename, int limit) {
        Path file = directory.resolve(filename);
        if (!Files.exists(file)) return List.of();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Collections.reverse(lines);
            return lines.stream().limit(limit).toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private String supportedLanguage(String language) {
        return switch (language) {
            case "fr", "de" -> language;
            default -> "en";
        };
    }
}
