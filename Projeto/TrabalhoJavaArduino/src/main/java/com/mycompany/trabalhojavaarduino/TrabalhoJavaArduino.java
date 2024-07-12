package com.mycompany.trabalhojavaarduino;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.OutputStream;
import java.util.Scanner;

public class TrabalhoJavaArduino extends JFrame {
    private static final String TITLE = "Comunicação com Arduino";
    private static final int FRAME_WIDTH = 600;
    private static final int FRAME_HEIGHT = 500;

    private static final Color TOP_PANEL_COLOR = new Color(70, 130, 180);
    private static final Color CONNECT_BUTTON_COLOR = new Color(46, 139, 87);
    private static final Color DISCONNECT_BUTTON_COLOR = new Color(220, 20, 60);
    private static final Color SEND_BUTTON_COLOR = new Color(70, 130, 180);
    private static final Color STATUS_AREA_COLOR = new Color(230, 230, 250);
    private static final Color MESSAGE_PANEL_COLOR = new Color(245, 245, 245);

    private JComboBox<String> portList;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton sendButton;
    private JTextArea statusArea;
    private JTextField messageField;

    private SerialPort serialPort;
    private OutputStream output;

    public TrabalhoJavaArduino() {
        setupFrame();
        setupComponents();
        listAvailablePorts();
    }

    private void setupFrame() {
        setTitle(TITLE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        setResizable(false);
    }

    private void setupComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel topPanel = createTopPanel(gbc);
        JPanel messagePanel = createMessagePanel(gbc);
        JScrollPane scrollPane = createStatusArea(gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(topPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(messagePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(scrollPane, gbc);
    }

    private JPanel createTopPanel(GridBagConstraints gbc) {
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(TOP_PANEL_COLOR);
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel portLabel = new JLabel("Selecionar Porta:");
        portLabel.setForeground(Color.WHITE);

        portList = new JComboBox<>();
        connectButton = createButton("Conectar", CONNECT_BUTTON_COLOR);
        disconnectButton = createButton("Desconectar", DISCONNECT_BUTTON_COLOR);
        disconnectButton.setEnabled(false);

        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());

        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        topPanel.add(portList, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        topPanel.add(connectButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        topPanel.add(disconnectButton, gbc);

        return topPanel;
    }

    private JPanel createMessagePanel(GridBagConstraints gbc) {
        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.setBackground(MESSAGE_PANEL_COLOR);
        messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel messageLabel = new JLabel("Mensagem:");
        messageField = new JTextField(20);
        sendButton = createButton("Enviar", SEND_BUTTON_COLOR);
        sendButton.setEnabled(false);

        sendButton.addActionListener(e -> sendMessage());

        gbc.gridx = 0;
        gbc.gridy = 0;
        messagePanel.add(messageLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        messagePanel.add(messageField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        messagePanel.add(sendButton, gbc);

        return messagePanel;
    }

    private JScrollPane createStatusArea(GridBagConstraints gbc) {
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setBackground(STATUS_AREA_COLOR);
        statusArea.setForeground(Color.BLACK);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return scrollPane;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private void listAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            portList.addItem(port.getSystemPortName());
        }
    }

    private void connect() {
        String selectedPort = (String) portList.getSelectedItem();
        if (selectedPort == null) {
            appendStatus("Nenhuma porta selecionada.");
            return;
        }

        try {
            serialPort = SerialPort.getCommPort(selectedPort);
            serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

            if (serialPort.openPort()) {
                output = serialPort.getOutputStream();
                updateUIOnConnect();
                appendStatus("Conectado à " + selectedPort);

                new Thread(() -> {
                    Scanner scanner = new Scanner(serialPort.getInputStream());
                    while (scanner.hasNextLine()) {
                        appendStatus("Recebido: " + scanner.nextLine());
                    }
                    scanner.close();
                }).start();
            } else {
                appendStatus("Falha ao conectar a " + selectedPort);
            }
        } catch (Exception e) {
            handleException("Falha ao conectar a " + selectedPort, e);
        }
    }

    private void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                serialPort.closePort();
                updateUIOnDisconnect();
                appendStatus("Desconectado");
            } catch (Exception e) {
                handleException("Falha ao desconectar", e);
            }
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (message.isEmpty()) {
            appendStatus("Nenhuma mensagem para enviar.");
            return;
        }

        try {
            output.write(message.getBytes());
            output.flush();
            appendStatus("Enviado: " + message);
            messageField.setText("");
        } catch (Exception e) {
            handleException("Falha ao enviar mensagem", e);
        }
    }

    private void appendStatus(String message) {
        statusArea.append(message + "\n");
    }

    private void handleException(String message, Exception e) {
        e.printStackTrace();
        appendStatus(message + ": " + e.getMessage());
    }

    private void updateUIOnConnect() {
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);
        sendButton.setEnabled(true);
    }

    private void updateUIOnDisconnect() {
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        sendButton.setEnabled(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrabalhoJavaArduino().setVisible(true));
    }
}
