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
 * GameRoom을 통한 턴 기반 시스템 관리
 * 4명 플레이어, 15초 턴, 4라운드 게임 진행
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
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        contentPane.add(logPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
    }

    public void AppendText(String str) {
        textArea.append(str + "\n");
        textArea.setCaretPosition(textArea.getText().length());
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

    private synchronized void updateRoomCount(String roomId, int change) {
        String roomInfo = rooms.get(roomId);
        if (roomInfo != null) {
            String[] parts = roomInfo.split("\\|");
            try {
                int currentCount = Integer.parseInt(parts[3]);
                int newCount = currentCount + change;
                parts[3] = String.valueOf(newCount);
                String newRoomInfo = String.join("|", parts);
                rooms.put(roomId, newRoomInfo);

                // ★ 추가: 방 목록 업데이트를 모든 클라이언트에게 브로드캐스트
                broadcastRoomListUpdate();
            } catch (Exception e) {
                AppendText("방 인원 업데이트 오류: " + e.getMessage());
            }
        }
    }

    // ★ 추가: 방 목록 업데이트 브로드캐스트 메서드
    private void broadcastRoomListUpdate() {
        StringBuilder roomList = new StringBuilder("/roomList ");
        for (String roomData : rooms.values()) {
            roomList.append(roomData).append(";;");
        }

        // RoomListUI를 보고 있는 모든 클라이언트에게 전송
        for (UserService user : UserVec) {
            // 현재 방에 있지 않은 사용자에게만 전송 (로비에 있는 사용자)
            if (user.currentRoomId == null) {
                user.WriteOne(roomList.toString());
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
        String userName = "";  // package-private로 변경 (getUserByName에서 접근 가능하도록)
        private String currentRoomId = null;

        public UserService(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                InputStream is = clientSocket.getInputStream();
                dis = new DataInputStream(is);
                OutputStream os = clientSocket.getOutputStream();
                dos = new DataOutputStream(os);
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

        public void WriteAll(String str) {
            for (UserService user : UserVec) {
                user.WriteOne(str);
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

        private void closeConnection() {
            try {
                if (currentRoomId != null) {
                    updateRoomCount(currentRoomId, -1);
                    WriteToRoomExceptMe(currentRoomId, "/playerLeft " + userName);

                    GameRoom groom = gameRooms.get(currentRoomId);
                    if (groom != null) {
                        groom.removePlayer(userName);
                    }
                }

                if (dos != null) dos.close();
                if (dis != null) dis.close();
                if (clientSocket != null) clientSocket.close();
                UserVec.removeElement(this);
                updateClientCount();
                AppendText("[퇴장] " + userName);
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

                        // GameRoom 객체 생성
                        GameRoom groom = new GameRoom(roomId, parts[1], userName, parts[5],
                                Integer.parseInt(parts[4])); // maxPlayers

                        // 방장을 GameRoom에 추가
                        groom.addPlayer(userName);

                        gameRooms.put(roomId, groom);

                        AppendText("[방 생성] " + parts[1] + " (Host: " + userName + ")");
                        WriteOne("/roomCreated " + roomId);
                    }
                    else if (msg.equals("/getRoomList")) {
                        StringBuilder roomList = new StringBuilder("/roomList ");
                        for (String roomData : rooms.values()) {
                            roomList.append(roomData).append(";;");
                        }
                        WriteOne(roomList.toString());
                    }
                    else if (msg.startsWith("/joinRoom ")) {
                        String roomId = msg.substring(10);
                        currentRoomId = roomId;

                        GameRoom groom = gameRooms.get(roomId);
                        if (groom != null) {
                            // 새 플레이어를 GameRoom에 추가
                            if (!groom.getPlayers().contains(userName)) {
                                groom.addPlayer(userName);
                                updateRoomCount(roomId, 1);
                            }
                        }

                        AppendText("[방 참가] " + userName + " -> " + roomId);

                        // ★ 기존 플레이어 목록을 포함하여 전송
                        if (groom != null) {
                            List<String> players = groom.getPlayers();
                            StringBuilder playerList = new StringBuilder();
                            for (String player : players) {
                                playerList.append(player).append(",");
                            }
                            // 마지막 콤마 제거
                            if (playerList.length() > 0) {
                                playerList.setLength(playerList.length() - 1);
                            }

                            WriteOne("/joinedRoom " + roomId + "|" + playerList.toString());
                        } else {
                            WriteOne("/joinedRoom " + roomId);
                        }

                        // 다른 플레이어들에게 새 플레이어 입장 알림
                        WriteToRoomExceptMe(roomId, "/playerJoined " + userName);
                    }

                    else if (msg.startsWith("/leaveRoom")) {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            if (groom != null) {
                                groom.removePlayer(userName);
                            }

                            updateRoomCount(currentRoomId, -1);
                            AppendText("[방 퇴장] " + userName + " <- " + currentRoomId);
                            WriteToRoomExceptMe(currentRoomId, "/playerLeft " + userName);
                            currentRoomId = null;
                        }
                    }
                    // === 게임 시작 (카테고리 기반) ===
                    else if (msg.startsWith("/gameStart ")) {
                        // "/gameStart 카테고리" 형식
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
                                AppendText("[게임 시작] 방: " + currentRoomId + " (by " + userName + ", 카테고리: " + selectedCategory + ")");

                                // ⭐ startGameWithCategory 사용
                                groom.startGameWithCategory(selectedCategory, new GameRoom.GameEventListener() {
                                    @Override
                                    public void onTurnChanged(int turnIndex, int round, String currentPlayer) {
                                        String gameState = groom.getGameStateString();
                                        WriteToRoom(currentRoomId, gameState);
                                        AppendText(String.format("[턴 변경] R%d - %s의 턴 (%d초)",
                                                round, currentPlayer, groom.getRemainingSeconds()));
                                    }

                                    @Override
                                    public void onGameEnded() {
                                        AppendText("[게임 종료] 방: " + currentRoomId);
                                        WriteToRoom(currentRoomId, "/gameEnded");
                                    }

                                    @Override
                                    public void onTimerTick(int remainingSeconds) {
                                        String gameState = groom.getGameStateString();
                                        WriteToRoom(currentRoomId, gameState);
                                    }
                                });

                                // ⭐ 각 플레이어에게 역할 정보 전송
                                List<String> players = groom.getPlayers();
                                for (String player : players) {
                                    UserService user = getUserByName(player);
                                    if (user != null) {
                                        String playerInfo = groom.getPlayerInfo(player);
                                        user.WriteOne("/gameStart " + playerInfo);

                                        if (groom.isLiar(player)) {
                                            AppendText("[역할 배정] " + player + " → 라이어 (카테고리: " + selectedCategory + ")");
                                        } else {
                                            AppendText("[역할 배정] " + player + " → 시민 (키워드: " + groom.getSelectedKeyword() + ")");
                                        }
                                    }
                                }

                                String roomInfo = rooms.get(currentRoomId);
                                if(roomInfo != null) {
                                    String[] roomParts = roomInfo.split("\\|");
                                    roomParts[7] = "PLAYING";
                                    rooms.put(currentRoomId, String.join("|", roomParts));
                                }
                            } else {
                                WriteOne("[시스템] 4명이 모여야 게임을 시작할 수 있습니다.");
                            }
                        }
                    }
                    // === draw 명령 - 턴 검증 ===
                    else if (msg.startsWith("/draw")) {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);

                            if (groom != null && groom.isPlayerTurn(userName)) {
                                WriteToRoomExceptMe(currentRoomId, msg);
                            } else {
                                WriteOne("[시스템] 당신의 턴이 아닙니다!");
                            }
                        }
                    }
                    // === clear 명령 - 턴 검증 ===
                    else if (msg.startsWith("/clear")) {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);

                            if (groom != null && groom.isPlayerTurn(userName)) {
                                WriteToRoomExceptMe(currentRoomId, msg);
                            }
                        }
                    }
                    // === 투표 처리 ===
                    else if (msg.startsWith("/vote ")) {
                        String votedPlayer = msg.substring(6);
                        AppendText("[투표] " + userName + " -> " + votedPlayer);
                        WriteToRoom(currentRoomId, "[투표 알림] " + userName + "님이 투표를 완료했습니다.");
                        // 여기에 투표 결과 집계 로직 추가 가능
                    }
                    // === 채팅 - 턴 검증 ===
                    else {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);

                            // 게임이 실행 중이 아니거나, 자신의 턴일 때만 채팅 가능
                            if (groom == null || !groom.isGameRunning() || groom.isPlayerTurn(userName)) {
                                WriteToRoom(currentRoomId, msg);
                            } else {
                                WriteOne("[시스템] 당신의 턴이 아닙니다!");
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