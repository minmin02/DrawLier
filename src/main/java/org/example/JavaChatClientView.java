package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
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
    private JButton btnLeaveRoom; //방 나가기 버튼
    private JButton btnEmoji; //이모지 버튼

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

        //방 나가기 버튼
        btnLeaveRoom = new JButton("나가기");
        btnLeaveRoom.setPreferredSize(new Dimension(80, 30));
        btnLeaveRoom.setBackground(new Color(220, 53, 69));
        btnLeaveRoom.setFocusPainted(false);
        btnLeaveRoom.addActionListener(e -> leaveRoom());
        panel.add(btnLeaveRoom);

        //구분선
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 25));
        panel.add(separator);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

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
        btnColorPicker.setPreferredSize(new Dimension(100, 30));
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

    //방 나가기 로직
    private void leaveRoom() {
        try{
            if(dos != null){
                dos.writeUTF("/leaveRoom");
            }
            isRunning = false;
            SwingUtilities.invokeLater(() -> {
                new RoomListUI(userName, serverIp, serverPort).setVisible(true);
                dispose();
            });
        }catch(IOException ex){
            ex.printStackTrace();
        }
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


    /**
     * 한글과 이모지를 모두 지원하는 폰트 생성
     * Windows의 맑은 고딕은 한글과 기본 유니코드 이모지를 모두 지원
     */
    private Font createKoreanFont(int size) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        // 1순위: 맑은 고딕 (한글 + 이모지 지원)
        // Windows에서 기본 제공되며 한글과 유니코드 이모지를 모두 표시 가능
        for (String font : availableFonts) {
            if (font.equals("맑은 고딕") || font.equals("Malgun Gothic")) {
                return new Font("맑은 고딕", Font.PLAIN, size);
            }
        }

        // 2순위: Apple SD Gothic Neo (macOS 한글 폰트)
        for (String font : availableFonts) {
            if (font.contains("Apple SD Gothic") || font.contains("AppleSDGothicNeo")) {
                return new Font(font, Font.PLAIN, size);
            }
        }

        // 3순위: 나눔고딕 (한글 폰트)
        for (String font : availableFonts) {
            if (font.contains("나눔고딕") || font.contains("NanumGothic")) {
                return new Font(font, Font.PLAIN, size);
            }
        }

        // 4순위: 굴림 (Windows 기본 한글 폰트)
        for (String font : availableFonts) {
            if (font.equals("굴림") || font.equals("Gulim")) {
                return new Font("굴림", Font.PLAIN, size);
            }
        }

        // 최종 폴백: Dialog 폰트 (시스템이 자동으로 적절한 폰트 선택)
        return new Font(Font.DIALOG, Font.PLAIN, size);
    }

    /**
     * 이모지 표시를 위한 폰트 생성 (이모지 우선)
     * 이모지 버튼, 이모지 선택기 등에 적용
     */
    private Font createEmojiFont(int size) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        // 1순위: Segoe UI Emoji (Windows 이모지 폰트)
        for (String font : availableFonts) {
            if (font.equals("Segoe UI Emoji")) {
                return new Font("Segoe UI Emoji", Font.PLAIN, size);
            }
        }

        // 2순위: Apple Color Emoji (macOS)
        for (String font : availableFonts) {
            if (font.equals("Apple Color Emoji")) {
                return new Font("Apple Color Emoji", Font.PLAIN, size);
            }
        }

        // 3순위: Noto Color Emoji (Linux)
        for (String font : availableFonts) {
            if (font.equals("Noto Color Emoji")) {
                return new Font("Noto Color Emoji", Font.PLAIN, size);
            }
        }

        // 4순위: 맑은 고딕 (일부 이모지 지원)
        for (String font : availableFonts) {
            if (font.equals("맑은 고딕") || font.equals("Malgun Gothic")) {
                return new Font("맑은 고딕", Font.PLAIN, size);
            }
        }

        // 최종 폴백: Dialog 폰트
        return new Font(Font.DIALOG, Font.PLAIN, size);
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("채팅"));

        textArea = new JTextArea();
        textArea.setEditable(false);
        // ✅ 맑은 고딕 사용 (한글 + 이모지 모두 지원)
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        txtInput = new JTextField();
        // ✅ 맑은 고딕 사용
        txtInput.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        txtInput.addActionListener(e -> sendMessage());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        btnEmoji = new JButton("😊");
        // ✅ 맑은 고딕 사용
        btnEmoji.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        btnEmoji.setPreferredSize(new Dimension(50, 30));
        btnEmoji.setToolTipText("이모지 선택");
        btnEmoji.addActionListener(e -> showEmojiPicker());

        btnSend = new JButton("전송");
        btnSend.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btnSend.addActionListener(e -> sendMessage());

        buttonPanel.add(btnEmoji);
        buttonPanel.add(btnSend);

        inputPanel.add(txtInput, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
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
            // ✅ 맑은 고딕 사용
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

    private void showEmojiPicker() {
        JDialog emojiDialog = new JDialog(this, "이모티콘 선택", true);
        emojiDialog.setSize(450, 350);
        emojiDialog.setLocationRelativeTo(this);

        JPanel emojiPanel = new JPanel(new GridLayout(8, 6, 5, 5));
        emojiPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 텍스트 이모티콘 배열 (한글과 호환되는 형식)
        String[][] emoticons = {
                {"^_^", "웃음"},
                {"ㅋㅋㅋ", "크크크"},
                {"ㅎㅎㅎ", "하하하"},
                {"ㅠㅠ", "슬픔"},
                {"ㅜㅜ", "울음"},
                {"^^", "미소"},
                {"^o^", "신남"},
                {"T_T", "눈물"},
                {">_<", "화남"},
                {"O_O", "놀람"},
                {"-_-", "무표정"},
                {"@_@", "어지러움"},
                {"*_*", "반짝"},
                {"♥", "하트"},
                {"★", "별"},
                {"♪", "음표"},
                {"(~_~)", "졸림"},
                {"(^3^)", "뽀뽀"},
                {"(>_<)", "아픔"},
                {"(=^ω^=)", "고양이"},
                {"(╯°□°）╯", "뒤집기"},
                {"¯\\_(ツ)_/¯", "모르겠음"},
                {"(ಠ_ಠ)", "째려봄"},
                {"(✿◠‿◠)", "행복"},
                {"(づ｡◕‿‿◕｡)づ", "포옹"},
                {"(ノ^_^)ノ", "축하"},
                {"ヽ(°〇°)ﾉ", "당황"},
                {"(｡♥‿♥｡)", "사랑"},
                {"(ง'̀-'́)ง", "파이팅"},
                {"(◕‿◕)", "귀여움"},
                {"(⌐■_■)", "쿨함"},
                {"(╥_╥)", "흑흑"},
                {"(ノಠ益ಠ)ノ", "분노"},
                {"(づ￣ ³￣)づ", "뽀뽀2"},
                {"(•‿•)", "윙크"},
                {"(⊙_⊙)", "응?"},
                {"ㄱㅅ", "감사"},
                {"ㅊㅋ", "축하"},
                {"ㅅㄱ", "수고"},
                {"ㄳ", "감사2"},
                {"굿", "좋아요"},
                {"오키", "OK"},
                {"ㅇㅋ", "OK2"},
                {"ㄴㄴ", "노노"},
                {"ㅇㅇ", "응응"},
                {"ㄹㅇ", "리얼"},
                {"헐", "놀람2"},
                {"대박", "대박"}
        };

        Font emoticonFont = new Font("맑은 고딕", Font.PLAIN, 16);

        for (String[] emoticon : emoticons) {
            String symbol = emoticon[0];
            String label = emoticon[1];

            JButton btnEmoticon = new JButton("<html><center>" + symbol + "<br><small>" + label + "</small></center></html>");
            btnEmoticon.setFont(emoticonFont);
            btnEmoticon.setFocusPainted(false);
            btnEmoticon.setToolTipText(label);
            btnEmoticon.addActionListener(e -> {
                txtInput.setText(txtInput.getText() + symbol + " ");
                emojiDialog.dispose();
            });
            emojiPanel.add(btnEmoticon);
        }

        JScrollPane scrollPane = new JScrollPane(emojiPanel);
        emojiDialog.add(scrollPane);
        emojiDialog.setVisible(true);
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

//        if (isMyTurn) {
//            appendText("[시스템] ⭐ 당신의 턴입니다! 15초 동안 그려주세요!");
//        }
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
            new VotingUI(userName, players, dos, dis, currentRoom.getRoomId(), serverIp, serverPort);
            dispose();
        });
    }

    class ListenNetwork extends Thread {
        public void run() {
            while (isRunning) {
                try {
                    String msg = dis.readUTF();
                    if(msg.startsWith("/updatePlayerList")) {
                        String playerStr = msg.substring(18); //명령어 길이만큼
                        String [] players = playerStr.split(",");

                        //로컬 데이터 초기화 및 갱신
                        currentRoom.getPlayers().clear();

                        List<String> newPlayerList = new ArrayList<>();
                        for(String p : players){
                            if(!p.trim().isEmpty()){
                                newPlayerList.add(p);
                            }
                        }
                        //UI 업데이트
                        updatePlayerList(newPlayerList);
                    }

                    if (msg.startsWith("/gameStart ")) {
                        String[] parts = msg.substring(11).split("\\|");
                        if (parts.length >= 2) {
                            String role = parts[0];
                            String info = parts[1];

                            SwingUtilities.invokeLater(() -> {
                                if(btnLeaveRoom != null){
                                    btnLeaveRoom.setEnabled(false); //나가기 버튼 비활성화
                                }
                                if (role.equals("LIAR")) {
                                    isLiar = true;
                                    myKeyword = info;

                                    // 커스텀 다이얼로그 생성
                                    JDialog dialog = new JDialog(JavaChatClientView.this, true);
                                    dialog.setSize(462, 432);
                                    dialog.setLocationRelativeTo(JavaChatClientView.this);
                                    dialog.setResizable(false);

                                    // ⭐ 배경 이미지가 있는 커스텀 패널
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

                                    // ⭐ 카테고리 정보만 오른쪽 하단에 표시
                                    JLabel messageLabel = new JLabel(
                                            "<html><div style='text-align: right;'>" +
                                                    "카테고리: " + info +
                                                    "</div></html>"
                                    );
                                    messageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                                    messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                                    messageLabel.setOpaque(false);

                                    // ⭐ 확인 버튼을 이미지로 대체
                                    JButton okButton = new JButton();
                                    try {
                                        ImageIcon icon = new ImageIcon(getClass().getResource("/info/check.png"));
                                        Image img = icon.getImage().getScaledInstance(120, 40, Image.SCALE_SMOOTH);
                                        okButton.setIcon(new ImageIcon(img));

                                        // 이미지 버튼 스타일
                                        okButton.setBorderPainted(false);
                                        okButton.setContentAreaFilled(false);
                                        okButton.setFocusPainted(false);
                                        okButton.setOpaque(false);
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

                                    // ⭐ 하단 패널 구성 (메시지 + 버튼)
                                    JPanel bottomPanel = new JPanel(new BorderLayout());
                                    bottomPanel.setOpaque(false);
                                    bottomPanel.add(messageLabel, BorderLayout.EAST); // 오른쪽에 배치
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

                                    // ⭐ 배경 이미지가 있는 커스텀 패널
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

                                    // ⭐ 키워드 정보만 오른쪽 하단에 표시
                                    JLabel messageLabel = new JLabel(
                                            "<html><div style='text-align: right;'>" +
                                                    "키워드: " + info +
                                                    "</div></html>"
                                    );
                                    messageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                                    messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                                    messageLabel.setOpaque(false);

                                    // ⭐ 확인 버튼을 이미지로 대체
                                    JButton okButton = new JButton();
                                    try {
                                        ImageIcon icon = new ImageIcon(getClass().getResource("/info/check2.png"));
                                        Image img = icon.getImage().getScaledInstance(120, 40, Image.SCALE_SMOOTH);
                                        okButton.setIcon(new ImageIcon(img));

                                        // 이미지 버튼 스타일
                                        okButton.setBorderPainted(false);
                                        okButton.setContentAreaFilled(false);
                                        okButton.setFocusPainted(false);
                                        okButton.setOpaque(false);
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

                                    // ⭐ 하단 패널 구성 (메시지 + 버튼)
                                    JPanel bottomPanel = new JPanel(new BorderLayout());
                                    bottomPanel.setOpaque(false);
                                    bottomPanel.add(messageLabel, BorderLayout.EAST); // 오른쪽에 배치
                                    bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

                                    panel.add(bottomPanel, BorderLayout.SOUTH);

                                    dialog.add(panel);
                                    dialog.setVisible(true);
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
                    else if (msg.startsWith("/gameEnded")) {
                        System.out.println("[Client] 게임 종료 수신 - 투표 화면으로 전환");
                        isRunning = false;

                        SwingUtilities.invokeLater(() -> {
                            appendText("===== 게임이 종료되었습니다! 투표를 시작합니다. =====");
                            openVotingUI();
                        });

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