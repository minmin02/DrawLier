package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * 게임 클라이언트 뷰 - 수정됨
 * 중요 수정사항: 게임 종료 시 ListenNetwork 스레드를 즉시 break하여
 * VotingUI가 입력 스트림을 독점할 수 있도록 수정
 */
public class JavaChatClientView extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtInput;
    private JTextArea textArea;
    private JButton btnClearAll;

    private DrawingPanel drawingPanel;

    private JButton btnSend;
    private JButton btnStartGame;
    private JLabel[] playerLabels;
    private JLabel lblRoomInfo;
    private JLabel lblTimer;
    private JLabel lblCurrentTurn;
    private JPanel playerPanel;

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

        initializeUI();
        new ListenNetwork().start();

        SwingUtilities.invokeLater(() -> {
            updatePlayerList(currentRoom.getPlayers());
        });
    }

    private void initializeUI() {
        setTitle("DrawLier - " + currentRoom.getRoomName() + " [" + userName + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1200, 800);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBackground(Color.WHITE);
        setContentPane(contentPane);

        JPanel topPanel = createTopPanel();
        contentPane.add(topPanel, BorderLayout.NORTH);

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplitPane.setResizeWeight(0.7);

        drawingPanel = new DrawingPanel();
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel drawContainer = new JPanel(new BorderLayout());
        drawContainer.add(drawingPanel, BorderLayout.CENTER);
        drawContainer.add(createToolPanel(), BorderLayout.SOUTH);

        centerSplitPane.setLeftComponent(drawContainer);

        JPanel chatPanel = createChatPanel();
        centerSplitPane.setRightComponent(chatPanel);

        contentPane.add(centerSplitPane, BorderLayout.CENTER);

        playerPanel = createPlayerPanel();
        contentPane.add(playerPanel, BorderLayout.EAST);

        JPanel bottomPanel = createBottomPanel();
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
        panel.setBackground(new Color(240, 240, 240));

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

        panel.add(new JLabel("굵기:"));
        panel.add(strokeSelector);

        btnColorPicker = new JButton("색상 선택");
        btnColorPicker.setPreferredSize(new Dimension(120, 30));
        btnColorPicker.setBackground(currentColor);
        btnColorPicker.setForeground(Color.WHITE);
        btnColorPicker.setOpaque(true);
        btnColorPicker.setBorderPainted(true);
        updateToolButtons(btnColorPicker);

        btnColorPicker.addActionListener(e -> {
            if (!drawingPanel.isEnabled()) {
                JOptionPane.showMessageDialog(this, "당신의 턴이 아닙니다!", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            updateToolButtons(btnColorPicker);

            Color newColor = JColorChooser.showDialog(
                    JavaChatClientView.this,
                    "색상 선택",
                    currentColor
            );

            if (newColor != null) {
                currentColor = newColor;
                btnColorPicker.setBackground(currentColor);

                int brightness = (newColor.getRed() + newColor.getGreen() + newColor.getBlue()) / 3;
                btnColorPicker.setForeground(brightness > 128 ? Color.BLACK : Color.WHITE);
            }
        });

        btnEraserTool = new JButton("지우개");
        btnEraserTool.setPreferredSize(new Dimension(80, 30));
        btnEraserTool.addActionListener(e -> {
            if (!drawingPanel.isEnabled()) {
                JOptionPane.showMessageDialog(this, "당신의 턴이 아닙니다!", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentColor = DRAWING_BG_COLOR;
            updateToolButtons(btnEraserTool);
        });

        btnClearAll = new JButton("전체 지우기");
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

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        lblRoomInfo = new JLabel(String.format("[%s] 카테고리: %s | 방장: %s",
                currentRoom.getRoomName(),
                currentRoom.getCategory(),
                currentRoom.getHostName()));
        lblRoomInfo.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        panel.add(lblRoomInfo, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.setOpaque(false);

        lblCurrentTurn = new JLabel("게임 준비 중...");
        lblCurrentTurn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblCurrentTurn.setForeground(new Color(0, 102, 204));
        rightPanel.add(lblCurrentTurn);

        lblTimer = new JLabel("남은 시간: 15초");
        lblTimer.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblTimer.setForeground(new Color(220, 53, 69));
        rightPanel.add(lblTimer);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("채팅"));

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        txtInput = new JTextField();
        txtInput.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        txtInput.addActionListener(e -> sendMessage());

        btnSend = new JButton("전송");
        btnSend.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btnSend.addActionListener(e -> sendMessage());

        inputPanel.add(txtInput, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        java.util.List<String> players = currentRoom.getPlayers();
        panel.setBorder(BorderFactory.createTitledBorder("플레이어 (" + players.size() + "/4)"));
        panel.setPreferredSize(new Dimension(200, 0));

        playerLabels = new JLabel[4];

        for (int i = 0; i < 4; i++) {
            JPanel slotPanel = new JPanel(new BorderLayout());
            slotPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            slotPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            playerLabels[i] = new JLabel();
            playerLabels[i].setFont(new Font("맑은 고딕", Font.BOLD, 14));
            playerLabels[i].setOpaque(true);
            playerLabels[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            playerLabels[i].setHorizontalAlignment(SwingConstants.CENTER);

            if (i < players.size()) {
                String playerName = players.get(i);
                playerLabels[i].setText(playerName);
                playerLabels[i].setBackground(new Color(180, 220, 255));

                if (playerName.equals(currentRoom.getHostName())) {
                    playerLabels[i].setText("👑 " + playerName);
                }
            } else {
                playerLabels[i].setText("대기 중...");
                playerLabels[i].setBackground(new Color(230, 230, 230));
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
            appendText("[시스템] 당신의 턴이 아닙니다!");
            txtInput.setText("");
            return;
        }

        sendProtocol(userName + ": " + msg);
        txtInput.setText("");
    }

    private void sendProtocol(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            appendText("메시지 전송 실패");
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

        if (isMyTurn) {
            appendText("[시스템] ⭐ 당신의 턴입니다! 15초 동안 그려주세요!");
        }
    }

    private void appendText(String msg) {
        textArea.append(msg + "\n");
        textArea.setCaretPosition(textArea.getText().length());
    }

    private void updatePlayerList(java.util.List<String> players) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 4; i++) {
                if (i < players.size()) {
                    String playerName = players.get(i);
                    playerLabels[i].setText(playerName);
                    playerLabels[i].setBackground(new Color(180, 220, 255));

                    if (playerName.equals(currentRoom.getHostName())) {
                        playerLabels[i].setText("👑 " + playerName);
                    }
                } else {
                    playerLabels[i].setText("대기 중...");
                    playerLabels[i].setBackground(new Color(230, 230, 230));
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
            // 주의: 여기서 isRunning=false는 이미 늦음. 스레드 내부에서 처리해야 함.
            new VotingUI(userName, players, dos, dis, currentRoom.getRoomId(), serverIp, serverPort);
            dispose();
        });
    }

    class ListenNetwork extends Thread {
        public void run() {
            while (isRunning) {
                try {
                    String msg = dis.readUTF();

                    if (msg.startsWith("/gameStart ")) {
                        String[] parts = msg.substring(11).split("\\|");
                        if (parts.length >= 2) {
                            String role = parts[0];
                            String info = parts[1];

                            SwingUtilities.invokeLater(() -> {
                                if (role.equals("LIAR")) {
                                    isLiar = true;
                                    myKeyword = info;
                                    JOptionPane.showMessageDialog(JavaChatClientView.this,
                                            "🎭 당신은 라이어입니다! 🎭\n\n" +
                                                    "카테고리: " + info + "\n\n" +
                                                    "다른 사람들의 그림을 보고\n" +
                                                    "키워드를 추측하세요!",
                                            "역할 - 라이어",
                                            JOptionPane.WARNING_MESSAGE);
                                } else if (role.equals("CITIZEN")) {
                                    isLiar = false;
                                    myKeyword = info;
                                    JOptionPane.showMessageDialog(JavaChatClientView.this,
                                            "✅ 당신은 시민입니다! ✅\n\n" +
                                                    "키워드: " + info + "\n\n" +
                                                    "이 키워드를 그려주세요!",
                                            "역할 - 시민",
                                            JOptionPane.INFORMATION_MESSAGE);
                                }

                                appendText("===== 게임이 시작되었습니다! =====");
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
                    // ★★★ 여기가 가장 중요 수정 포인트 ★★★
                    else if (msg.startsWith("/gameEnded")) {
                        System.out.println("[Client] 게임 종료 수신 - 투표 화면으로 전환");
                        isRunning = false; // 루프 조건 해제

                        // UI 전환 예약
                        SwingUtilities.invokeLater(() -> {
                            appendText("===== 게임이 종료되었습니다! 투표를 시작합니다. =====");
                            openVotingUI();
                        });

                        // ★핵심★: 여기서 즉시 break를 걸어야
                        // 이 스레드가 다음 메시지(/voteResult)를 훔쳐가지 않습니다.
                        break;
                    }
                    else if (msg.startsWith("/playerJoined ")) {
                        String newPlayer = msg.substring(14);
                        currentRoom.addPlayer(newPlayer);
                        appendText("[입장] " + newPlayer + "님이 입장했습니다.");
                        updatePlayerList(currentRoom.getPlayers());
                    }
                    else if (msg.startsWith("/playerLeft ")) {
                        String leftPlayer = msg.substring(12);
                        currentRoom.removePlayer(leftPlayer);
                        appendText("[퇴장] " + leftPlayer + "님이 퇴장했습니다.");
                        updatePlayerList(currentRoom.getPlayers());
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
                        appendText("서버와의 연결이 끊어졌습니다.");
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
}