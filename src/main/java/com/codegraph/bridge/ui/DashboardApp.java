package com.codegraph.bridge.ui;

import com.codegraph.bridge.service.CliProcessService;
import com.codegraph.bridge.service.WindowsStartupService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardApp {

        private static final String APP_NAME = "CodeGraph Agent";

        private static final String VERSION = "1.0.0";

        private static final int PORT = 9870;

        private final CliProcessService cliProcessService;
        private final Runnable exitAction;

        private Stage stage;

        private BorderPane root;

        private Label pageTitle;

        private Label agentStatus;

        private Label codeGraphStatus;

        private final WindowsStartupService startupService;

        private final ThemeManager themeManager = new ThemeManager();

        /*
         * Background executor for Windows registry operations.
         * This keeps the JavaFX Application Thread responsive and is
         * compatible with Java 17.
         */
        private final ExecutorService settingsExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "codegraph-settings");
                thread.setDaemon(true);
                return thread;
        });

        public DashboardApp(
                        CliProcessService cliProcessService,
                        WindowsStartupService startupService,
                        Runnable exitAction) {

                this.cliProcessService = cliProcessService;
                this.startupService = startupService;
                this.exitAction = exitAction;
        }

        public void show() {

                Platform.runLater(() -> {

                        if (stage == null) {

                                createStage();
                        }

                        stage.show();

                        stage.toFront();

                        stage.requestFocus();
                });
        }

        private void createStage() {

                stage = new Stage();

                stage.setTitle(
                                APP_NAME);

                stage.setWidth(950);

                stage.setHeight(620);

                stage.setMinWidth(850);

                stage.setMinHeight(550);

                /*
                 * Closing the dashboard should NOT
                 * terminate the Agent.
                 *
                 * It simply hides the window.
                 */
                stage.setOnCloseRequest(event -> {

                        event.consume();

                        stage.hide();
                });

                root = new BorderPane();

                root.setLeft(
                                createSideMenu());

                root.setCenter(
                                createDashboard());

                Scene scene = new Scene(root);

                scene.getStylesheets().add(
                                getClass().getResource("/dashboard.css").toExternalForm());

                themeManager.attach(scene);

                stage.setScene(scene);
        }

        private VBox createSideMenu() {

                VBox sidebar = new VBox();

                sidebar.getStyleClass()
                                .add(
                                                "sidebar");

                sidebar.setPrefWidth(
                                220);

                sidebar.setPadding(
                                new Insets(25, 15, 20, 15));

                /*
                 * Logo
                 */
                Label logo = new Label(
                                "◉  CodeGraph");

                logo.getStyleClass()
                                .add(
                                                "logo");

                sidebar.getChildren()
                                .add(
                                                logo);

                sidebar.getChildren()
                                .add(
                                                new Separator());

                /*
                 * Navigation
                 */
                Button dashboard = createMenuButton(
                                "⌂   Dashboard");

                Button settings = createMenuButton(
                                "⚙   Settings");

                Button updates = createMenuButton(
                                "↻   Check for Update");

                Button about = createMenuButton(
                                "ⓘ   About");

                dashboard.setOnAction(
                                event -> showDashboard());

                settings.setOnAction(
                                event -> showSettings());

                updates.setOnAction(
                                event -> showUpdates());

                about.setOnAction(
                                event -> showAbout());

                VBox navigation = new VBox(
                                5,
                                dashboard,
                                settings,
                                updates,
                                about);

                VBox.setMargin(
                                navigation,
                                new Insets(
                                                20,
                                                0,
                                                0,
                                                0));

                sidebar.getChildren()
                                .add(
                                                navigation);

                Region spacer = new Region();

                VBox.setVgrow(
                                spacer,
                                Priority.ALWAYS);

                sidebar.getChildren()
                                .add(
                                                spacer);

                /*
                 * Exit
                 */
                Button exit = createMenuButton(
                                "⏻   Exit");

                exit.getStyleClass()
                                .add(
                                                "exit-button");

                exit.setOnAction(
                                event -> exitAction.run());

                sidebar.getChildren()
                                .add(
                                                exit);

                return sidebar;
        }

        private Button createMenuButton(
                        String text) {

                Button button = new Button(text);

                button.setMaxWidth(
                                Double.MAX_VALUE);

                button.setAlignment(
                                Pos.CENTER_LEFT);

                button.setPadding(
                                new Insets(
                                                12,
                                                15,
                                                12,
                                                15));

                button.getStyleClass()
                                .add(
                                                "menu-button");

                return button;
        }

        private VBox createDashboard() {

                VBox container = new VBox();

                container.setSpacing(
                                25);

                container.setPadding(
                                new Insets(
                                                35));

                pageTitle = new Label(
                                "Dashboard");

                pageTitle.getStyleClass()
                                .add(
                                                "page-title");

                Label subtitle = new Label(
                                "CodeGraph Agent is running on this Windows machine.");

                subtitle.getStyleClass()
                                .add(
                                                "subtitle");

                HBox statusCards = new HBox(
                                20);

                statusCards.getChildren()
                                .add(
                                                createAgentCard());

                statusCards.getChildren()
                                .add(
                                                createCodeGraphCard());

                HBox systemCards = new HBox(
                                20);

                systemCards.getChildren()
                                .add(
                                                createInfoCard(
                                                                "Agent Version",
                                                                VERSION));

                systemCards.getChildren()
                                .add(
                                                createInfoCard(
                                                                "Port",
                                                                String.valueOf(PORT)));

                systemCards.getChildren()
                                .add(
                                                createInfoCard(
                                                                "Process ID",
                                                                String.valueOf(
                                                                                ProcessHandle
                                                                                                .current()
                                                                                                .pid())));

                container.getChildren()
                                .addAll(
                                                pageTitle,
                                                subtitle,
                                                statusCards,
                                                systemCards);

                return container;
        }

        private VBox createAgentCard() {

                VBox card = createCard();

                Label title = new Label(
                                "Agent");

                title.getStyleClass()
                                .add(
                                                "card-title");

                agentStatus = new Label(
                                "● Running");

                agentStatus.getStyleClass()
                                .add(
                                                "status-running");

                Label description = new Label(
                                "Local bridge is ready.");

                description.getStyleClass()
                                .add(
                                                "card-description");

                card.getChildren()
                                .addAll(
                                                title,
                                                agentStatus,
                                                description);

                return card;
        }

        private VBox createCodeGraphCard() {

                VBox card = createCard();

                Label title = new Label(
                                "CodeGraph CLI");

                title.getStyleClass()
                                .add(
                                                "card-title");

                codeGraphStatus = new Label(
                                "Checking...");

                codeGraphStatus.getStyleClass()
                                .add(
                                                "status-running");

                Label description = new Label(
                                "Checking local installation.");

                description.getStyleClass()
                                .add(
                                                "card-description");

                card.getChildren()
                                .addAll(
                                                title,
                                                codeGraphStatus,
                                                description);

                refreshCodeGraphStatus();

                return card;
        }

        private VBox createInfoCard(
                        String title,
                        String value) {

                VBox card = createCard();

                Label titleLabel = new Label(title);

                titleLabel.getStyleClass()
                                .add(
                                                "card-title");

                Label valueLabel = new Label(value);

                valueLabel.getStyleClass()
                                .add(
                                                "info-value");

                card.getChildren()
                                .addAll(
                                                titleLabel,
                                                valueLabel);

                return card;
        }

        private VBox createCard() {

                VBox card = new VBox(
                                10);

                card.setPrefWidth(
                                280);

                card.setPadding(
                                new Insets(
                                                20));

                card.getStyleClass()
                                .add(
                                                "card");

                return card;
        }

        private void showDashboard() {

                root.setCenter(
                                createDashboard());
        }

        private void showSettings() {

                VBox page = createPage(
                                "Settings",
                                "Configure CodeGraph Agent.");
                Label themeLabel = new Label("Appearance");
                themeLabel.getStyleClass().add("setting-label");

                ComboBox<ThemeManager.ThemeMode> themeSelector = new ComboBox<>();

                themeSelector.getItems().addAll(
                                ThemeManager.ThemeMode.LIGHT,
                                ThemeManager.ThemeMode.DARK,
                                ThemeManager.ThemeMode.SYSTEM);

                themeSelector.setValue(themeManager.getMode());
                themeSelector.setMaxWidth(250);

                themeSelector.setOnAction(event -> themeManager.setMode(themeSelector.getValue()));

                Label themeHint = new Label(
                                "System follows the Windows appearance setting.");
                themeHint.getStyleClass().add("setting-hint");

                CheckBox startup = new CheckBox(
                                "Start CodeGraph Agent with Windows");

                // Registry operations must not block the JavaFX Application Thread.
                startup.setDisable(true);

                startup.selectedProperty()
                                .addListener(
                                                (observable, oldValue, newValue) -> {

                                                        settingsExecutor.submit(() -> {

                                                                boolean success;

                                                                try {

                                                                        if (newValue) {
                                                                                success = startupService
                                                                                                .enableStartup();
                                                                        } else {
                                                                                success = startupService
                                                                                                .disableStartup();
                                                                        }

                                                                } catch (Exception e) {

                                                                        e.printStackTrace();
                                                                        success = false;
                                                                }

                                                                final boolean result = success;

                                                                Platform.runLater(() -> {

                                                                        if (!result) {

                                                                                startup.setDisable(false);

                                                                                // Avoid recursively firing the
                                                                                // listener.
                                                                                if (startup.isSelected() != oldValue) {
                                                                                        startup.setSelected(oldValue);
                                                                                }

                                                                                showInformation(
                                                                                                "Startup Settings",
                                                                                                "Unable to update Windows startup configuration.");

                                                                        } else {

                                                                                startup.setDisable(false);
                                                                        }
                                                                });
                                                        });
                                                });

                CheckBox minimized = new CheckBox(
                                "Start Agent minimized to system tray");

                minimized.setSelected(true);

                Label port = new Label(
                                "Agent Port");

                port.getStyleClass().add("agent-port");

                TextField portField = new TextField(
                                String.valueOf(PORT));

                portField.setMaxWidth(250);

                page.getChildren()
                                .addAll(
                                                themeLabel,
                                                themeSelector,
                                                themeHint,
                                                new Separator(),
                                                startup,
                                                minimized,
                                                port,
                                                portField);

                root.setCenter(page);

                /*
                 * Check Windows startup state asynchronously after the page
                 * has been displayed. This keeps the JavaFX UI responsive.
                 */
                settingsExecutor.submit(() -> {

                        boolean enabled;

                        try {

                                enabled = startupService.isStartupEnabled();

                        } catch (Exception e) {

                                e.printStackTrace();
                                enabled = false;
                        }

                        final boolean startupEnabled = enabled;

                        Platform.runLater(() -> {

                                startup.setSelected(startupEnabled);
                                startup.setDisable(false);
                        });
                });
        }

        private void showUpdates() {

                VBox page = createPage(
                                "Updates",
                                "Keep CodeGraph Agent up to date.");

                Label currentVersion = new Label(
                                "Current version: " + VERSION);

                currentVersion.getStyleClass().add("version-label");

                Button check = new Button(
                                "Check for Update");

                check.setOnAction(
                                event -> showInformation(
                                                "Update",
                                                "You are running the latest available version."));

                page.getChildren()
                                .addAll(
                                                currentVersion,
                                                check);

                root.setCenter(page);
        }

        private void showAbout() {

                VBox page = createPage(
                                "About",
                                "CodeGraph Agent");

                Label version = new Label(
                                "Version " + VERSION);

                version.getStyleClass()
                                .add("version-label");

                Label description = new Label(
                                "CodeGraph Agent provides a secure local bridge between the CodeGraph web application and Windows.");

                description.setWrapText(true);
                description.setMaxWidth(Double.MAX_VALUE);

                description.getStyleClass()
                                .add("about-description");

                page.getChildren()
                                .addAll(
                                                version,
                                                description);

                root.setCenter(page);
        }

        private VBox createPage(
                        String title,
                        String description) {

                VBox page = new VBox(
                                30);

                page.setPadding(
                                new Insets(
                                                35));

                Label titleLabel = new Label(title);

                titleLabel.getStyleClass()
                                .add(
                                                "page-title");

                Label descriptionLabel = new Label(description);

                descriptionLabel.getStyleClass()
                                .add(
                                                "subtitle");

                page.getChildren()
                                .addAll(
                                                titleLabel,
                                                descriptionLabel);

                return page;
        }

        private void refreshCodeGraphStatus() {

                try {

                        CliProcessService.CodeGraphInfo info = cliProcessService.detectInstallation();

                        if (info.installed()) {

                                codeGraphStatus.setText(
                                                "● Installed");

                        } else {

                                codeGraphStatus.setText(
                                                "○ Not Found");

                                codeGraphStatus
                                                .getStyleClass()
                                                .remove(
                                                                "status-running");

                                codeGraphStatus
                                                .getStyleClass()
                                                .add(
                                                                "status-warning");
                        }

                } catch (Exception e) {

                        codeGraphStatus.setText(
                                        "⚠ Error");
                }
        }

        public void hide() {

                Platform.runLater(() -> {

                        if (stage != null) {
                                stage.hide();
                        }
                });
        }

        private void showInformation(
                        String title,
                        String message) {

                Alert alert = new Alert(
                                Alert.AlertType.INFORMATION);

                alert.setTitle(
                                title);

                alert.setHeaderText(
                                null);

                alert.setContentText(
                                message);

                alert.showAndWait();
        }
}