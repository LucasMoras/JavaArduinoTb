package com.mycompany.trabalhojavaarduino;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
public class TrabalhoJavaArduino extends JFrame {
    private JComboBox<String> portList;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton sendButton;
    private JTextArea statusArea;
    private JTextField messageField;
    private SerialPort serialPort;
    private OutputStream output;
    private InputStream input;
    public TrabalhoJavaArduino() {
        setTitle("Comunicação com Arduino");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        setResizable(false);
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
        listAvailablePorts(); // Lista as portas disponíveis ao iniciar
    }
    private JPanel createTopPanel(GridBagConstraints gbc) {
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(new Color(70, 130, 180));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel portLabel = new JLabel("Selecionar Porta:");
        portLabel.setForeground(Color.WHITE);
        portList = new JComboBox<>();
        connectButton = new JButton("Conectar");
        connectButton.setBackground(new Color(46, 139, 87));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        disconnectButton = new JButton("Desconectar");
        disconnectButton.setBackground(new Color(220, 20, 60));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setEnabled(false);
        disconnectButton.setFocusPainted(false);
        sendButton = new JButton("Enviar");
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.setEnabled(false);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener((ActionEvent e) -> {
            sendMessage();
        });
        connectButton.addActionListener((ActionEvent e) -> {
            connect();
        });
        disconnectButton.addActionListener((ActionEvent e) -> {
            disconnect();
        });
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
        messagePanel.setBackground(new Color(245, 245, 245));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel messageLabel = new JLabel("Mensagem:");
        messageField = new JTextField(20);
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
        statusArea.setBackground(new Color(230, 230, 250));
        statusArea.setForeground(Color.BLACK);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return scrollPane;
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
            statusArea.append("Nenhuma porta selecionada.\n");
            return;
        }
        try {
            serialPort = SerialPort.getCommPort(selectedPort);
            serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
            if (serialPort.openPort()) {
                input = serialPort.getInputStream();
                output = serialPort.getOutputStream();
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                sendButton.setEnabled(true);
                statusArea.append("Conectado à " + selectedPort + "\n");
                new Thread(() -> {
                    try {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            String message = new String(buffer, 0, bytesRead);
                            statusArea.append("Recebido: " + message + "\n");
                        }
                    } catch (IOException e) {
                    }
                }).start();

            } else {
                statusArea.append("Falha ao conectar a " + selectedPort + "\n");
            }
        } catch (SerialPortInvalidPortException e) {
        }
    }
    private void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            sendButton.setEnabled(false);
            statusArea.append("Desconectado\n");
        }
    }
    private void sendMessage() {
        String message = messageField.getText();
        if (message.isEmpty()) {
            statusArea.append("Nenhuma mensagem para enviar.\n");
            return;
        }
        try {
            output.write((message + "\n").getBytes());
            output.flush();
            statusArea.append("Enviado: " + message + "\n");
            messageField.setText("");
        } catch (IOException e) {
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrabalhoJavaArduino().setVisible(true));
    }
}
