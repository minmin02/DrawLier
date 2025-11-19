package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.net.Socket;

/**
 * 게임 클라이언트 뷰
 * 수정사항: 방장 권한에 따른 '게임 시작' 버튼 제어 로직 강화
 */
public class JavaChatClientView extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtInput;
    private JTextArea textArea;

    // DrawingPanel 변수 선언
    private DrawingPanel drawingPanel;

    private JButton btnSend;
    private JButton btnStartGame;
    private JLabel[] playerLabels;
    private JLabel lblRoomInfo;
    private JLabel lblTimer;
    private JPanel playerPanel;

    private String userName;
    private GameRoom currentRoom;
    private boolean isHost; // 내가 방장인지 여부

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private Timer gameTimer;
    private int remainingTime;

    // 그리기 색상 (기본 검정)
    private Color currentColor = Color.BLACK;

    public JavaChatClientView(String userName, Socket socket, DataInputStream dis,
                              DataOutputStream dos, GameRoom room, boolean isHost) {
        this.userName = userName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.currentRoom = room;
        this.isHost = isHost;
        this.remainingTime = room.getTimeLimit();

        initializeUI();

        // 수신 스레드 시작
        new ListenNetwork().start();
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

        // 상단 패널
        JPanel topPanel = createTopPanel();
        contentPane.add(topPanel, BorderLayout.NORTH);

        // 중앙 패널 (그리기 + 채팅)
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplitPane.setResizeWeight(0.7);

        // 그리기 패널 생성
        drawingPanel = new DrawingPanel();
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 그리기 도구 패널
        JPanel drawContainer = new JPanel(new BorderLayout());
        drawContainer.add(drawingPanel, BorderLayout.CENTER);
        drawContainer.add(createToolPanel(), BorderLayout.SOUTH);

        centerSplitPane.setLeftComponent(drawContainer);

        JPanel chatPanel = createChatPanel();
        centerSplitPane.setRightComponent(chatPanel);

        contentPane.add(centerSplitPane, BorderLayout.CENTER);

        // 우측 플레이어 패널
        playerPanel = createPlayerPanel();
        contentPane.add(playerPanel, BorderLayout.EAST);

        // 하단 패널 (게임 시작 버튼 등)
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

    private JPanel createToolPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(240, 240, 240));

        JButton btnBlack = new JButton("검정");
        btnBlack.setBackground(Color.BLACK);
        btnBlack.setForeground(Color.WHITE);
        btnBlack.addActionListener(e -> currentColor = Color.BLACK);

        JButton btnRed = new JButton("빨강");
        btnRed.setBackground(Color.RED);
        btnRed.setForeground(Color.WHITE);
        btnRed.addActionListener(e -> currentColor = Color.RED);

        JButton btnBlue = new JButton("파랑");
        btnBlue.setBackground(Color.BLUE);
        btnBlue.setForeground(Color.WHITE);
        btnBlue.addActionListener(e -> currentColor = Color.BLUE);

        JButton btnEraser = new JButton("전체 지우기");
        btnEraser.addActionListener(e -> {
            drawingPanel.clear();
            sendProtocol("/clear");
        });

        panel.add(btnBlack);
        panel.add(btnRed);
        panel.add(btnBlue);
        panel.add(btnEraser);

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
            } else {
                playerLabels[i].setText("대기 중...");
                playerLabels[i].setBackground(new Color(230, 230, 230));
            }

            slotPanel.add(playerLabels[i], BorderLayout.CENTER);
            panel.add(slotPanel);
        }

        return panel;
    }

    // [수정됨] 하단 패널 및 게임 시작 버튼 로직
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        btnStartGame = new JButton("게임 시작");
        btnStartGame.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnStartGame.setPreferredSize(new Dimension(250, 50));
        btnStartGame.setBackground(new Color(40, 167, 69));
        btnStartGame.setForeground(Color.WHITE);
        btnStartGame.setFocusPainted(false);

        // 초기 버튼 상태 설정
        if (isHost) {
            if (currentRoom.getCurrentPlayers() < 4) {
                btnStartGame.setEnabled(false);
                btnStartGame.setText("4명이 모여야 시작 가능");
            } else {
                btnStartGame.setEnabled(true);
                btnStartGame.setText("게임 시작");
            }
        } else {
            // 방장이 아니면 무조건 비활성화
            btnStartGame.setEnabled(false);
            btnStartGame.setText("방장이 게임을 시작합니다");
        }

        btnStartGame.addActionListener(e -> startGame());

        panel.add(btnStartGame);

        return panel;
    }

    private void disconnect() {
        try {
            if (gameTimer != null) {
                gameTimer.stop();
            }
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
        // 클라이언트 측에서도 한 번 더 검사
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

    private void startTimer() {
        if (gameTimer != null && gameTimer.isRunning()) return;

        gameTimer = new Timer(1000, e -> {
            remainingTime--;
            lblTimer.setText("남은 시간: " + formatTime(remainingTime));

            if (remainingTime <= 0) {
                gameTimer.stop();
                appendText("===== 시간 종료! =====");
            }
        });
        gameTimer.start();
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void appendText(String msg) {
        textArea.append(msg + "\n");
        textArea.setCaretPosition(textArea.getText().length());
    }

    // [중요 수정] 플레이어 입장/퇴장 시 버튼 상태 업데이트 로직
    private void updatePlayerList(java.util.List<String> players) {
        SwingUtilities.invokeLater(() -> {
            // 불필요한 데이터 조작 코드를 삭제하고, UI 갱신에만 집중합니다.

            // 1. UI 리스트 갱신
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

            // 2. 방장 여부 및 인원수에 따른 버튼 상태 제어
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
                            startTimer();
                            btnStartGame.setEnabled(false);
                            btnStartGame.setText("게임 진행 중");
                        });
                    }
                    // [여기 수정] 플레이어 입장 처리 로직
                    else if (msg.startsWith("/playerJoined ")) {
                        String newPlayer = msg.substring(14);

                        // 1. 일단 무조건 추가 시도 (중복 체크는 GameRoom 내부에서 처리됨)
                        currentRoom.addPlayer(newPlayer);

                        // 2. 로그 출력
                        appendText("[입장] " + newPlayer + "님이 입장했습니다.");

                        // 3. UI 갱신 (반드시 현재 룸의 최신 리스트를 넘겨야 함)
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

                screenGraphic.setColor(new Color(r, g, b));
                screenGraphic.setStroke(new BasicStroke(2));
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
                prevX = e.getX();
                prevY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                checkImageBuffer();
                int currX = e.getX();
                int currY = e.getY();

                screenGraphic.setColor(currentColor);
                screenGraphic.setStroke(new BasicStroke(2));
                screenGraphic.drawLine(prevX, prevY, currX, currY);
                repaint();

                String msg = String.format("/draw %d %d %d %d %d %d %d",
                        prevX, prevY, currX, currY,
                        currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
                sendProtocol(msg);

                prevX = currX;
                prevY = currY;
            }
        }
    }
}