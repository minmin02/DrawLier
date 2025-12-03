package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * DrawServer - 게임 서버
 * 수정사항:
 * 1. 게임 종료(투표 시작) 시 방 상태 FINISH로 변경 및 목록 숨김
 * 2. 퇴장 로그 중복 출력 방지
 * 3. 인원 0명 시 방 자동 삭제
 * 4. ★ 방장 퇴장 시 다음 사람에게 방장 위임 기능 추가
 */
public class DrawServer extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtPortNumber;
    private JLabel lblConnectedClients;
    private JTextArea textArea;
    private ServerSocket socket;
    private Vector<UserService> UserVec = new Vector<>();

    private Map<String, String> rooms = new HashMap<>();
    private Map<String, String> roomOwners = new HashMap<>();
    private Map<String, GameRoom> gameRooms = new HashMap<>();

    public static void main(String[] args) {
        // UTF-8 인코딩 강제 설정
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("client.encoding.override", "UTF-8");

        EventQueue.invokeLater(() -> {
            try {
                DrawServer frame = new DrawServer();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public DrawServer() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 600, 500);
        setTitle("DrawLier 중계 서버");

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.setBackground(Color.WHITE);
        contentPane.setLayout(new BorderLayout(10, 10));
        setContentPane(contentPane);

        JPanel topPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        topPanel.setOpaque(false);

        JLabel lblPort = new JLabel("포트 번호:", SwingConstants.RIGHT);
        lblPort.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        topPanel.add(lblPort);

        txtPortNumber = new JTextField("30000");
        txtPortNumber.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        topPanel.add(txtPortNumber);

        JLabel lblCount = new JLabel("접속자 수:", SwingConstants.RIGHT);
        lblCount.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        topPanel.add(lblCount);

        lblConnectedClients = new JLabel("0");
        lblConnectedClients.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        topPanel.add(lblConnectedClients);

        contentPane.add(topPanel, BorderLayout.NORTH);

        JButton btnServerStart = new JButton("서버 시작");
        btnServerStart.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnServerStart.setPreferredSize(new Dimension(0, 50));
        btnServerStart.setBackground(new Color(40, 167, 69));
        btnServerStart.setForeground(Color.WHITE);
        btnServerStart.setFocusPainted(false);
        btnServerStart.addActionListener(e -> {
            try {
                int port = Integer.parseInt(txtPortNumber.getText());
                socket = new ServerSocket(port);
                AppendText("=== 서버 시작됨 (포트: " + port + ") ===");
                btnServerStart.setEnabled(false);
                txtPortNumber.setEnabled(false);

                AcceptServer acceptServer = new AcceptServer();
                acceptServer.start();
            } catch (Exception ex) {
                AppendText("서버 시작 실패: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setOpaque(false);
        btnPanel.add(btnServerStart, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("서버 로그"));

        textArea = new JTextArea();
        textArea.setEditable(false);

        // UTF-8 한글 지원을 위한 폰트 설정
        Font logFont = getKoreanSupportFont(12);
        textArea.setFont(logFont);

        JScrollPane scrollPane = new JScrollPane(textArea);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        contentPane.add(logPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
    }

    public void AppendText(String str) {
        textArea.append(str + "\n");
        textArea.setCaretPosition(textArea.getText().length());
    }

    /**
     * 한글을 지원하는 폰트를 반환합니다.
     */
    private Font getKoreanSupportFont(int size) {
        String[] koreanFonts = {
            "맑은 고딕", "Malgun Gothic", "나눔고딕", "NanumGothic",
            "Apple SD Gothic Neo", "Noto Sans CJK KR", "Dialog"
        };
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();
        for (String koreanFont : koreanFonts) {
            for (String availableFont : availableFonts) {
                if (availableFont.equals(koreanFont)) {
                    return new Font(koreanFont, Font.PLAIN, size);
                }
            }
        }
        return new Font(Font.DIALOG, Font.PLAIN, size);
    }

    public UserService getUserByName(String userName) {
        for (UserService user : UserVec) {
            if (user.userName.equals(userName)) {
                return user;
            }
        }
        return null;
    }

    private void updateClientCount() {
        SwingUtilities.invokeLater(() -> {
            lblConnectedClients.setText(String.valueOf(UserVec.size()));
        });
    }

    // [추가] 방 삭제 및 목록 갱신
    private synchronized void removeRoom(String roomId) {
        if (rooms.containsKey(roomId)) {
            String roomName = "";
            GameRoom gr = gameRooms.get(roomId);
            if(gr != null) roomName = gr.getRoomName();

            rooms.remove(roomId);
            roomOwners.remove(roomId);
            gameRooms.remove(roomId);

            AppendText("[방 삭제] " + roomName + " (" + roomId + ")");
            broadcastRoomListUpdate();
        }
    }

    // [추가] 방 상태 변경 (FINISH 등)
    private synchronized void setRoomStatus(String roomId, String status) {
        String roomInfo = rooms.get(roomId);
        if (roomInfo != null) {
            String[] parts = roomInfo.split("\\|");
            if (parts.length >= 8) {
                parts[7] = status;
                String newRoomInfo = String.join("|", parts);
                rooms.put(roomId, newRoomInfo);
                broadcastRoomListUpdate();
            }
        }
    }

    // ★★★ [수정] 방장 정보도 업데이트 하도록 수정 ★★★
    private synchronized void updateRoomInfo(String roomId, int playerCount, String newHostName) {
        String roomInfo = rooms.get(roomId);
        if (roomInfo != null) {
            String[] parts = roomInfo.split("\\|");
            try {
                if (playerCount <= 0) {
                    removeRoom(roomId);
                } else {
                    parts[3] = String.valueOf(playerCount); // 인원 수 업데이트
                    if (newHostName != null && !newHostName.isEmpty()) {
                        parts[2] = newHostName; // 방장 이름 업데이트
                    }
                    String newRoomInfo = String.join("|", parts);
                    rooms.put(roomId, newRoomInfo);
                    broadcastRoomListUpdate();
                }
            } catch (Exception e) {
                AppendText("방 정보 업데이트 오류: " + e.getMessage());
            }
        }
    }

    // [추가] FINISH 상태인 방은 목록에서 제외
    private String getFilteredRoomList() {
        StringBuilder roomList = new StringBuilder("/roomList ");
        for (String roomData : rooms.values()) {
            String[] parts = roomData.split("\\|");
            if (parts.length >= 8 && "FINISH".equals(parts[7])) {
                continue;
            }
            roomList.append(roomData).append(";;");
        }
        return roomList.toString();
    }

    private void broadcastRoomListUpdate() {
        String roomList = getFilteredRoomList();
        for (UserService user : UserVec) {
            if (user.currentRoomId == null) {
                user.WriteOne(roomList);
            }
        }
    }

    class AcceptServer extends Thread {
        public void run() {
            AppendText("클라이언트 접속 대기 중...");
            while (true) {
                try {
                    Socket clientSocket = socket.accept();
                    AppendText("[접속] " + clientSocket.getInetAddress());
                    UserService newUser = new UserService(clientSocket);
                    UserVec.add(newUser);
                    updateClientCount();
                    newUser.start();
                } catch (IOException e) {
                    AppendText("Accept 에러: " + e.getMessage());
                    break;
                }
            }
        }
    }

    class UserService extends Thread {
        private Socket clientSocket;
        private DataInputStream dis;
        private DataOutputStream dos;
        String userName = "";
        private String currentRoomId = null;
        private boolean isClosed = false;

        public UserService(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());
            } catch (Exception e) {
                AppendText("UserService 초기화 에러");
            }
        }

        public void WriteOne(String msg) {
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
                closeConnection();
            }
        }

        public void WriteToRoom(String roomId, String msg) {
            for (UserService user : UserVec) {
                if (roomId.equals(user.currentRoomId)) {
                    user.WriteOne(msg);
                }
            }
        }

        public void WriteToRoomExceptMe(String roomId, String msg) {
            for (UserService user : UserVec) {
                if (roomId.equals(user.currentRoomId) && user != this) {
                    user.WriteOne(msg);
                }
            }
        }

        private void broadcastPlayerList(String roomId){
            GameRoom groom = gameRooms.get(roomId);
            if(groom != null){
                String playerStr = String.join(",", groom.getPlayers());
            }
        }

        // ★★★ [추가] 방 퇴장 및 방장 위임 로직을 처리하는 메소드 ★★★
        private synchronized void handleLeaveRoom() {
            if (currentRoomId == null) return;

            GameRoom groom = gameRooms.get(currentRoomId);
            if (groom == null) return;

            String leavingPlayerName = this.userName;
            boolean wasHost = groom.getHostName().equals(leavingPlayerName);

            groom.removePlayer(leavingPlayerName);
            AppendText("[방 퇴장] " + leavingPlayerName + " <- " + groom.getRoomName());

            // 방에 남은 사람이 있는지 확인
            if (groom.getPlayers().isEmpty()) {
                removeRoom(currentRoomId); // 방 삭제
            } else {
                // 방장이 나갔고, 남은 인원이 있다면 방장 위임
                if (wasHost) {
                    String newHostName = groom.getPlayers().get(0); // 다음 사람을 새 방장으로
                    groom.setHostName(newHostName);
                    roomOwners.put(currentRoomId, newHostName);
                    AppendText("[방장 위임] " + groom.getRoomName() + " -> " + newHostName);

                    // 방 정보(방장, 인원) 업데이트 및 로비에 브로드캐스트
                    updateRoomInfo(currentRoomId, groom.getPlayers().size(), newHostName);
                    // 방에 있는 사람들에게 방장 변경 알림
                    WriteToRoom(currentRoomId, "/hostChanged " + newHostName);
                } else {
                    // 일반 유저가 나갔을 경우 인원수만 업데이트
                    updateRoomInfo(currentRoomId, groom.getPlayers().size(), null);
                }
                // 나갔다는 정보와 최신 플레이어 목록을 방의 다른 사람들에게 전송
                WriteToRoomExceptMe(currentRoomId, "/playerLeft " + leavingPlayerName);
                broadcastPlayerList(currentRoomId);
            }
            this.currentRoomId = null; // 현재 유저의 방 정보 초기화
        }

        private synchronized void closeConnection() {
            if (isClosed) return;
            isClosed = true;

            try {
                handleLeaveRoom(); // 퇴장 로직 호출

                if (dos != null) dos.close();
                if (dis != null) dis.close();
                if (clientSocket != null) clientSocket.close();
                UserVec.removeElement(this);
                updateClientCount();
                AppendText("[접속 종료] " + userName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        public void run() {
            try {
                String firstMsg = dis.readUTF();
                if (firstMsg.startsWith("/login ")) {
                    userName = firstMsg.substring(7).trim();
                    AppendText("[로그인] " + userName);
                    WriteOne("/loginOK");
                }

                while (true) {
                    String msg = dis.readUTF().trim();

                    if (msg.startsWith("/createRoom ")) {
                        String roomData = msg.substring(12);
                        String[] parts = roomData.split("\\|");
                        String roomId = parts[0];

                        rooms.put(roomId, roomData);
                        roomOwners.put(roomId, userName);
                        currentRoomId = roomId;

                        GameRoom groom = new GameRoom(roomId, parts[1], userName, parts[5], Integer.parseInt(parts[4]));
                        groom.addPlayer(userName);
                        gameRooms.put(roomId, groom);

                        AppendText("[방 생성] " + parts[1] + " (Host: " + userName + ")");
                        WriteOne("/roomCreated " + roomId);
                        broadcastRoomListUpdate(); // ★★★ 방 생성 후 즉시 목록 갱신
                    }
                    else if (msg.equals("/getRoomList")) {
                        String roomList = getFilteredRoomList();
                        WriteOne(roomList);
                    }
                    else if (msg.startsWith("/joinRoom ")) {
                        String roomId = msg.substring(10);
                        currentRoomId = roomId;

                        GameRoom groom = gameRooms.get(roomId);
                        if (groom != null) {
                            if (!groom.getPlayers().contains(userName)) {
                                groom.addPlayer(userName);
                                // ★★★ 인원수 업데이트 로직 변경 ★★★
                                updateRoomInfo(roomId, groom.getPlayers().size(), null);
                            }
                        }

                        AppendText("[방 참가] " + userName + " -> " + roomId);

                        if (groom != null) {
                            String playerListStr = String.join(",", groom.getPlayers());
                            WriteOne("/joinedRoom " + roomId + "|" + playerListStr);
                        } else {
                            WriteOne("/joinedRoom " + roomId);
                        }

                        WriteToRoomExceptMe(roomId, "/playerJoined " + userName);
                        broadcastPlayerList(roomId);
                    }
                    // ★★★ [수정] 퇴장 로직을 handleLeaveRoom으로 위임 ★★★
                    else if (msg.startsWith("/leaveRoom")) {
                        handleLeaveRoom();
                    }
                    else if (msg.startsWith("/gameStart ")) {
                        String[] parts = msg.split(" ", 2);
                        if (parts.length < 2) {
                            WriteOne("[시스템] 카테고리를 선택해주세요.");
                            continue;
                        }
                        String selectedCategory = parts[1];
                        String owner = roomOwners.get(currentRoomId);

                        if (owner != null && owner.equals(userName)) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            if (groom != null && groom.canStartGame()) {
                                AppendText("[게임 시작] 방: " + currentRoomId + " (카테고리: " + selectedCategory + ")");
                                groom.startGameWithCategory(selectedCategory, new GameRoom.GameEventListener() {
                                    @Override
                                    public void onTurnChanged(int turnIndex, int round, String currentPlayer) {
                                        WriteToRoom(currentRoomId, groom.getGameStateString());
                                        AppendText(String.format("[턴 변경] R%d - %s의 턴 (%d초)", round, currentPlayer, groom.getRemainingSeconds()));
                                    }
                                    @Override
                                    public void onGameEnded() {
                                        AppendText("[게임 종료] 방: " + currentRoomId + " -> 투표 진입 (목록에서 숨김)");
                                        setRoomStatus(currentRoomId, "FINISH");
                                        WriteToRoom(currentRoomId, "/gameEnded");
                                    }
                                    @Override
                                    public void onTimerTick(int remainingSeconds) {
                                        WriteToRoom(currentRoomId, groom.getGameStateString());
                                    }
                                });

                                for (String player : groom.getPlayers()) {
                                    UserService user = getUserByName(player);
                                    if (user != null) {
                                        user.WriteOne("/gameStart " + groom.getPlayerInfo(player));
                                        if (groom.isLiar(player)) AppendText("[역할 배정] " + player + " → 라이어");
                                        else AppendText("[역할 배정] " + player + " → 시민 (키워드: " + groom.getSelectedKeyword() + ")");
                                    }
                                }
                                setRoomStatus(currentRoomId, "PLAYING");
                            }
                        }
                    }
                    else if (msg.startsWith("/draw")) {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            if (groom != null && groom.isPlayerTurn(userName)) {
                                WriteToRoomExceptMe(currentRoomId, msg);
                            }
                        }
                    }
                    else if (msg.startsWith("/clear")) {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            if (groom != null && groom.isPlayerTurn(userName)) {
                                WriteToRoomExceptMe(currentRoomId, msg);
                            }
                        }
                    }
                    else if (msg.startsWith("/vote ")) {
                        String votedPlayer = msg.substring(6);
                        GameRoom groom = gameRooms.get(currentRoomId);
                        if (groom != null && groom.isVotingPhase()) {
                            groom.addVote(userName, votedPlayer);
                            AppendText("[투표] " + userName + " -> " + votedPlayer);
                            if (groom.hasAllVoted()) {
                                String mostVoted = groom.getMostVotedPlayer();
                                boolean isLiar = groom.isMostVotedLiar();
                                AppendText("[투표 결과] 최다 득표: " + mostVoted + " (라이어 여부: " + isLiar + ")");
                                WriteToRoom(currentRoomId, "/voteResult " + mostVoted + "|" + isLiar);
                                if (!isLiar) {
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(500);
                                            String finalResult = "/finalResult LIAR|" + groom.getLiarName() + "|" + mostVoted + "님이 억울하게 투표되었습니다! 실제 라이어는 " + groom.getLiarName() + "님이었습니다.\n정답 키워드: " + groom.getSelectedKeyword();
                                            WriteToRoom(currentRoomId, finalResult);
                                            AppendText("[게임 결과] 라이어 승리 - " + currentRoomId);
                                        } catch (InterruptedException e) { e.printStackTrace(); }
                                    }).start();
                                } else {
                                    AppendText("[진행] 라이어(" + groom.getLiarName() + ") 정답 입력 대기 중...");
                                }
                            }
                        }
                    }
                    else if (msg.startsWith("/liarAnswer ")) {
                        String answer = msg.substring(12);
                        GameRoom groom = gameRooms.get(currentRoomId);
                        if (groom != null && groom.getLiarName().equals(userName)) {
                            boolean correct = groom.checkLiarAnswer(answer);
                            AppendText("[라이어 답변] " + userName + " -> " + answer + " (정답: " + correct + ")");
                            String finalResult;
                            if (correct) {
                                finalResult = "/finalResult LIAR|" + userName + "|라이어 " + userName + "님이 정답을 맞췄습니다!\n정답: " + groom.getSelectedKeyword();
                            } else {
                                finalResult = "/finalResult CITIZEN|" + userName + "|라이어 " + userName + "님이 정답을 맞추지 못했습니다!\n라이어의 답변: " + answer + "\n정답: " + groom.getSelectedKeyword();
                            }
                            WriteToRoom(currentRoomId, finalResult);
                            AppendText("[게임 결과] " + (correct ? "라이어" : "시민") + " 승리 - " + currentRoomId);
                        }
                    }
                    else {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            if (groom == null || !groom.isGameRunning() || groom.isPlayerTurn(userName)) {
                                WriteToRoom(currentRoomId, msg);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                closeConnection();
            }
        }
    }
}
