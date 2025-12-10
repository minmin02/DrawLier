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

/**
 * 게임 메인 화면 클래스
 * - 그림 그리기 패널, 채팅 패널, 플레이어 목록 등을 관리
 * - 서버와 통신하여 게임 진행 상태를 동기화
 */
public class JavaChatClientView extends JFrame implements DrawingPanel.DrawingCallback, ChatPanel.ChatCallback {

    private static final long serialVersionUID = 1L;

    // UI 컴포넌트
    private JPanel contentPane;
    private JButton btnClearAll;
    private JButton btnLeaveRoom;

    private DrawingPanel drawingPanel; // 그림 그리기 패널
    private ChatPanel chatPanel; // 채팅 패널
    private JButton btnStartGame; // 게임 시작 버튼
    private JLabel[] playerLabels; // 플레이어 목록 라벨 배열
    private JLabel lblRoomInfo;
    private JLabel lblTimer; // 남은 시간 표시 라벨
    private JLabel lblCurrentTurn; // 현재 턴 표시 라벨
    private JPanel playerPanel; // 플레이어 목록 패널
    private JLabel lblHostName; // 방장 이름 라벨

    // 사용자 정보
    private String userName; // 현재 사용자 닉네임
    private GameRoom currentRoom; // 현재 게임방 정보
    private boolean isHost; // 방장 여부

    // 네트워크 통신
    private Socket socket; // 서버 연결 소켓
    private DataInputStream dis; // 입력 스트림
    private DataOutputStream dos; // 출력 스트림
    private String serverIp;
    private String serverPort;

    // 그림 그리기 도구
    private JButton btnColorPicker; // 색상 선택 버튼
    private JButton btnEraserTool; // 지우개 버튼

    private Color currentColor = Color.BLACK; // 현재 선택된 색상
    private int strokeWidth = 2; // 펜 굵기
    private final Color DRAWING_BG_COLOR = Color.WHITE; // 그림판 배경색
    private boolean isRunning = true; // 네트워크 스레드 실행 여부

    // 게임 상태
    private boolean isLiar; // 라이어 역할 여부
    private String myKeyword; // 받은 키워드 (라이어는 카테고리, 시민은 제시어)

    private Map<String, ImageIcon> emojiMap; // 이모지 이미지 맵


    /**
     * [실행 흐름 1] 게임 화면 생성자
     * - RoomListUI에서 방에 입장하면 호출됨
     * - 사용자 정보, 소켓, 방 정보를 초기화하고 UI 구성
     *
     * @param userName 사용자 닉네임
     * @param socket 서버와의 연결 소켓
     * @param dis 서버로부터 데이터를 받는 입력 스트림
     * @param dos 서버로 데이터를 보내는 출력 스트림
     * @param room 현재 게임방 정보
     * @param isHost 방장 여부
     * @param serverIp 서버 IP 주소
     * @param serverPort 서버 포트 번호
     */
    public JavaChatClientView(String userName, Socket socket, DataInputStream dis,
                              DataOutputStream dos, GameRoom room, boolean isHost, String serverIp, String serverPort) {
        // [실행 흐름 1-1] 멤버 변수 초기화
        this.userName = userName;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.currentRoom = room;
        this.isHost = isHost;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        // [실행 흐름 1-2] 이모지 이미지 로드
        loadEmojis();

        // [실행 흐름 1-3] UI 초기화 (패널, 버튼, 레이블 등 생성)
        initializeUI();

        // [실행 흐름 1-4] 서버 메시지 수신 스레드 시작
        new ListenNetwork().start();

        // [실행 흐름 1-5] 플레이어 목록 UI 업데이트
        SwingUtilities.invokeLater(() -> {
            updatePlayerList(currentRoom.getPlayers());
        });
    }

    /**
     * [실행 흐름 2] 이모지 이미지 로드
     * - /Imoji 폴더의 모든 PNG, GIF 파일을 읽어 HashMap에 저장
     * - 파일명을 키로 사용하여 이모지 전송 시 참조
     */
    private void loadEmojis() {
        emojiMap = new HashMap<>();
        String path = "/Imoji";
        URL dirURL = getClass().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            try {
                // 디렉토리 내 모든 파일 읽기
                File[] files = new File(dirURL.toURI()).listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        // PNG 파일만 처리
                        if (fileName.toLowerCase().endsWith(".png")) {
                            // 확장자를 제외한 파일명을 키로 사용
                            String emojiKey = fileName.substring(0, fileName.lastIndexOf('.'));
                            ImageIcon icon = new ImageIcon(file.toURI().toURL());
                            // 이미지를 32x32 크기로 조정
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


    /**
     * [실행 흐름 3] UI 초기화
     * - 전체 화면 레이아웃 구성
     * - 상단 패널, 그리기 패널, 채팅 패널, 플레이어 패널, 하단 버튼 패널 생성
     */
    private void initializeUI() {
        // [실행 흐름 3-1] 기본 윈도우 설정
        setTitle("DrawLier - " + currentRoom.getRoomName() + " [" + userName + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1200, 800);

        // [실행 흐름 3-2] 배경 이미지가 있는 메인 패널 생성
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

        // [실행 흐름 3-3] 상단 패널 생성
        JPanel topPanel = createTopPanel();
        topPanel.setOpaque(false);
        contentPane.add(topPanel, BorderLayout.NORTH);

        // [실행 흐름 3-4] 중앙 분할 패널 생성
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplitPane.setResizeWeight(0.2);
        centerSplitPane.setEnabled(false);
        centerSplitPane.setOpaque(false);
        centerSplitPane.setBorder(null);

        // [실행 흐름 3-5] 그리기 패널 생성
        drawingPanel = new DrawingPanel(this);
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel drawContainer = new JPanel(new BorderLayout());
        drawContainer.setOpaque(false);
        drawContainer.add(drawingPanel, BorderLayout.CENTER);

        // [실행 흐름 3-6] 도구 패널 생성
        JPanel toolPanel = createToolPanel();
        drawContainer.add(toolPanel, BorderLayout.SOUTH);

        centerSplitPane.setLeftComponent(drawContainer);

        // [실행 흐름 3-7] 채팅 패널 생성
        chatPanel = new ChatPanel(userName, currentRoom, emojiMap, this, this);
        centerSplitPane.setRightComponent(chatPanel);

        centerSplitPane.setDividerLocation(0.38);
        contentPane.add(centerSplitPane, BorderLayout.CENTER);

        // [실행 흐름 3-8] 플레이어 목록 패널 생성
        playerPanel = createPlayerPanel();
        playerPanel.setOpaque(false);
        contentPane.add(playerPanel, BorderLayout.EAST);

        // [실행 흐름 3-9] 하단 게임 시작 버튼 패널 생성
        JPanel bottomPanel = createBottomPanel();
        bottomPanel.setOpaque(false);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // 화면 중앙에 배치

        // [실행 흐름 3-10] 윈도우 종료 이벤트 리스너 등록
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect(); // 서버 연결 종료
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


    /**
     * [실행 흐름 5] 서버로 메시지 전송
     * - DrawingPanel, ChatPanel에서 콜백으로 호출
     * - 채팅 메시지, 그리기 명령 등을 서버로 전송
     *
     * @param msg 전송할 메시지 (프로토콜 형식)
     */
    @Override
    public void sendProtocol(String msg) {
        try {
            dos.writeUTF(msg); // 서버로 메시지 전송
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


    /**
     * [실행 흐름 4] 서버 메시지 수신 스레드
     * - 서버로부터 지속적으로 메시지를 수신하여 처리
     * - 게임 상태 업데이트, 채팅 메시지, 그리기 명령 등 처리
     */
    class ListenNetwork extends Thread {
        public void run() {
            while (isRunning) {
                try {
                    // [실행 흐름 4-1] 서버로부터 메시지 수신 (블로킹)
                    String msg = dis.readUTF();

                    // [실행 흐름 4-2] 플레이어 목록 업데이트 메시지 처리
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
                        updatePlayerList(currentRoom.getPlayers()); // UI 업데이트
                    }

                    // [실행 흐름 4-3] 게임 시작 메시지 처리 (역할 배정)
                    else if (msg.startsWith("/gameStart ")) {
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
                    // [실행 흐름 4-4] 게임 상태 업데이트 메시지 처리 (턴, 라운드, 타이머)
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
                    // [실행 흐름 4-5] 게임 종료 메시지 처리 (투표 화면으로 전환)
                    else if (msg.startsWith("/gameEnded")) {
                        System.out.println("[Client] 게임 종료 수신 - 투표 화면으로 전환");

                        //서버로부터 받은 최신 플레이어 리스트로 업데이트
                        String[] parts = msg.split(" ", 2);
                        if (parts.length > 1) {
                            String[] playerArray = parts[1].split(",");
                            List<String> updatedPlayers = new ArrayList<>();
                            for (String p : playerArray) {
                                if (!p.trim().isEmpty()) {
                                    updatedPlayers.add(p.trim());
                                }
                            }
                            currentRoom.updatePlayers(updatedPlayers);
                            System.out.println("[Client] 플레이어 리스트 업데이트: " + updatedPlayers);
                        }

                        isRunning = false;

                        SwingUtilities.invokeLater(() -> {
                            chatPanel.appendSystemMessage("===== 게임이 종료되었습니다! 투표를 시작합니다. =====");
                            openVotingUI(); // 투표 UI 열기
                        });

                        break;
                    }
                    // [실행 흐름 4-6] 플레이어 입장 메시지 처리
                    else if (msg.startsWith("/playerJoined ")) {
                        String newPlayer = msg.substring(14);
                        SwingUtilities.invokeLater(() -> {
                            currentRoom.addPlayer(newPlayer);
                            chatPanel.appendSystemMessage("[입장] " + newPlayer + "님이 입장했습니다.");
                            updatePlayerList(currentRoom.getPlayers());
                        });
                    }
                    // [실행 흐름 4-7] 플레이어 퇴장 메시지 처리
                    else if (msg.startsWith("/playerLeft ")) {
                        String leftPlayer = msg.substring(12);
                        SwingUtilities.invokeLater(() -> {
                            currentRoom.removePlayer(leftPlayer);
                            chatPanel.appendSystemMessage("[퇴장] " + leftPlayer + "님이 퇴장했습니다.");
                            updatePlayerList(currentRoom.getPlayers());
                        });
                    }
                    // [실행 흐름 4-8] 방장 변경 메시지 처리
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
                    // [실행 흐름 4-9] 그리기 명령 메시지 처리
                    else if (msg.startsWith("/draw ")) {
                        drawingPanel.processDrawCommand(msg); // 다른 플레이어의 그리기 동기화
                    }
                    // [실행 흐름 4-10] 전체 지우기 명령 메시지 처리
                    else if (msg.startsWith("/clear")) {
                        drawingPanel.clear(); // 그림판 초기화
                    }
                    // [실행 흐름 4-11] 일반 채팅 메시지 처리
                    else {
                        chatPanel.appendChatMessage(msg); // 채팅창에 표시
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
