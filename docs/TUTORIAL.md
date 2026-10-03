# Learn Java with THOR

This short tour explains how one command travels through the application. It is
designed for learners who know basic Java syntax and want to see how a desktop
project separates responsibilities.

## 1. The view emits an event

In `thor-view.fxml`, the command field and **Transmit** button both call
`#sendCommand`. FXML keeps layout details out of Java while the controller owns
the behavior.

```xml
<TextField fx:id="commandInput" onAction="#sendCommand"/>
<Button onAction="#sendCommand"/>
```

## 2. The controller delegates parsing

`ThorController.sendCommand()` reads the text and passes it to `CommandEngine`.
The engine returns a small `CommandResult` instead of editing controls or files.
That makes the parser usable in a test without starting JavaFX.

```java
CommandResult result = commandEngine.execute(input, text);
```

The controller then handles the requested action: reply, save a note, start a
timer or report status.

## 3. The engine validates input

`CommandEngine` accepts `/timer 25`, but rejects missing, non-numeric and unsafe
values. The 1–180 minute rule lives in one place and has a focused automated
test.

Try adding a `/clear` command:

1. add a new action to `CommandResult.Action`;
2. recognize `/clear` in `CommandEngine.execute()`;
3. handle the action in `ThorController`;
4. add a unit test before changing the interface.

## 4. The store owns files

`CompanionStore` is the only class that decides where notes, settings and journal
entries live. Settings are written to a temporary file and moved into place, so
an interrupted write is less likely to leave a broken configuration.

Tests inject a temporary directory through the package-visible constructor.
They never touch the real user folder.

## 5. Translation is data

The three `Messages_*.properties` files share identical keys. `LocalizationTest`
checks that every translation is complete. To add a language, copy the English
file, translate its values and add the new language to the settings selector.

## 6. Build and inspect

Run:

```bash
mvn clean verify
```

Then open the reports in `target/surefire-reports` or launch the application
with `mvn javafx:run`. Build output belongs in `target` and `dist`; both are
ignored by Git.
