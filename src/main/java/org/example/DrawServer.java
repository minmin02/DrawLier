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
    private JTextArea textArea; //서버 로그를 표시하는 텍스트 영역
    private ServerSocket socket; //클라이언트의 연결을 기다리는 서버 소켓
    private Vector<UserService> UserVec = new Vector<>(); //연결된 모든 클라이언트(UserService)를 저장하는 벡터

    //방 정보를 관리하는 맵
    private Map<String, String> rooms = new HashMap<>(); //Key: roomId, Value: 방 정보 프로토콜 문자열
    private Map<String, String> roomOwners = new HashMap<>(); //Key: roomId, Value: 방장 userName
    private Map<String, GameRoom> gameRooms = new HashMap<>(); //Key: roomId, Value: GameRoom 객체

    public static void main(String[] args) {
        //UTF-8 인코딩 강제 설정
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

    //생성자
    public DrawServer() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 600, 500);
        setTitle("DrawLier 중계 서버");

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.setBackground(Color.WHITE);
        contentPane.setLayout(new BorderLayout(10, 10));
        setContentPane(contentPane);

        //상단 UI 패널 (포트 번호, 접속자 수)
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

        //서버 시작 버튼
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

                //클라이언트 접속을 기다리는 스레드 시작
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

        //서버 로그 UI
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("서버 로그"));

        textArea = new JTextArea();
        textArea.setEditable(false);

        //UTF-8 한글 지원을 위한 폰트 설정
        Font logFont = getKoreanSupportFont(12);
        textArea.setFont(logFont);

        JScrollPane scrollPane = new JScrollPane(textArea);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        contentPane.add(logPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
    }

    //서버 로그 텍스트를 UI에 추가하는 메소드
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

    //사용자 이름으로 UserService 객체를 찾는 메소드
    public UserService getUserByName(String userName) {
        for (UserService user : UserVec) {
            if (user.userName.equals(userName)) {
                return user;
            }
        }
        return null;
    }

    //접속자 수 UI를 갱신하는 메소드
    private void updateClientCount() {
        SwingUtilities.invokeLater(() -> {
            lblConnectedClients.setText(String.valueOf(UserVec.size()));
        });
    }

    //방을 목록에서 제거하는 메소드
    private synchronized void removeRoom(String roomId) {
        if (rooms.containsKey(roomId)) {
            String roomName = "";
            GameRoom gr = gameRooms.get(roomId);
            if(gr != null) roomName = gr.getRoomName();

            rooms.remove(roomId);
            roomOwners.remove(roomId);
            gameRooms.remove(roomId);

            AppendText("[방 삭제] " + roomName + " (" + roomId + ")");
            broadcastRoomListUpdate(); //변경된 방 목록을 로비에 있는 모두에게 전송
        }
    }

    //방의 상태(대기중, 게임중, 종료)를 변경하는 메소드
    private synchronized void setRoomStatus(String roomId, String status) {
        String roomInfo = rooms.get(roomId);
        if (roomInfo != null) {
            String[] parts = roomInfo.split("\\|");
            //프로토콜: roomId|roomName|hostName|current|max|category|time|status
            if (parts.length >= 8) {
                parts[7] = status;
                String newRoomInfo = String.join("|", parts);
                rooms.put(roomId, newRoomInfo);
                broadcastRoomListUpdate();
            }
        }
    }

    //방 정보를 업데이트하는 메소드 (인원, 방장 변경)
    private synchronized void updateRoomInfo(String roomId, int playerCount, String newHostName) {
        String roomInfo = rooms.get(roomId);
        if (roomInfo != null) {
            String[] parts = roomInfo.split("\\|");
            try {
                if (playerCount <= 0) { //방에 남은 인원이 없으면
                    removeRoom(roomId); //방 삭제
                } else {
                    parts[3] = String.valueOf(playerCount); //인원 수 업데이트
                    if (newHostName != null && !newHostName.isEmpty()) {
                        parts[2] = newHostName; //방장 이름 업데이트
                    }
                    String newRoomInfo = String.join("|", parts);
                    rooms.put(roomId, newRoomInfo);
                    broadcastRoomListUpdate(); //변경된 방 목록을 로비에 있는 모두에게 전송
                }
            } catch (Exception e) {
                AppendText("방 정보 업데이트 오류: " + e.getMessage());
            }
        }
    }

    //로비에 표시될 방 목록 문자열을 생성하는 메소드 (게임이 끝난 방은 제외)
    private String getFilteredRoomList() {
        StringBuilder roomList = new StringBuilder("/roomList ");
        for (String roomData : rooms.values()) {
            String[] parts = roomData.split("\\|");
            //FINISH 상태인 방은 목록에서 제외
            if (parts.length >= 8 && "FINISH".equals(parts[7])) {
                continue;
            }
            roomList.append(roomData).append(";;");
        }
        return roomList.toString();
    }

    //변경된 방 목록을 로비에 있는 모든 클라이언트에게 전송하는 메소드
    private void broadcastRoomListUpdate() {
        String roomList = getFilteredRoomList();
        for (UserService user : UserVec) {
            //방에 들어가 있지 않은(로비에 있는) 유저에게만 전송
            if (user.currentRoomId == null) {
                user.WriteOne(roomList);
            }
        }
    }

    //클라이언트의 접속을 계속해서 기다리고, 접속 시 UserService 스레드를 생성하는 스레드
    class AcceptServer extends Thread {
        public void run() {
            AppendText("클라이언트 접속 대기 중...");
            while (true) {
                try {
                    Socket clientSocket = socket.accept(); //클라이언트 접속 대기
                    AppendText("[접속] " + clientSocket.getInetAddress());
                    UserService newUser = new UserService(clientSocket);
                    UserVec.add(newUser); //벡터에 새로운 사용자 추가
                    updateClientCount(); //접속자 수 UI 갱신
                    newUser.start(); //사용자별 메시지 수신 스레드 시작
                } catch (IOException e) {
                    AppendText("Accept 에러: " + e.getMessage());
                    break;
                }
            }
        }
    }

    //각 클라이언트와 개별적으로 통신하는 스레드
    class UserService extends Thread {
        private Socket clientSocket;
        private DataInputStream dis;
        private DataOutputStream dos;
        String userName = "";
        private String currentRoomId = null; //현재 참가 중인 방의 ID
        private boolean isClosed = false; //연결 종료 여부 (중복 처리 방지)

        //생성자
        public UserService(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());
            } catch (Exception e) {
                AppendText("UserService 초기화 에러");
            }
        }

        //특정 클라이언트에게 메시지를 전송하는 메소드
        public void WriteOne(String msg) {
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
                closeConnection(); //전송 실패 시 연결 종료 처리
            }
        }

        //현재 방에 있는 모든 클라이언트에게 메시지를 전송하는 메소드
        public void WriteToRoom(String roomId, String msg) {
            for (UserService user : UserVec) {
                if (roomId.equals(user.currentRoomId)) {
                    user.WriteOne(msg);
                }
            }
        }

        //현재 방에 있는 다른 클라이언트들에게만 메시지를 전송하는 메소드 (나 자신 제외)
        public void WriteToRoomExceptMe(String roomId, String msg) {
            for (UserService user : UserVec) {
                if (roomId.equals(user.currentRoomId) && user != this) {
                    user.WriteOne(msg);
                }
            }
        }

        //해당 방의 모든 유저에게 최신 플레이어 명단을 전송하는 메소드
        private void broadcastPlayerList(String roomId){
            GameRoom groom = gameRooms.get(roomId);
            if(groom != null){
                String playerStr = String.join(",", groom.getPlayers());
                WriteToRoom(roomId, "/updatePlayerList " + playerStr);
            }
        }

        //방 퇴장 및 방장 위임 로직을 처리하는 메소드
        private synchronized void handleLeaveRoom() {
            if (currentRoomId == null) return; //방에 없으면 아무것도 안 함

            GameRoom groom = gameRooms.get(currentRoomId);
            if (groom == null) return;

            String leavingPlayerName = this.userName;
            boolean wasHost = groom.getHostName().equals(leavingPlayerName); //나가는 사람이 방장이었는지 확인

            groom.removePlayer(leavingPlayerName); //게임룸 객체에서 플레이어 제거
            AppendText("[방 퇴장] " + leavingPlayerName + " <- " + groom.getRoomName());

            //방에 남은 사람이 있는지 확인
            if (groom.getPlayers().isEmpty()) {
                removeRoom(currentRoomId); //방에 아무도 없으면 방 삭제
            } else {
                //방장이 나갔고, 남은 인원이 있다면 방장 위임
                if (wasHost) {
                    String newHostName = groom.getPlayers().get(0); //다음 사람(0번 인덱스)을 새 방장으로
                    groom.setHostName(newHostName); //GameRoom 객체에 새 방장 설정
                    roomOwners.put(currentRoomId, newHostName); //서버의 방장 목록 정보 업데이트
                    AppendText("[방장 위임] " + groom.getRoomName() + " -> " + newHostName);

                    //방 정보(방장, 인원) 업데이트 및 로비에 브로드캐스트
                    updateRoomInfo(currentRoomId, groom.getPlayers().size(), newHostName);
                    //방에 있는 사람들에게 방장 변경 알림
                    WriteToRoom(currentRoomId, "/hostChanged " + newHostName);
                } else {
                    //일반 유저가 나갔을 경우 인원수만 업데이트
                    updateRoomInfo(currentRoomId, groom.getPlayers().size(), null);
                }
                //나갔다는 정보와 최신 플레이어 목록을 방의 다른 사람들에게 전송
                WriteToRoomExceptMe(currentRoomId, "/playerLeft " + leavingPlayerName);
                broadcastPlayerList(currentRoomId);
            }
            this.currentRoomId = null; //현재 유저의 방 정보 초기화
        }

        //클라이언트와의 연결을 종료하는 메소드
        private synchronized void closeConnection() {
            if (isClosed) return; //이미 종료 처리되었으면 중복 실행 방지
            isClosed = true;

            try {
                handleLeaveRoom(); //방에서 나가는 로직 처리

                if (dos != null) dos.close();
                if (dis != null) dis.close();
                if (clientSocket != null) clientSocket.close();
                UserVec.removeElement(this); //전체 사용자 목록에서 제거
                updateClientCount(); //접속자 수 UI 갱신
                AppendText("[접속 종료] " + userName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        //클라이언트로부터 메시지를 수신하고 처리하는 메인 루프
        public void run() {
            try {
                //최초 접속 시 로그인 처리
                String firstMsg = dis.readUTF();
                if (firstMsg.startsWith("/login ")) {
                    userName = firstMsg.substring(7).trim();
                    AppendText("[로그인] " + userName);
                    WriteOne("/loginOK");
                }

                while (true) {
                    String msg = dis.readUTF().trim();

                    //방 생성 요청
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
                        broadcastRoomListUpdate(); //방 생성 후 즉시 목록 갱신
                    }
                    //방 목록 요청
                    else if (msg.equals("/getRoomList")) {
                        String roomList = getFilteredRoomList();
                        WriteOne(roomList);
                    }
                    //방 참가 요청
                    else if (msg.startsWith("/joinRoom ")) {
                        String roomId = msg.substring(10);
                        currentRoomId = roomId;

                        GameRoom groom = gameRooms.get(roomId);
                        if (groom != null) {
                            if (!groom.getPlayers().contains(userName)) {
                                groom.addPlayer(userName);
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
                    //방 나가기 요청
                    else if (msg.startsWith("/leaveRoom")) {
                        handleLeaveRoom();
                    }
                    //게임 시작 요청
                    else if (msg.startsWith("/gameStart ")) {
                        String[] parts = msg.split(" ", 2);
                        if (parts.length < 2) {
                            WriteOne("[시스템] 카테고리를 선택해주세요.");
                            continue;
                        }
                        String selectedCategory = parts[1];
                        String owner = roomOwners.get(currentRoomId);

                        //방장만 게임 시작 가능
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

                                //각 플레이어에게 역할(시민/라이어) 및 키워드 전송
                                for (String player : groom.getPlayers()) {
                                    UserService user = getUserByName(player);
                                    if (user != null) {
                                        user.WriteOne("/gameStart " + groom.getPlayerInfo(player));
                                        if (groom.isLiar(player)) AppendText("[역할 배정] " + player + " → 라이어");
                                        else AppendText("[역할 배정] " + player + " → 시민 (키워드: " + groom.getSelectedKeyword() + ")");
                                    }
                                }
                                setRoomStatus(currentRoomId, "PLAYING"); //방 상태를 '게임중'으로 변경
                            }
                        }
                    }
                    //그리기 정보 수신
                    else if (msg.startsWith("/draw")) {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            //자기 턴인 경우에만 다른 사람에게 그림 정보 전송
                            if (groom != null && groom.isPlayerTurn(userName)) {
                                WriteToRoomExceptMe(currentRoomId, msg);
                            }
                        }
                    }
                    //전체 지우기 정보 수신
                    else if (msg.startsWith("/clear")) {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            if (groom != null && groom.isPlayerTurn(userName)) {
                                WriteToRoomExceptMe(currentRoomId, msg);
                            }
                        }
                    }
                    //투표 정보 수신
                    else if (msg.startsWith("/vote ")) {
                        String votedPlayer = msg.substring(6);
                        GameRoom groom = gameRooms.get(currentRoomId);
                        if (groom != null && groom.isVotingPhase()) {
                            groom.addVote(userName, votedPlayer);
                            AppendText("[투표] " + userName + " -> " + votedPlayer);
                            //모든 플레이어가 투표했는지 확인
                            if (groom.hasAllVoted()) {
                                String mostVoted = groom.getMostVotedPlayer();
                                boolean isLiar = groom.isMostVotedLiar();
                                AppendText("[투표 결과] 최다 득표: " + mostVoted + " (라이어 여부: " + isLiar + ")");
                                WriteToRoom(currentRoomId, "/voteResult " + mostVoted + "|" + isLiar);
                                //시민이 잘못 지목된 경우 (라이어 승리)
                                if (!isLiar) {
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(500); //클라이언트가 대기화면을 볼 수 있도록 잠시 대기
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
                    //라이어 정답 입력 정보 수신
                    else if (msg.startsWith("/liarAnswer ")) {
                        String answer = msg.substring(12);
                        GameRoom groom = gameRooms.get(currentRoomId);
                        if (groom != null && groom.getLiarName().equals(userName)) {
                            boolean correct = groom.checkLiarAnswer(answer);
                            AppendText("[라이어 답변] " + userName + " -> " + answer + " (정답: " + correct + ")");
                            String finalResult;
                            if (correct) { //라이어가 정답을 맞춘 경우 (라이어 승리)
                                finalResult = "/finalResult LIAR|" + userName + "|라이어 " + userName + "님이 정답을 맞췄습니다!\n정답: " + groom.getSelectedKeyword();
                            } else { //라이어가 정답을 틀린 경우 (시민 승리)
                                finalResult = "/finalResult CITIZEN|" + userName + "|라이어 " + userName + "님이 정답을 맞추지 못했습니다!\n라이어의 답변: " + answer + "\n정답: " + groom.getSelectedKeyword();
                            }
                            WriteToRoom(currentRoomId, finalResult);
                            AppendText("[게임 결과] " + (correct ? "라이어" : "시민") + " 승리 - " + currentRoomId);
                        }
                    }
                    //그 외 메시지는 채팅으로 간주
                    else {
                        if (currentRoomId != null) {
                            GameRoom groom = gameRooms.get(currentRoomId);
                            //게임 중이 아닐 때, 또는 게임 중이지만 자기 턴일 때만 채팅 가능
                            if (groom == null || !groom.isGameRunning() || groom.isPlayerTurn(userName)) {
                                WriteToRoom(currentRoomId, msg);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                closeConnection(); //예외 발생 시 연결 종료
            }
        }
    }
}
