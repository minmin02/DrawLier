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

public class JavaChatClientView extends JFrame implements DrawingPanel.DrawingCallback, ChatPanel.ChatCallback {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JButton btnClearAll;
    private JButton btnLeaveRoom;

    private DrawingPanel drawingPanel;
    private ChatPanel chatPanel;
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

        drawingPanel = new DrawingPanel(this);
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel drawContainer = new JPanel(new BorderLayout());
        drawContainer.setOpaque(false);
        drawContainer.add(drawingPanel, BorderLayout.CENTER);

        JPanel toolPanel = createToolPanel();
        drawContainer.add(toolPanel, BorderLayout.SOUTH);

        centerSplitPane.setLeftComponent(drawContainer);

        chatPanel = new ChatPanel(userName, currentRoom, emojiMap, this, this);
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

        btnStartGame = new UIComponents.RoundedButton("게임 시작");
        btnStartGame.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnStartGame.setPreferredSize(new Dimension(250, 50));
        btnStartGame.setBackground(new Color(220, 53, 69));  // 빨간색
        btnStartGame.setForeground(Color.WHITE);             // 흰색 글씨
        btnStartGame.setFocusPainted(false);
        btnStartGame.setBorderPainted(false);

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
                btnStartGame.setBackground(new Color(220, 53, 69));  // 빨간색
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


    @Override
    public void sendProtocol(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            if (chatPanel != null) {
                chatPanel.appendSystemMessage("메시지 전송 실패");
            }
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
        chatPanel.getTxtInput().setEnabled(canInteract);
        chatPanel.getBtnSend().setEnabled(canInteract);

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

                        List<String> newPlayerList = new ArrayList<>();
                        for(String p : players){
                            if(!p.trim().isEmpty()){
                                newPlayerList.add(p);
                            }
                        }
                        currentRoom.updatePlayers(newPlayerList);
                        updatePlayerList(currentRoom.getPlayers());
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

                                chatPanel.appendSystemMessage("===== 게임이 시작되었습니다! =====");
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
                            chatPanel.appendSystemMessage("===== 게임이 종료되었습니다! 투표를 시작합니다. =====");
                            openVotingUI();
                        });

                        break;
                    }
                    else if (msg.startsWith("/playerJoined ")) {
                        String newPlayer = msg.substring(14);
                        SwingUtilities.invokeLater(() -> {
                            currentRoom.addPlayer(newPlayer);
                            chatPanel.appendSystemMessage("[입장] " + newPlayer + "님이 입장했습니다.");
                            updatePlayerList(currentRoom.getPlayers());
                        });
                    }
                    else if (msg.startsWith("/playerLeft ")) {
                        String leftPlayer = msg.substring(12);
                        SwingUtilities.invokeLater(() -> {
                            currentRoom.removePlayer(leftPlayer);
                            chatPanel.appendSystemMessage("[퇴장] " + leftPlayer + "님이 퇴장했습니다.");
                            updatePlayerList(currentRoom.getPlayers());
                        });
                    }
                    else if (msg.startsWith("/hostChanged ")) {
                        String newHostName = msg.substring(13);
                        SwingUtilities.invokeLater(() -> {
                            currentRoom.setHostName(newHostName);
                            isHost = userName.equals(newHostName);
                            chatPanel.appendSystemMessage("[시스템] 방장이 " + newHostName + "님으로 변경되었습니다.");
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
                        chatPanel.appendChatMessage(msg);
                    }
                } catch (IOException e) {
                    if(isRunning){
                        chatPanel.appendSystemMessage("서버와의 연결이 끊어졌습니다.");
                    }
                    break;
                }
            }
            System.out.println("[Client] ListenNetwork 스레드 종료");
        }
    }

    public ImageIcon resizeIcon(String path, int width, int height) {
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

    @Override
    public Color getCurrentColor() {
        return currentColor;
    }

    @Override
    public int getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    public Color getDrawingBgColor() {
        return DRAWING_BG_COLOR;
    }
}
