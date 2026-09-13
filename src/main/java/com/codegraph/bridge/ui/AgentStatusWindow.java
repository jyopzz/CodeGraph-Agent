package com.codegraph.bridge.ui;

import com.codegraph.bridge.service.CliProcessService;

import javax.swing.*;
import java.awt.*;

public class AgentStatusWindow {

        private final CliProcessService cliProcessService;

        private JFrame frame;

        private JLabel agentStatusLabel;
        private JLabel versionLabel;
        private JLabel portLabel;
        private JLabel pidLabel;

        private JLabel codeGraphStatusLabel;
        private JLabel codeGraphVersionLabel;
        private JLabel codeGraphPathLabel;

        public AgentStatusWindow(
                        CliProcessService cliProcessService) {
                this.cliProcessService = cliProcessService;

                createWindow();
        }

        private void createWindow() {

                frame = new JFrame("CodeGraph Agent");

                frame.setDefaultCloseOperation(
                                WindowConstants.DISPOSE_ON_CLOSE);

                frame.setSize(480, 420);

                frame.setLocationRelativeTo(null);

                frame.setResizable(false);

                JPanel root = new JPanel(new BorderLayout());

                root.setBorder(
                                BorderFactory.createEmptyBorder(
                                                20,
                                                20,
                                                20,
                                                20));

                root.add(
                                createHeader(),
                                BorderLayout.NORTH);

                root.add(
                                createContent(),
                                BorderLayout.CENTER);

                root.add(
                                createFooter(),
                                BorderLayout.SOUTH);

                frame.setContentPane(root);
        }

        private JPanel createHeader() {

                JPanel panel = new JPanel(new BorderLayout());

                JLabel title = new JLabel("CodeGraph Agent");

                title.setFont(
                                new Font(
                                                "Segoe UI",
                                                Font.BOLD,
                                                22));

                JLabel subtitle = new JLabel(
                                "Windows background service");

                subtitle.setFont(
                                new Font(
                                                "Segoe UI",
                                                Font.PLAIN,
                                                13));

                panel.add(
                                title,
                                BorderLayout.NORTH);

                panel.add(
                                subtitle,
                                BorderLayout.SOUTH);

                return panel;
        }

        private JPanel createContent() {

                JPanel content = new JPanel();

                content.setLayout(
                                new BoxLayout(
                                                content,
                                                BoxLayout.Y_AXIS));

                content.setBorder(
                                BorderFactory.createEmptyBorder(
                                                25,
                                                0,
                                                15,
                                                0));

                /*
                 * Agent
                 */

                content.add(
                                createSectionTitle("Agent"));

                agentStatusLabel = createValueLabel("● Running");

                versionLabel = createValueLabel("1.0.0");

                portLabel = createValueLabel("9870");

                pidLabel = createValueLabel(
                                String.valueOf(
                                                ProcessHandle.current().pid()));

                content.add(
                                createRow(
                                                "Status",
                                                agentStatusLabel));

                content.add(
                                createRow(
                                                "Version",
                                                versionLabel));

                content.add(
                                createRow(
                                                "Port",
                                                portLabel));

                content.add(
                                createRow(
                                                "Process ID",
                                                pidLabel));

                content.add(
                                Box.createVerticalStrut(20));

                /*
                 * CodeGraph
                 */

                content.add(
                                createSectionTitle("CodeGraph CLI"));

                codeGraphStatusLabel = createValueLabel("Checking...");

                codeGraphVersionLabel = createValueLabel("-");

                codeGraphPathLabel = createValueLabel("-");

                content.add(
                                createRow(
                                                "Status",
                                                codeGraphStatusLabel));

                content.add(
                                createRow(
                                                "Version",
                                                codeGraphVersionLabel));

                content.add(
                                createRow(
                                                "Path",
                                                codeGraphPathLabel));

                return content;
        }

        private JPanel createFooter() {

                JPanel panel = new JPanel(
                                new FlowLayout(
                                                FlowLayout.RIGHT));

                JButton refresh = new JButton("Refresh");

                refresh.addActionListener(
                                event -> refreshStatus());

                JButton close = new JButton("Close");

                close.addActionListener(
                                event -> frame.dispose());

                panel.add(refresh);
                panel.add(close);

                return panel;
        }

        private JLabel createSectionTitle(
                        String text) {

                JLabel label = new JLabel(text);

                label.setFont(
                                new Font(
                                                "Segoe UI",
                                                Font.BOLD,
                                                15));

                label.setBorder(
                                BorderFactory.createEmptyBorder(
                                                0,
                                                0,
                                                8,
                                                0));

                return label;
        }

        private JLabel createValueLabel(
                        String text) {

                JLabel label = new JLabel(text);

                label.setFont(
                                new Font(
                                                "Segoe UI",
                                                Font.PLAIN,
                                                13));

                return label;
        }

        private JPanel createRow(
                        String name,
                        JLabel value) {

                JPanel row = new JPanel(
                                new BorderLayout());

                row.setBorder(
                                BorderFactory.createEmptyBorder(
                                                4,
                                                0,
                                                4,
                                                0));

                JLabel nameLabel = new JLabel(name);

                nameLabel.setPreferredSize(
                                new Dimension(
                                                100,
                                                25));

                row.add(
                                nameLabel,
                                BorderLayout.WEST);

                row.add(
                                value,
                                BorderLayout.CENTER);

                return row;
        }

        public void showWindow() {

                refreshStatus();

                frame.setVisible(true);

                frame.toFront();

                frame.requestFocus();
        }

        private void refreshStatus() {

                try {

                        CliProcessService.CodeGraphInfo info = cliProcessService.detectInstallation();

                        agentStatusLabel.setText(
                                        "● Running");

                        versionLabel.setText(
                                        "1.0.0");

                        portLabel.setText(
                                        "9870");

                        pidLabel.setText(
                                        String.valueOf(
                                                        ProcessHandle.current().pid()));

                        if (info.installed()) {

                                codeGraphStatusLabel.setText(
                                                "● Installed");

                                codeGraphVersionLabel.setText(
                                                info.version());

                                codeGraphPathLabel.setText(
                                                info.path());

                        } else {

                                codeGraphStatusLabel.setText(
                                                "○ Not Found");

                                codeGraphVersionLabel.setText(
                                                "-");

                                codeGraphPathLabel.setText(
                                                "-");
                        }

                } catch (Exception e) {

                        codeGraphStatusLabel.setText(
                                        "Error");

                        codeGraphVersionLabel.setText(
                                        "-");

                        codeGraphPathLabel.setText(
                                        "-");
                }
        }
}