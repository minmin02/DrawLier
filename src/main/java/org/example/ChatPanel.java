package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ChatPanel extends JPanel {
    private JTextField txtInput;
    private JPanel chatContainer;
    private JScrollPane chatScrollPane;
    private JButton btnEmoji;
    private JButton btnSend;

    private String userName;
    private GameRoom currentRoom;
    private Map<String, ImageIcon> emojiMap;
    private ChatCallback callback;
    private JFrame parentFrame;

    public interface ChatCallback {
        void sendProtocol(String msg);
        ImageIcon resizeIcon(String path, int width, int height);
    }

    public ChatPanel(String userName, GameRoom currentRoom, Map<String, ImageIcon> emojiMap,
                     ChatCallback callback, JFrame parentFrame) {
        this.userName = userName;
        this.currentRoom = currentRoom;
        this.emojiMap = emojiMap;
        this.callback = callback;
        this.parentFrame = parentFrame;

        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("채팅"));
        setBackground(Color.WHITE);

        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(250, 250, 252));
        chatContainer.setBorder(new EmptyBorder(10, 10, 10, 0));

        chatScrollPane = new JScrollPane(chatContainer);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 5));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        txtInput = new UIComponents.RoundedTextField(15);
        txtInput.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        txtInput.setBorder(new EmptyBorder(10, 15, 10, 15));
        txtInput.setBackground(new Color(250, 250, 250));
        txtInput.addActionListener(e -> sendMessage());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        btnEmoji = new JButton("😊");
        btnEmoji.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        btnEmoji.setPreferredSize(new Dimension(50, 36));
        btnEmoji.setToolTipText("이모지 선택");
        btnEmoji.setBackground(new Color(245, 245, 245));
        btnEmoji.setFocusPainted(false);
        btnEmoji.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        btnEmoji.addActionListener(e -> showEmojiPicker());

        btnSend = new JButton();
        btnSend.setPreferredSize(new Dimension(70, 36));

        ImageIcon sendIcon = callback.resizeIcon("/game/send.png", 70, 36);
        if (sendIcon != null) {
            btnSend.setIcon(sendIcon);
            UIUtils.applyButtonEffects(btnSend);
        } else {
            btnSend.setText("전송");
        }
        btnSend.addActionListener(e -> sendMessage());

        buttonPanel.add(btnEmoji);
        buttonPanel.add(btnSend);

        inputPanel.add(txtInput, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    public JTextField getTxtInput() {
        return txtInput;
    }

    public JButton getBtnSend() {
        return btnSend;
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    public void appendSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            messagePanel.setOpaque(false);
            messagePanel.setBorder(new EmptyBorder(3, 10, 3, 10));

            JLabel systemLabel = new JLabel(message);
            systemLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            systemLabel.setForeground(new Color(120, 120, 120));
            systemLabel.setBorder(new EmptyBorder(4, 12, 4, 12));
            systemLabel.setBackground(new Color(240, 240, 240));
            systemLabel.setOpaque(true);

            messagePanel.add(systemLabel);
            chatContainer.add(messagePanel);
            chatContainer.revalidate();

            scrollToBottom();
        });
    }

    public void appendChatMessage(String fullMessage) {
        SwingUtilities.invokeLater(() -> {
            if (fullMessage.startsWith("[입장]") || fullMessage.startsWith("[퇴장]") ||
                    fullMessage.startsWith("[시스템]") || fullMessage.startsWith("=====")) {
                appendSystemMessage(fullMessage);
                return;
            }

            String sender = "";
            String message = fullMessage;
            boolean isMyMessage = false;

            if (fullMessage.contains(": ")) {
                int colonIndex = fullMessage.indexOf(": ");
                sender = fullMessage.substring(0, colonIndex);
                message = fullMessage.substring(colonIndex + 2);
                isMyMessage = sender.equals(userName);
            }

            JPanel outerPanel = new JPanel() {
                @Override
                public Dimension getMaximumSize() {
                    Dimension pref = getPreferredSize();
                    return new Dimension(Integer.MAX_VALUE, pref.height);
                }
            };

            if (isMyMessage) {
                outerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            } else {
                outerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            }
            outerPanel.setOpaque(false);
            outerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

            JPanel messageContainer = new JPanel();
            messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.X_AXIS));
            messageContainer.setOpaque(false);

            Color bubbleColor = isMyMessage ? new Color(220, 240, 255) : new Color(240, 240, 240);
            JPanel bubble = new UIComponents.RoundedBubblePanel(bubbleColor, isMyMessage);
            bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
            bubble.setOpaque(false);

            bubble.setMaximumSize(new Dimension(250, Short.MAX_VALUE));

            int tailSize = 18;
            if (isMyMessage) {
                bubble.setBorder(new EmptyBorder(8, 12, 8, 12 + tailSize));
            } else {
                bubble.setBorder(new EmptyBorder(8, 12 + tailSize, 8, 12));
            }

            if (!sender.isEmpty() && !isMyMessage) {
                JLabel nameLabel = new JLabel(sender);
                nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                nameLabel.setForeground(new Color(100, 100, 100));
                nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                bubble.add(nameLabel);
                bubble.add(Box.createVerticalStrut(2));
            }

            if (message.startsWith("/emoji ")) {
                String emojiKey = message.substring(7).trim();
                if (emojiMap.containsKey(emojiKey)) {
                    JLabel emojiLabel = new JLabel(emojiMap.get(emojiKey));
                    bubble.add(emojiLabel);
                }
            } else {
                JTextArea msgArea = new JTextArea(message);
                msgArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                msgArea.setForeground(isMyMessage ? new Color(40, 40, 40) : Color.BLACK);
                msgArea.setOpaque(false);
                msgArea.setEditable(false);
                msgArea.setLineWrap(true);
                msgArea.setWrapStyleWord(true);
                msgArea.setBorder(null);
                msgArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                bubble.add(msgArea);
            }

            JLabel timeLabel = new JLabel(getCurrentTime());
            timeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 9));
            timeLabel.setForeground(new Color(150, 150, 150));
            timeLabel.setVerticalAlignment(SwingConstants.BOTTOM);

            if (isMyMessage) {
                messageContainer.add(timeLabel);
                messageContainer.add(Box.createHorizontalStrut(5));
                messageContainer.add(bubble);
            } else {
                messageContainer.add(bubble);
                messageContainer.add(Box.createHorizontalStrut(5));
                messageContainer.add(timeLabel);
            }

            outerPanel.add(messageContainer);
            chatContainer.add(outerPanel);
            chatContainer.revalidate();
            chatContainer.repaint();

            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;

        if (currentRoom != null && currentRoom.isGameRunning() && !currentRoom.isPlayerTurn(userName)) {
            appendSystemMessage("[시스템] 당신의 턴이 아닙니다!");
            txtInput.setText("");
            return;
        }

        if (callback != null) {
            callback.sendProtocol(userName + ": " + msg);
        }
        txtInput.setText("");
    }

    private void showEmojiPicker() {
        JDialog emojiDialog = new JDialog(parentFrame, "이모티콘 선택", true);
        emojiDialog.setSize(450, 350);
        emojiDialog.setLocationRelativeTo(parentFrame);

        JPanel emojiPanel = new JPanel(new GridLayout(0, 6, 5, 5));
        emojiPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (emojiMap.isEmpty()) {
            emojiPanel.add(new JLabel("이모티콘을 불러올 수 없습니다."));
        } else {
            for (Map.Entry<String, ImageIcon> entry : emojiMap.entrySet()) {
                String emojiKey = entry.getKey();
                ImageIcon emojiIcon = entry.getValue();

                JButton btnEmoticon = new JButton(emojiIcon);
                btnEmoticon.setToolTipText(emojiKey);
                btnEmoticon.setBorder(BorderFactory.createEmptyBorder());
                btnEmoticon.setContentAreaFilled(false);
                btnEmoticon.setFocusPainted(false);
                btnEmoticon.setCursor(new Cursor(Cursor.HAND_CURSOR));

                btnEmoticon.addActionListener(e -> {
                    if (callback != null) {
                        callback.sendProtocol(userName + ": /emoji " + emojiKey);
                    }
                    emojiDialog.dispose();
                });
                emojiPanel.add(btnEmoticon);
            }
        }

        JScrollPane scrollPane = new JScrollPane(emojiPanel);
        emojiDialog.add(scrollPane);
        emojiDialog.setVisible(true);
    }
}
