package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * 게임 클라이언트 뷰 (그리기 기능 및 채팅)
 * 수정사항:
 * 1. 방 나가기 버튼 클릭 시, 부모 창(방 목록)으로 복귀하는 콜백 로직 구현.
 * 2. 방장이라도 /roomDelete 대신 /leaveRoom 프로토콜을 전송하도록 수정 (서버에 위임).
 * 3. 그리기 굵기 선택 및 턴 제어 로직 유지.
 */
public class JavaChatClientView extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtInput;
    private JTextArea textArea;

    private DrawingPanel drawingPanel;

    private JButton btnSend;
    private JButton btnStartGame;
    private JLabel[] playerLabels;
    private JLabel lblRoomInfo;
    private JLabel lblTimer;
    private JPanel playerPanel;
    private JButton btnColorPicker; // 색상 선택 버튼 필드 추가 (툴 강조용)
    private JButton btnEraserTool;
    private JButton btnLeaveRoom; // 방 나가기 버튼 필드 추가

    // [콜백 인터페이스 정의]
    public interface RoomLeaveListener {
        void onRoomLeft();
    }
    private final RoomLeaveListener roomLeaveListener; // 방 나가기 후 호출할 리스너

    private Color currentColor = Color.BLACK;
    private int strokeWidth = 2; // 그리기 굵기 (기본값: 2)
    private final Color DRAWING_BG_COLOR = Color.WHITE;

    private String userName;
    private GameRoom currentRoom;
    private boolean isHost;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private Timer gameTimer;
    private int totalTimeLimit;
    private int remainingTime;

    private int turnTimeLimit;
    private int currentTurnIndex = -1;
    private int roundCount = 0;
    private boolean isMyTurn = false;

    // [생성자 수정: 리스너 추가]
    public JavaChatClientView(String userName, Socket socket, DataInputStream dis,
                              DataOutputStream dos, GameRoom room, boolean isHost,
                              RoomLeaveListener listener) {
        this.userName = userName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.currentRoom = room;
        this.isHost = isHost;
        this.totalTimeLimit = room.getTimeLimit();
        this.roomLeaveListener = listener; // 리스너 저장

        this.turnTimeLimit = totalTimeLimit / 8;
        this.remainingTime = this.totalTimeLimit;

        initializeUI();

        new ListenNetwork().start();
    }

    private void initializeUI() {
        setTitle("DrawLier - " + currentRoom.getRoomName() + " [" + userName + "]");
        // EXIT_ON_CLOSE 대신 DO_NOTHING_ON_CLOSE로 설정하여, windowClosing에서 수동 처리
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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

        // 창 닫기 버튼(X) 클릭 시 방 나가기 로직 수행
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectAndClose();
            }
        });
    }


    private void updateToolButtons(JButton activeTool) {
        if (btnColorPicker != null) btnColorPicker.setBorder(UIManager.getBorder("Button.border"));
        if (btnEraserTool != null) btnEraserTool.setBorder(UIManager.getBorder("Button.border"));

        if (activeTool != null) {
            activeTool.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 3));
        }
    }

    private JPanel createToolPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(240, 240, 240));

        // --- 굵기 선택 콤보박스 ---
        String[] widths = {"1", "2", "4", "8", "12"};
        JComboBox<String> strokeSelector = new JComboBox<>(widths);
        strokeSelector.setSelectedItem("2");
        strokeSelector.addActionListener(e -> {
            try {
                strokeWidth = Integer.parseInt((String) strokeSelector.getSelectedItem());
            } catch (NumberFormatException ex) {
                strokeWidth = 2;
            }
        });

        panel.add(new JLabel("굵기:"));
        panel.add(strokeSelector);

        // --- 색상 선택 버튼 (JColorChooser 호출) ---
        btnColorPicker = new JButton("색상 선택");
        btnColorPicker.setPreferredSize(new Dimension(120, 30));

        btnColorPicker.setBackground(currentColor);
        btnColorPicker.setForeground(Color.WHITE);
        btnColorPicker.setOpaque(true);
        btnColorPicker.setBorderPainted(true);
        updateToolButtons(btnColorPicker);

        btnColorPicker.addActionListener(e -> {
            updateToolButtons(btnColorPicker);

            final JColorChooser colorChooser = new JColorChooser(currentColor);
            colorChooser.setPreviewPanel(new JPanel());

            AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
            for (AbstractColorChooserPanel ccp : panels) {
                if (!ccp.getDisplayName().equals("Swatches")) {
                    colorChooser.removeChooserPanel(ccp);
                }
            }

            JDialog dialog = JColorChooser.createDialog(
                    this,
                    "색상 팔레트",
                    true,
                    colorChooser,
                    a -> {
                        Color selectedColor = colorChooser.getColor();
                        if (selectedColor != null) {
                            currentColor = selectedColor;
                            btnColorPicker.setBackground(currentColor);
                            btnColorPicker.setForeground(Color.WHITE);
                        }
                    },
                    b -> {}
            );
            dialog.setVisible(true);
        });

        // --- 지우개 툴 버튼 (부분 지우개) ---
        btnEraserTool = new JButton("지우개");
        btnEraserTool.setPreferredSize(new Dimension(80, 30));
        btnEraserTool.addActionListener(e -> {
            currentColor = DRAWING_BG_COLOR;
            updateToolButtons(btnEraserTool);
        });

        // --- 전체 지우기 버튼 ---
        JButton btnClearAll = new JButton("전체 지우기");
        btnClearAll.addActionListener(e -> {
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

        lblTimer = new JLabel("남은 시간: " + formatTime(remainingTime));
        lblTimer.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblTimer.setForeground(new Color(220, 53, 69));
        panel.add(lblTimer, BorderLayout.EAST);

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
        panel.setBorder(BorderFactory.createTitledBorder("플레이어 (" +
                currentRoom.getCurrentPlayers() + "/" + currentRoom.getMaxPlayers() + ")"));
        panel.setPreferredSize(new Dimension(200, 0));

        playerLabels = new JLabel[4];
        java.util.List<String> players = currentRoom.getPlayers();

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

                if (i == currentTurnIndex && roundCount > 0) {
                    playerLabels[i].setBackground(new Color(255, 255, 150));
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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panel.setOpaque(false);

        btnStartGame = new JButton("게임 시작");
        btnStartGame.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnStartGame.setPreferredSize(new Dimension(250, 50));
        btnStartGame.setForeground(Color.WHITE);
        btnStartGame.setFocusPainted(false);

        btnLeaveRoom = new JButton("방 나가기");
        btnLeaveRoom.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnLeaveRoom.setPreferredSize(new Dimension(150, 50));
        btnLeaveRoom.setBackground(new Color(220, 53, 69));
        btnLeaveRoom.setForeground(Color.WHITE);
        btnLeaveRoom.setFocusPainted(false);
        btnLeaveRoom.addActionListener(e -> disconnectAndClose());


        updateStartButtonState(currentRoom.getPlayers());

        btnStartGame.addActionListener(e -> startGame());

        panel.add(btnStartGame);
        panel.add(btnLeaveRoom);

        return panel;
    }

    // [수정] 방장이든 아니든 /leaveRoom을 서버에 전송하고 창을 닫고 복귀
    private void disconnectAndClose() {
        // 방장 여부와 관계없이 서버에 퇴장 메시지를 보냅니다.
        sendProtocol("/leaveRoom");

        // UI 정리 및 소켓 종료
        disconnect();
        // 부모 창으로 복귀를 위해 리스너 호출
        if (roomLeaveListener != null) {
            roomLeaveListener.onRoomLeft();
        }
        dispose();
    }

    private void disconnect() {
        try {
            if (gameTimer != null) {
                gameTimer.stop();
            }
            if (dos != null) dos.close();
            if (dis != null) dis.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;

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
        if (currentRoom.getCurrentPlayers() < 4) {
            JOptionPane.showMessageDialog(this, "4명이 모여야 게임을 시작할 수 있습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        sendProtocol("/gameStart");
    }

    private void startTurnTimer(int turnTime) {
        if (gameTimer != null) {
            gameTimer.stop();
        }

        remainingTime = turnTime;

        gameTimer = new Timer(1000, e -> {
            remainingTime--;
            lblTimer.setText(String.format("라운드 %d - %s 턴 | 남은 시간: %s",
                    roundCount, currentRoom.getPlayers().get(currentTurnIndex), formatTime(remainingTime)));

            if (remainingTime <= 0) {
                gameTimer.stop();
                appendText("===== 턴 종료! =====");
                sendProtocol("/turnEnd");
            }
        });
        gameTimer.start();
    }

    private void updateStartButtonState(List<String> players) {
        if (btnStartGame == null) return;

        if (isHost) {
            if (players.size() >= 4) {
                btnStartGame.setEnabled(true);
                btnStartGame.setText("게임 시작");
                btnStartGame.setBackground(new Color(40, 167, 69));
            } else {
                btnStartGame.setEnabled(false);
                btnStartGame.setText("4명이 모여야 시작 가능 (" + players.size() + "/4)");
                btnStartGame.setBackground(Color.GRAY);
            }
        } else {
            btnStartGame.setEnabled(false);
            btnStartGame.setText("방장이 게임을 시작합니다");
            btnStartGame.setBackground(Color.GRAY);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void appendText(String msg) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(msg + "\n");
            textArea.setCaretPosition(textArea.getText().length());
        });
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

                    if (i == currentTurnIndex && roundCount > 0) {
                        playerLabels[i].setBackground(new Color(255, 255, 150));
                    }

                } else {
                    playerLabels[i].setText("대기 중...");
                    playerLabels[i].setBackground(new Color(230, 230, 230));
                }
            }

            playerPanel.setBorder(BorderFactory.createTitledBorder(
                    "플레이어 (" + players.size() + "/" + currentRoom.getMaxPlayers() + ")"));

            updateStartButtonState(players);
        });
    }

    class ListenNetwork extends Thread {
        public void run() {
            while (true) {
                try {
                    String msg = dis.readUTF();

                    if (msg.startsWith("/gameStart")) {
                        SwingUtilities.invokeLater(() -> {
                            appendText("===== 게임이 시작되었습니다! =====");
                            btnStartGame.setEnabled(false);
                            btnStartGame.setText("게임 진행 중");
                        });
                    }
                    else if (msg.startsWith("/startTurn ")) {
                        String[] parts = msg.split(" ");
                        int turnIndex = Integer.parseInt(parts[1]);
                        roundCount = Integer.parseInt(parts[2]);
                        currentTurnIndex = turnIndex;

                        SwingUtilities.invokeLater(() -> {
                            String player = currentRoom.getPlayers().get(turnIndex);
                            isMyTurn = userName.equals(player);

                            appendText(String.format("===== 라운드 %d, %s 턴 시작! =====", roundCount, player));

                            drawingPanel.clear();

                            startTurnTimer(turnTimeLimit);

                            updatePlayerList(currentRoom.getPlayers());
                        });
                    }
                    // 방 삭제 프로토콜 수신 (서버가 마지막 플레이어 퇴장 또는 강제 삭제 시 보냄)
                    else if (msg.startsWith("/roomDeleted")) {
                        JOptionPane.showMessageDialog(JavaChatClientView.this,
                                "방이 해체되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                        disconnect();
                        dispose();
                        // 이 경우, onRoomLeft()는 이미 호출되었거나, RoomListUI가 스스로 갱신해야 합니다.
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
                        // [중요] GameRoom 내부에서 방장 위임 로직 처리
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
                    else if (msg.startsWith("/loginOK")) {
                        // pass
                    }
                    else {
                        appendText(msg);
                    }
                } catch (IOException e) {
                    appendText("서버와의 연결이 끊어졌습니다.");
                    break;
                }
            }
        }
    }

    /**
     * DrawingPanel 클래스 정의
     */
    class DrawingPanel extends JPanel {
        private Image screenImage;
        private Graphics2D screenGraphic;
        private int prevX, prevY;

        public DrawingPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(600, 500));

            MyMouseListener mm = new MyMouseListener();
            addMouseListener(mm);
            addMouseMotionListener(mm);
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
                int width = Integer.parseInt(parts[8]); // 굵기 정보 추가

                screenGraphic.setColor(new Color(r, g, b));
                screenGraphic.setStroke(new BasicStroke(width)); // 굵기 적용
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
                if (!isMyTurn) return; // [제어] 내 턴이 아니면 그리기 불가
                prevX = e.getX();
                prevY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isMyTurn) return; // [제어] 내 턴이 아니면 그리기 불가

                checkImageBuffer();
                int currX = e.getX();
                int currY = e.getY();

                screenGraphic.setColor(currentColor);
                screenGraphic.setStroke(new BasicStroke(strokeWidth)); // 굵기 적용
                screenGraphic.drawLine(prevX, prevY, currX, currY);
                repaint();

                // RGB 값과 굵기(strokeWidth) 값을 프로토콜에 추가하여 전송
                String msg = String.format("/draw %d %d %d %d %d %d %d %d",
                        prevX, prevY, currX, currY,
                        currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), strokeWidth);
                sendProtocol(msg);

                prevX = currX;
                prevY = currY;
            }
        }
    }
}