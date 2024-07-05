package com.mycompany.trabalhojavaarduino;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

public class TrabalhoJavaArduino extends JFrame {
    private JComboBox<String> portList;
    private JButton connectButton;
    private JButton disconnectButton;
    private JTextArea statusArea;

    private SerialPort serialPort;
    private CommPortIdentifier selectedPortIdentifier;
    private static final int TIMEOUT = 2000;
    private static final int DATA_RATE = 9600;

    public TrabalhoJavaArduino() {
        setTitle("Arduino Communication");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        portList = new JComboBox<>();
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        statusArea = new JTextArea();
        statusArea.setEditable(false);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Select Port:"));
        topPanel.add(portList);
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(statusArea), BorderLayout.CENTER);

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
                statusArea.append("No port selected.\n");
                return;
            }
            selectedPortIdentifier = CommPortIdentifier.getPortIdentifier(selectedPort);
            serialPort = (SerialPort) selectedPortIdentifier.open("ArduinoCommunicationApp", TIMEOUT);
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            serialPort.addEventListener(new SerialPortEventListener() {
                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                        try {
                            byte[] readBuffer = new byte[20];
                            int numBytes = serialPort.getInputStream().read(readBuffer);
                            if (numBytes > 0) {
                                statusArea.append("Received: " + new String(readBuffer, 0, numBytes) + "\n");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            statusArea.append("Error reading from port: " + e.getMessage() + "\n");
                        }
                    }
                }
            });

            serialPort.notifyOnDataAvailable(true);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            statusArea.append("Connected to " + selectedPort + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            statusArea.append("Failed to connect to " + selectedPort + ": " + e.getMessage() + "\n");
        }
    }

    private void disconnect() {
        if (serialPort != null) {
            try {
                serialPort.removeEventListener();
                serialPort.close();
                statusArea.append("Disconnected\n");
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            } catch (Exception e) {
                e.printStackTrace();
                statusArea.append("Failed to disconnect: " + e.getMessage() + "\n");
            }
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

