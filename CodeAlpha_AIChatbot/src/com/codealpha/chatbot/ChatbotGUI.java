package com.codealpha.chatbot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatbotGUI extends JFrame {
	private static final long serialVersionUID = 1L;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private NLPEngine nlpEngine;

    public ChatbotGUI() {
        nlpEngine = new NLPEngine();
        setupUI();
    }

    private void setupUI() {
        setTitle("CodeAlpha Core-NLP Portal");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(27, 38, 59));
        headerPanel.setPreferredSize(new Dimension(500, 60));
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 15));
        JLabel titleLabel = new JLabel("🧠 CORE-NLP MATHEMATICAL VECTOR BOT");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Chat Log Panel
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Tech ledger look
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(13, 27, 42));
        chatArea.setForeground(new Color(224, 225, 221));
        chatArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Lower Control Panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBackground(new Color(27, 38, 59));
        inputPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setBackground(new Color(224, 225, 221));
        inputField.setForeground(new Color(13, 27, 42));
        
        sendButton = new JButton("PARSE");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        sendButton.setBackground(new Color(119, 141, 169));
        sendButton.setForeground(new Color(13, 27, 42));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Initial System Message
        chatArea.append("[SYSTEM INFO]: Vector space model loaded. Core-NLP vocabulary indexed.\n");
        chatArea.append("🤖 Assistant: Hello! Try testing my mathematical intent detection with phrases like 'Could you introduce yourself?'\n\n");

        // Action Handlers
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUserMessage();
            }
        };

        sendButton.addActionListener(actionListener);
        inputField.addActionListener(actionListener);
    }

    private void handleUserMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            chatArea.append("👤 User: " + text + "\n");
            inputField.setText("");

            // Compute math metrics
            String botResponse = nlpEngine.matchIntent(text);
            
            // Fast mock latency simulation
            Timer timer = new Timer(250, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    chatArea.append("🤖 Bot: " + botResponse + "\n\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatbotGUI().setVisible(true);
        });
    }
}
