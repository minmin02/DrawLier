package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class JavaChatClientView extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtInput;
    private JPanel chatContainer;
    private JScrollPane chatScrollPane;
    private JButton btnClearAll;
    private JButton btnLeaveRoom;
    private JButton btnEmoji;

    private DrawingPanel drawingPanel;

    private JButton btnSend;
    private JButton btnStartGame;
    private JLabel[] playerLabels;
    private JLabel lblRoomInfo;
    private JLabel lblTimer;
    private JLabel lblCurrentTurn;
    private JPanel playerPanel;
    private JLabel lblHostName;

    private String userName;
    private GameRoom currentRoom;
    private boolean isHost;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String serverIp;
    private String serverPort;

    private JButton btnColorPicker;
    private JButton btnEraserTool;

    private Color currentColor = Color.BLACK;
    private int strokeWidth = 2;
    private final Color DRAWING_BG_COLOR = Color.WHITE;
    private boolean isRunning = true;

    private boolean isLiar;
    private String myKeyword;

    private Map<String, ImageIcon> emojiMap;


    public JavaChatClientView(String userName, Socket socket, DataInputStream dis,
                              DataOutputStream dos, GameRoom room, boolean isHost, String serverIp, String serverPort) {
        this.userName = userName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.currentRoom = room;
        this.isHost = isHost;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        loadEmojis();
        initializeUI();
        new ListenNetwork().start();

        SwingUtilities.invokeLater(() -> {
            updatePlayerList(currentRoom.getPlayers());
        });
    }

    private void loadEmojis() {
        emojiMap = new HashMap<>();
        String path = "/Imoji";
        URL dirURL = getClass().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            try {
                File[] files = new File(dirURL.toURI()).listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        if (fileName.toLowerCase().endsWith(".png") || fileName.toLowerCase().endsWith(".gif")) {
                            String emojiKey = fileName.substring(0, fileName.lastIndexOf('.'));
                            ImageIcon icon = new ImageIcon(file.toURI().toURL());
                            Image scaledImage = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                            emojiMap.put(emojiKey, new ImageIcon(scaledImage));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
             System.err.println("이모티콘 디렉토리를 찾을 수 없습니다: " + path);
        }
    }


    private void initializeUI() {
        setTitle("DrawLier - " + currentRoom.getRoomName() + " [" + userName + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1200, 800);

        contentPane = new JPanel() {
            Image background = new ImageIcon(getClass().getResource("/game/back.png")).getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
            }
        };
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.setLayout(new BorderLayout(10, 10));
        setContentPane(contentPane);

        JPanel topPanel = createTopPanel();
        topPanel.setOpaque(false);
        contentPane.add(topPanel, BorderLayout.NORTH);

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplitPane.setResizeWeight(0.2);
        centerSplitPane.setEnabled(false);
        centerSplitPane.setOpaque(false);
        centerSplitPane.setBorder(null);

        drawingPanel = new DrawingPanel();
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel drawContainer = new JPanel(new BorderLayout());
        drawContainer.setOpaque(false);
        drawContainer.add(drawingPanel, BorderLayout.CENTER);

        JPanel toolPanel = createToolPanel();
        drawContainer.add(toolPanel, BorderLayout.SOUTH);

        centerSplitPane.setLeftComponent(drawContainer);

        JPanel chatPanel = createChatPanel();
        centerSplitPane.setRightComponent(chatPanel);

        centerSplitPane.setDividerLocation(0.38);
        contentPane.add(centerSplitPane, BorderLayout.CENTER);

        playerPanel = createPlayerPanel();
        playerPanel.setOpaque(false);
        contentPane.add(playerPanel, BorderLayout.EAST);

        JPanel bottomPanel = createBottomPanel();
        bottomPanel.setOpaque(false);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void updateToolButtons(JButton activeTool) {
        if (btnColorPicker != null) btnColorPicker.setBorder(UIManager.getBorder("Button.border"));
        if (btnEraserTool != null) btnEraserTool.setBorder(UIManager.getBorder("Button.border"));

        if (activeTool != null) {
            activeTool.setBorder(BorderFactory.createLineBorder(Color.GRAY, 3));
        }
    }

    private JPanel createToolPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);

        btnLeaveRoom = new JButton();
        btnLeaveRoom.setPreferredSize(new Dimension(80, 30));

        ImageIcon exitIcon = resizeIcon("/game/exit.png", 80, 30);
        if (exitIcon != null) {
            btnLeaveRoom.setIcon(exitIcon);
            UIUtils.applyButtonEffects(btnLeaveRoom);
        } else {
            btnLeaveRoom.setText("나가기");
            btnLeaveRoom.setBackground(new Color(220, 53, 69));
            btnLeaveRoom.setForeground(Color.WHITE);
        }
        btnLeaveRoom.addActionListener(e -> leaveRoom());
        panel.add(btnLeaveRoom);

        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 25));
        panel.add(separator);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        JLabel lblStroke = new JLabel();
        ImageIcon strictIcon = resizeIcon("/game/size.png", 40, 20);
        if (strictIcon != null) {
            lblStroke.setIcon(strictIcon);
        } else {
            lblStroke.setText("굵기:");
        }
        panel.add(lblStroke);

        String[] widths = {"1", "2", "4", "8", "12"};
        JComboBox<String> strokeSelector = new JComboBox<>(widths);
        strokeSelector.setSelectedItem("2");
        strokeSelector.addActionListener(e -> {
            try {
                strokeWidth = Integer.parseInt((String)strokeSelector.getSelectedItem());
            } catch (NumberFormatException ex) {
                strokeWidth = 2;
            }
        });
        panel.add(strokeSelector);

        btnColorPicker = new JButton();
        btnColorPicker.setPreferredSize(new Dimension(100, 30));

        ImageIcon colorIcon = resizeIcon("/game/color.png", 100, 30);
        if (colorIcon != null) {
            btnColorPicker.setIcon(colorIcon);
            UIUtils.applyButtonEffects(btnColorPicker);
        } else {
            btnColorPicker.setText("색상 선택");
            btnColorPicker.setBackground(currentColor);
        }

        updateToolButtons(btnColorPicker);

        btnColorPicker.addActionListener(e -> {
            if (!drawingPanel.isEnabled()) {
                JOptionPane.showMessageDialog(this, "당신의 턴이 아닙니다!", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            updateToolButtons(btnColorPicker);

            Color newColor = JColorChooser.showDialog(this, "색상 선택", currentColor);

            if (newColor != null) {
                currentColor = newColor;
            }
        });

        btnEraserTool = new JButton();
        btnEraserTool.setPreferredSize(new Dimension(80, 30));

        ImageIcon eraseIcon = resizeIcon("/game/erase.png", 80, 30);
        if (eraseIcon != null) {
            btnEraserTool.setIcon(eraseIcon);
            UIUtils.applyButtonEffects(btnEraserTool);
        } else {
            btnEraserTool.setText("지우개");
        }

        btnEraserTool.addActionListener(e -> {
            if (!drawingPanel.isEnabled()) {
                JOptionPane.showMessageDialog(this, "당신의 턴이 아닙니다!", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentColor = DRAWING_BG_COLOR;
            updateToolButtons(btnEraserTool);
        });

        btnClearAll = new JButton();
        btnClearAll.setPreferredSize(new Dimension(100, 30));

        ImageIcon allEraseIcon = resizeIcon("/game/allerase.png", 100, 30);
        if (allEraseIcon != null) {
            btnClearAll.setIcon(allEraseIcon);
            UIUtils.applyButtonEffects(btnClearAll);
        } else {
            btnClearAll.setText("전체 지우기");
        }

        btnClearAll.addActionListener(e -> {
            if (!drawingPanel.isEnabled()) {
                JOptionPane.showMessageDialog(this, "당신의 턴이 아닙니다!", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            drawingPanel.clear();
            sendProtocol("/clear");
            updateToolButtons(null);
        });

        panel.add(btnColorPicker);
        panel.add(btnEraserTool);
        panel.add(btnClearAll);

        return panel;
    }

    private void leaveRoom() {
        try {
            if (dos != null) {
                dos.writeUTF("/leaveRoom");
            }
            isRunning = false;
            SwingUtilities.invokeLater(() -> {
                new RoomListUI(userName, serverIp, serverPort, socket, dis, dos).setVisible(true);
                dispose();
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel leftInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftInfoPanel.setOpaque(false);

        JLabel lblRoomName = new JLabel("[" + currentRoom.getRoomName() + "]");
        lblRoomName.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblRoomName.setOpaque(false);
        leftInfoPanel.add(lblRoomName);

        JLabel lblCategoryIcon = new JLabel();
        ImageIcon catIcon = resizeIcon("/game/category.png", 80, 25);
        if (catIcon != null) {
            lblCategoryIcon.setIcon(catIcon);
        } else {
            lblCategoryIcon.setText("카테고리");
        }
        lblCategoryIcon.setOpaque(false);
        leftInfoPanel.add(lblCategoryIcon);

        JLabel lblCategoryValue = new JLabel(": " + currentRoom.getCategory());
        lblCategoryValue.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblCategoryValue.setOpaque(false);
        leftInfoPanel.add(lblCategoryValue);

        JLabel lblSep = new JLabel("|");
        lblSep.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblSep.setOpaque(false);
        leftInfoPanel.add(lblSep);

        JLabel lblHostIcon = new JLabel();
        ImageIcon hostIcon = resizeIcon("/game/host.png", 60, 25);
        if (hostIcon != null) {
            lblHostIcon.setIcon(hostIcon);
        } else {
            lblHostIcon.setText("방장");
        }
        lblHostIcon.setOpaque(false);
        leftInfoPanel.add(lblHostIcon);

        lblHostName = new JLabel(": " + currentRoom.getHostName());
        lblHostName.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblHostName.setOpaque(false);
        leftInfoPanel.add(lblHostName);

        panel.add(leftInfoPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.setOpaque(false);

        lblCurrentTurn = new JLabel("게임 준비 중...");
        lblCurrentTurn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblCurrentTurn.setForeground(new Color(0, 102, 204));
        lblCurrentTurn.setOpaque(false);
        rightPanel.add(lblCurrentTurn);

        lblTimer = new JLabel("남은 시간: 15초");
        lblTimer.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblTimer.setForeground(new Color(220, 53, 69));
        lblTimer.setOpaque(false);
        rightPanel.add(lblTimer);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("채팅"));
        panel.setBackground(Color.WHITE);

        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(250, 250, 252));
        chatContainer.setBorder(new EmptyBorder(10, 10, 10, 0));

        chatScrollPane = new JScrollPane(chatContainer);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 5));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        txtInput = new RoundedTextField(15);
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

        ImageIcon sendIcon = resizeIcon("/game/send.png", 70, 36);
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
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    private void appendSystemMessage(String message) {
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

    private void appendChatMessage(String fullMessage) {
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
            JPanel bubble = new RoundedBubblePanel(bubbleColor, isMyMessage);
            bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
            bubble.setOpaque(false);

            // ★★★ [수정] 말풍선 최대 너비 제한 ★★★
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
                // msgArea.setColumns(18); // 더 이상 필요 없음
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

    private void appendText(String msg) {
        appendChatMessage(msg);
    }

    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        java.util.List<String> players = currentRoom.getPlayers();
        panel.setBorder(BorderFactory.createTitledBorder("플레이어 (" + players.size() + "/4)"));
        panel.setPreferredSize(new Dimension(200, 0));

        playerLabels = new JLabel[4];

        int imgWidth = 180;
        int imgHeight = 50;

        for (int i = 0; i < 4; i++) {
            JPanel slotPanel = new JPanel(new BorderLayout());
            slotPanel.setOpaque(false);
            slotPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            slotPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

            playerLabels[i] = new JLabel();
            playerLabels[i].setFont(new Font("맑은 고딕", Font.BOLD, 14));

            playerLabels[i].setHorizontalTextPosition(JLabel.CENTER);
            playerLabels[i].setVerticalTextPosition(JLabel.CENTER);
            playerLabels[i].setHorizontalAlignment(SwingConstants.CENTER);
            playerLabels[i].setOpaque(false);

            if (i < players.size()) {
                String playerName = players.get(i);
                playerLabels[i].setText(playerName);

                if (playerName.equals(currentRoom.getHostName())) {
                    playerLabels[i].setText("👑 " + playerName);
                }

                ImageIcon playerIcon = resizeIcon("/game/player.png", imgWidth, imgHeight);
                if (playerIcon != null) {
                    playerLabels[i].setIcon(playerIcon);
                } else {
                    playerLabels[i].setOpaque(true);
                    playerLabels[i].setBackground(new Color(180, 220, 255));
                }

            } else {
                ImageIcon waitingIcon = resizeIcon("/game/waiting.png", imgWidth, imgHeight);
                if (waitingIcon != null) {
                    playerLabels[i].setIcon(waitingIcon);
                } else {
                    playerLabels[i].setOpaque(true);
                    playerLabels[i].setBackground(new Color(230, 230, 230));
                }
            }

            slotPanel.add(playerLabels[i], BorderLayout.CENTER);
            panel.add(slotPanel);
        }

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        btnStartGame = new JButton("게임 시작");
        btnStartGame.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnStartGame.setPreferredSize(new Dimension(250, 50));
        btnStartGame.setBackground(new Color(40, 167, 69));
        btnStartGame.setForeground(Color.WHITE);
        btnStartGame.setFocusPainted(false);

        updateStartButtonState();

        btnStartGame.addActionListener(e -> startGame());
        panel.add(btnStartGame);

        return panel;
    }

    private void updateStartButtonState() {
        List<String> players = currentRoom.getPlayers();

        if (isHost) {
            if (players.size() < 4) {
                btnStartGame.setEnabled(false);
                btnStartGame.setText("4명이 모여야 시작 가능 (" + players.size() + "/4)");
                btnStartGame.setBackground(Color.GRAY);
            } else {
                btnStartGame.setEnabled(true);
                btnStartGame.setText("게임 시작");
                btnStartGame.setBackground(new Color(40, 167, 69));
            }
        } else {
            btnStartGame.setEnabled(false);
            btnStartGame.setText("방장이 게임을 시작합니다");
            btnStartGame.setBackground(Color.GRAY);
        }
    }

    private void disconnect() {
        try {
            if (dos != null) {
                sendProtocol("/leaveRoom");
                dos.close();
            }
            if (dis != null) dis.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;

        if (currentRoom.isGameRunning() && !currentRoom.isPlayerTurn(userName)) {
            appendSystemMessage("[시스템] 당신의 턴이 아닙니다!");
            txtInput.setText("");
            return;
        }

        sendProtocol(userName + ": " + msg);
        txtInput.setText("");
    }

    private void showEmojiPicker() {
        JDialog emojiDialog = new JDialog(this, "이모티콘 선택", true);
        emojiDialog.setSize(450, 350);
        emojiDialog.setLocationRelativeTo(this);

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
                    sendProtocol(userName + ": /emoji " + emojiKey);
                    emojiDialog.dispose();
                });
                emojiPanel.add(btnEmoticon);
            }
        }

        JScrollPane scrollPane = new JScrollPane(emojiPanel);
        emojiDialog.add(scrollPane);
        emojiDialog.setVisible(true);
    }


    private void sendProtocol(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            appendSystemMessage("메시지 전송 실패");
            e.printStackTrace();
        }
    }

    private void startGame() {
        if (!isHost) {
            JOptionPane.showMessageDialog(this, "방장만 게임을 시작할 수 있습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentRoom.getPlayers().size() < 4) {
            JOptionPane.showMessageDialog(this, "4명이 모여야 게임을 시작할 수 있습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectedCategory = currentRoom.getCategory();
        sendProtocol("/gameStart " + selectedCategory);
    }

    private void updateTurnInfo(int turnIndex, int round, String currentPlayer, int remainingSeconds) {
        boolean isMyTurn = currentPlayer.equals(userName);

        String turnText = String.format("라운드 %d/4 - %s의 턴 (%d/4명)",
                round, currentPlayer, turnIndex + 1);

        lblCurrentTurn.setText(turnText);
        lblCurrentTurn.setForeground(isMyTurn ? new Color(255, 100, 0) : new Color(0, 102, 204));

        lblTimer.setText("남은 시간: " + remainingSeconds + "초");

        boolean canInteract = isMyTurn;
        drawingPanel.setEnabled(canInteract);
        txtInput.setEnabled(canInteract);
        btnSend.setEnabled(canInteract);

        if (btnColorPicker != null) btnColorPicker.setEnabled(canInteract);
        if (btnEraserTool != null) btnEraserTool.setEnabled(canInteract);
        if (btnClearAll != null) btnClearAll.setEnabled(canInteract);
    }

    private void updatePlayerList(java.util.List<String> players) {
        SwingUtilities.invokeLater(() -> {
            int imgWidth = 180;
            int imgHeight = 50;

            for (int i = 0; i < 4; i++) {
                playerLabels[i].setHorizontalTextPosition(JLabel.CENTER);
                playerLabels[i].setVerticalTextPosition(JLabel.CENTER);
                playerLabels[i].setOpaque(false);

                if (i < players.size()) {
                    String playerName = players.get(i);
                    playerLabels[i].setText(playerName);

                    if (playerName.equals(currentRoom.getHostName())) {
                        playerLabels[i].setText("👑 " + playerName);
                    }

                    ImageIcon playerIcon = resizeIcon("/game/player.png", imgWidth, imgHeight);
                    if (playerIcon != null) {
                        playerLabels[i].setIcon(playerIcon);
                    } else {
                        playerLabels[i].setOpaque(true);
                        playerLabels[i].setBackground(new Color(180, 220, 255));
                        playerLabels[i].setIcon(null);
                    }
                } else {
                    ImageIcon waitingIcon = resizeIcon("/game/waiting.png", imgWidth, imgHeight);
                    if (waitingIcon != null) {
                        playerLabels[i].setIcon(waitingIcon);
                    } else {
                        playerLabels[i].setOpaque(true);
                        playerLabels[i].setBackground(new Color(230, 230, 230));
                        playerLabels[i].setIcon(null);
                    }
                }
            }

            playerPanel.setBorder(BorderFactory.createTitledBorder(
                    "플레이어 (" + players.size() + "/" + currentRoom.getMaxPlayers() + ")"));

            updateStartButtonState();
        });
    }

    private void openVotingUI() {
        SwingUtilities.invokeLater(() -> {
            List<String> players = currentRoom.getPlayers();
            new VotingUI(userName, players, currentRoom.getRoomId(), serverIp, serverPort, socket, dis, dos);
            dispose();
        });
    }


    class ListenNetwork extends Thread {
        public void run() {
            while (isRunning) {
                try {
                    String msg = dis.readUTF();
                    if(msg.startsWith("/updatePlayerList")) {
                        String playerStr = msg.substring(18);
                        String [] players = playerStr.split(",");

                        currentRoom.getPlayers().clear();

                        List<String> newPlayerList = new ArrayList<>();
                        for(String p : players){
                            if(!p.trim().isEmpty()){
                                newPlayerList.add(p);
                            }
                        }
                        updatePlayerList(newPlayerList);
                    }

                    if (msg.startsWith("/gameStart ")) {
                        String[] parts = msg.substring(11).split("\\|");
                        if (parts.length >= 2) {
                            String role = parts[0];
                            String info = parts[1];

                            SwingUtilities.invokeLater(() -> {
                                if(btnLeaveRoom != null){
                                    btnLeaveRoom.setEnabled(false);
                                }
                                if (role.equals("LIAR")) {
                                    isLiar = true;
                                    myKeyword = info;

                                    JDialog dialog = new JDialog(JavaChatClientView.this, true);
                                    dialog.setSize(462, 432);
                                    dialog.setLocationRelativeTo(JavaChatClientView.this);
                                    dialog.setResizable(false);

                                    JPanel panel = new JPanel(new BorderLayout(10, 10)) {
                                        Image bgImage = null;
                                        {
                                            try {
                                                bgImage = new ImageIcon(getClass().getResource("/info/backGround.png")).getImage();
                                            } catch (Exception e) {
                                                System.err.println("경고: 배경 이미지를 찾을 수 없습니다.");
                                            }
                                        }

                                        @Override
                                        protected void paintComponent(Graphics g) {
                                            super.paintComponent(g);
                                            if (bgImage != null) {
                                                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                                            }
                                        }
                                    };
                                    panel.setOpaque(false);
                                    panel.setBorder(new EmptyBorder(30, 30, 30, 30));

                                    JLabel messageLabel = new JLabel(
                                            "<html><div style='text-align: right;'>" +
                                                    "카테고리: " + info +
                                                    "</div></html>"
                                    );
                                    messageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                                    messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                                    messageLabel.setOpaque(false);

                                    JButton okButton = new JButton();
                                    try {
                                        ImageIcon icon = new ImageIcon(getClass().getResource("/info/check.png"));
                                        Image img = icon.getImage().getScaledInstance(120, 40, Image.SCALE_SMOOTH);
                                        okButton.setIcon(new ImageIcon(img));
                                        UIUtils.applyButtonEffects(okButton);
                                    } catch (Exception e) {
                                        okButton.setText("확인");
                                        okButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
                                        System.err.println("경고: 확인 버튼 이미지를 찾을 수 없습니다.");
                                    }
                                    okButton.setPreferredSize(new Dimension(120, 40));
                                    okButton.addActionListener(e -> dialog.dispose());

                                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                                    buttonPanel.setOpaque(false);
                                    buttonPanel.add(okButton);

                                    JPanel bottomPanel = new JPanel(new BorderLayout());
                                    bottomPanel.setOpaque(false);
                                    bottomPanel.add(messageLabel, BorderLayout.EAST);
                                    bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

                                    panel.add(bottomPanel, BorderLayout.SOUTH);

                                    dialog.add(panel);
                                    dialog.setVisible(true);

                                } else if (role.equals("CITIZEN")) {
                                    isLiar = false;
                                    myKeyword = info;

                                    JDialog dialog = new JDialog(JavaChatClientView.this, true);
                                    dialog.setSize(462, 432);
                                    dialog.setLocationRelativeTo(JavaChatClientView.this);
                                    dialog.setResizable(false);

                                    JPanel panel = new JPanel(new BorderLayout(10, 10)) {
                                        Image bgImage = null;
                                        {
                                            try {
                                                bgImage = new ImageIcon(getClass().getResource("/info/backGround2.png")).getImage();
                                            } catch (Exception e) {
                                                System.err.println("경고: 배경 이미지를 찾을 수 없습니다.");
                                            }
                                        }

                                        @Override
                                        protected void paintComponent(Graphics g) {
                                            super.paintComponent(g);
                                            if (bgImage != null) {
                                                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                                            }
                                        }
                                    };
                                    panel.setOpaque(false);
                                    panel.setBorder(new EmptyBorder(30, 30, 30, 30));

                                    JLabel messageLabel = new JLabel(
                                            "<html><div style='text-align: right;'>" +
                                                    "키워드: " + info +
                                                    "</div></html>"
                                    );
                                    messageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                                    messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                                    messageLabel.setOpaque(false);

                                    JButton okButton = new JButton();
                                    try {
                                        ImageIcon icon = new ImageIcon(getClass().getResource("/info/check2.png"));
                                        Image img = icon.getImage().getScaledInstance(120, 40, Image.SCALE_SMOOTH);
                                        okButton.setIcon(new ImageIcon(img));
                                        UIUtils.applyButtonEffects(okButton);
                                    } catch (Exception e) {
                                        okButton.setText("확인");
                                        okButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
                                        System.err.println("경고: 확인 버튼 이미지를 찾을 수 없습니다.");
                                    }
                                    okButton.setPreferredSize(new Dimension(120, 40));
                                    okButton.addActionListener(e -> dialog.dispose());

                                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                                    buttonPanel.setOpaque(false);
                                    buttonPanel.add(okButton);

                                    JPanel bottomPanel = new JPanel(new BorderLayout());
                                    bottomPanel.setOpaque(false);
                                    bottomPanel.add(messageLabel, BorderLayout.EAST);
                                    bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

                                    panel.add(bottomPanel, BorderLayout.SOUTH);

                                    dialog.add(panel);
                                    dialog.setVisible(true);
                                }

                                appendSystemMessage("===== 게임이 시작되었습니다! =====");
                                btnStartGame.setEnabled(false);
                                btnStartGame.setText("게임 진행 중");
                                btnStartGame.setBackground(Color.GRAY);
                            });
                        }
                    }
                    else if (msg.startsWith("/gameState|")) {
                        String[] parts = msg.substring(11).split("\\|");
                        int turnIndex = Integer.parseInt(parts[0]);
                        int round = Integer.parseInt(parts[1]);
                        String currentPlayer = parts[2];
                        int remainingSeconds = Integer.parseInt(parts[3]);

                        SwingUtilities.invokeLater(() -> {
                            updateTurnInfo(turnIndex, round, currentPlayer, remainingSeconds);
                        });
                    }
                    else if (msg.startsWith("/gameEnded")) {
                        System.out.println("[Client] 게임 종료 수신 - 투표 화면으로 전환");
                        isRunning = false;

                        SwingUtilities.invokeLater(() -> {
                            appendSystemMessage("===== 게임이 종료되었습니다! 투표를 시작합니다. =====");
                            openVotingUI();
                        });

                        break;
                    }
                    else if (msg.startsWith("/playerJoined ")) {
                        String newPlayer = msg.substring(14);
                        currentRoom.addPlayer(newPlayer);
                        appendSystemMessage("[입장] " + newPlayer + "님이 입장했습니다.");
                        updatePlayerList(currentRoom.getPlayers());
                    }
                    else if (msg.startsWith("/playerLeft ")) {
                        String leftPlayer = msg.substring(12);
                        currentRoom.removePlayer(leftPlayer);
                        appendSystemMessage("[퇴장] " + leftPlayer + "님이 퇴장했습니다.");
                        updatePlayerList(currentRoom.getPlayers());
                    }
                    else if (msg.startsWith("/hostChanged ")) {
                        String newHostName = msg.substring(13);
                        SwingUtilities.invokeLater(() -> {
                            currentRoom.setHostName(newHostName);
                            isHost = userName.equals(newHostName);
                            appendSystemMessage("[시스템] 방장이 " + newHostName + "님으로 변경되었습니다.");
                            lblHostName.setText(": " + newHostName);
                            updatePlayerList(currentRoom.getPlayers());
                            updateStartButtonState();
                        });
                    }
                    else if (msg.startsWith("/draw ")) {
                        drawingPanel.processDrawCommand(msg);
                    }
                    else if (msg.startsWith("/clear")) {
                        drawingPanel.clear();
                    }
                    else {
                        appendText(msg);
                    }
                } catch (IOException e) {
                    if(isRunning){
                        appendSystemMessage("서버와의 연결이 끊어졌습니다.");
                    }
                    break;
                }
            }
            System.out.println("[Client] ListenNetwork 스레드 종료");
        }
    }

    class DrawingPanel extends JPanel {
        private Image screenImage;
        private Graphics2D screenGraphic;
        private int prevX, prevY;
        private boolean isEnabled = true;

        public DrawingPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(600, 500));

            MyMouseListener mm = new MyMouseListener();
            addMouseListener(mm);
            addMouseMotionListener(mm);
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.isEnabled = enabled;
            setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (screenImage != null) {
                g.drawImage(screenImage, 0, 0, null);
            }
        }

        public void checkImageBuffer() {
            if (screenImage == null) {
                screenImage = createImage(getWidth(), getHeight());
                screenGraphic = (Graphics2D) screenImage.getGraphics();
                screenGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                screenGraphic.setColor(Color.WHITE);
                screenGraphic.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        public void processDrawCommand(String command) {
            checkImageBuffer();
            try {
                String[] parts = command.split(" ");
                int x1 = Integer.parseInt(parts[1]);
                int y1 = Integer.parseInt(parts[2]);
                int x2 = Integer.parseInt(parts[3]);
                int y2 = Integer.parseInt(parts[4]);
                int r = Integer.parseInt(parts[5]);
                int g = Integer.parseInt(parts[6]);
                int b = Integer.parseInt(parts[7]);
                int width = Integer.parseInt(parts[8]);

                screenGraphic.setColor(new Color(r, g, b));
                screenGraphic.setStroke(new BasicStroke(width));
                screenGraphic.drawLine(x1, y1, x2, y2);
                repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void clear() {
            checkImageBuffer();
            screenGraphic.setColor(Color.WHITE);
            screenGraphic.fillRect(0, 0, getWidth(), getHeight());
            repaint();
        }

        class MyMouseListener extends MouseAdapter {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled) {
                    return;
                }

                checkImageBuffer();
                prevX = e.getX();
                prevY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isEnabled) {
                    return;
                }

                checkImageBuffer();
                int x = e.getX();
                int y = e.getY();

                screenGraphic.setColor(currentColor);
                screenGraphic.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                screenGraphic.drawLine(prevX, prevY, x, y);

                String drawCommand = String.format("/draw %d %d %d %d %d %d %d %d",
                        prevX, prevY, x, y,
                        currentColor.getRed(),
                        currentColor.getGreen(),
                        currentColor.getBlue(),
                        strokeWidth);

                sendProtocol(drawCommand);

                prevX = x;
                prevY = y;
                repaint();
            }
        }
    }

    static class RoundedBubblePanel extends JPanel {
        private Color backgroundColor;
        private boolean isRight;
        private static final int RADIUS = 20;
        private static final int TAIL_SIZE = 18;

        public RoundedBubblePanel(Color backgroundColor, boolean isRight) {
            this.backgroundColor = backgroundColor;
            this.isRight = isRight;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setColor(backgroundColor);

            if (isRight) {
                g2.fillRoundRect(0, 0, width - TAIL_SIZE, height, RADIUS, RADIUS);
                int[] xPoints = {width - TAIL_SIZE, width - 2, width - TAIL_SIZE};
                int[] yPoints = {height - 25, height - 3, height - 8};
                g2.fillPolygon(xPoints, yPoints, 3);
            } else {
                g2.fillRoundRect(TAIL_SIZE, 0, width - TAIL_SIZE, height, RADIUS, RADIUS);
                int[] xPoints = {TAIL_SIZE, 2, TAIL_SIZE};
                int[] yPoints = {height - 25, height - 3, height - 8};
                g2.fillPolygon(xPoints, yPoints, 3);
            }

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width += TAIL_SIZE;
            return size;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension size = super.getMaximumSize();
            size.width = Math.min(size.width, 140);
            return size;
        }
    }

    static class RoundedBorder implements javax.swing.border.Border {
        private int radius;
        private Color backgroundColor;

        RoundedBorder(int radius, Color backgroundColor) {
            this.radius = radius;
            this.backgroundColor = backgroundColor;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius/2 + 2, this.radius/2 + 2, this.radius/2 + 4, this.radius/2 + 4);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(x + 2, y + 2, width - 3, height - 3, radius, radius);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(x, y, width - 4, height - 4, radius, radius);
            g2.dispose();
        }
    }

    static class SpeechBubbleBorder implements javax.swing.border.Border {
        private int radius;
        private Color backgroundColor;
        private boolean isRight;
        private static final int TAIL_SIZE = 12;

        SpeechBubbleBorder(int radius, Color backgroundColor, boolean isRight) {
            this.radius = radius;
            this.backgroundColor = backgroundColor;
            this.isRight = isRight;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            if (isRight) {
                return new Insets(radius/2, radius/2, radius/2, radius/2 + TAIL_SIZE);
            } else {
                return new Insets(radius/2, radius/2 + TAIL_SIZE, radius/2, radius/2);
            }
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(backgroundColor);

            if (isRight) {
                g2.fillRoundRect(x, y, width - TAIL_SIZE, height, radius, radius);
                int[] xPoints = {width - TAIL_SIZE, width - 2, width - TAIL_SIZE};
                int[] yPoints = {height - 20, height - 5, height - 10};
                g2.fillPolygon(xPoints, yPoints, 3);
            } else {
                g2.fillRoundRect(x + TAIL_SIZE, y, width - TAIL_SIZE, height, radius, radius);
                int[] xPoints = {TAIL_SIZE, 2, TAIL_SIZE};
                int[] yPoints = {height - 20, height - 5, height - 10};
                g2.fillPolygon(xPoints, yPoints, 3);
            }

            g2.dispose();
        }
    }

    static class RoundedTextField extends JTextField {
        private int radius;

        public RoundedTextField(int radius) {
            super();
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(new Color(220, 220, 220));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedInputBorder implements javax.swing.border.Border {
        private int radius;
        private Color borderColor;

        RoundedInputBorder(int radius, Color borderColor) {
            this.radius = radius;
            this.borderColor = borderColor;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(250, 250, 250));
            g2.fillRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
    }

    private ImageIcon resizeIcon(String path, int width, int height) {
        try {
            java.net.URL imgURL = getClass().getResource(path);
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            } else {
                System.err.println("이미지를 찾을 수 없음: " + path);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
