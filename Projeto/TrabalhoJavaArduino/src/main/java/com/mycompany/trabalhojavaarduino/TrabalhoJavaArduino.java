package com.mycompany.trabalhojavaarduino;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.util.Enumeration;

public class TrabalhoJavaArduino extends JFrame {
    private JComboBox<String> portList;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton sendButton;
    private JTextArea statusArea;
    private JTextField messageField;

    private SerialPort serialPort;
    private CommPortIdentifier selectedPortIdentifier;
    private OutputStream output;
    private static final int TIMEOUT = 2000;
    private static final int DATA_RATE = 9600;

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

        // Configuração do painel superior
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(new Color(70, 130, 180));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

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

        // Configuração do campo de mensagem e botão de envio
        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.setBackground(new Color(245, 245, 245));
        messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel messageLabel = new JLabel("Mensagem:");
        messageField = new JTextField(20);
        sendButton = new JButton("Enviar");
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.setEnabled(false);
        sendButton.setFocusPainted(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        messagePanel.add(messageLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        messagePanel.add(messageField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        messagePanel.add(sendButton, gbc);

        // Configuração da área de status
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setBackground(new Color(230, 230, 250));
        statusArea.setForeground(Color.BLACK);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

        listAvailablePorts();

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void listAvailablePorts() {
        Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portList.addItem(portIdentifier.getName());
            }
        }
    }

    private void connect() {
        String selectedPort = (String) portList.getSelectedItem();
        try {
            if (selectedPort == null) {
                statusArea.append("Nenhuma porta selecionada.\n");
                return;
            }
            selectedPortIdentifier = CommPortIdentifier.getPortIdentifier(selectedPort);
            serialPort = (SerialPort) selectedPortIdentifier.open("ArduinoCommunicationApp", TIMEOUT);
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            output = serialPort.getOutputStream();

            serialPort.addEventListener(new SerialPortEventListener() {
                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                        try {
                            byte[] readBuffer = new byte[20];
                            int numBytes = serialPort.getInputStream().read(readBuffer);
                            if (numBytes > 0) {
                                statusArea.append("Recebido: " + new String(readBuffer, 0, numBytes) + "\n");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            statusArea.append("Erro ao ler da porta: " + e.getMessage() + "\n");
                        }
                    }
                }
            });

            serialPort.notifyOnDataAvailable(true);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            sendButton.setEnabled(true);
            statusArea.append("Conectado à " + selectedPort + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            statusArea.append("Falha ao conectar a " + selectedPort + ": " + e.getMessage() + "\n");
        }
    }

    private void disconnect() {
        if (serialPort != null) {
            try {
                serialPort.removeEventListener();
                serialPort.close();
                statusArea.append("Desconectado\n");
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                sendButton.setEnabled(false);
            } catch (Exception e) {
                e.printStackTrace();
                statusArea.append("Falha ao desconectar: " + e.getMessage() + "\n");
            }
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (message.isEmpty()) {
            statusArea.append("Nenhuma mensagem para enviar.\n");
            return;
        }
        try {
            output.write(message.getBytes());
            output.flush();
            statusArea.append("Enviado: " + message + "\n");
            messageField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            statusArea.append("Falha ao enviar mensagem: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TrabalhoJavaArduino().setVisible(true);
            }
        });
    }
}
