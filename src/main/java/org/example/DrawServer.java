package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * DrawServer - 소켓 중계 전용 서버
 * 수정사항: 방 입장 시 기존 멤버 목록 동기화 로직 추가
 */
public class DrawServer extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtPortNumber;
    private JLabel lblConnectedClients;
    private JTextArea textArea;
    private ServerSocket socket;
    // Thread-safe한 리스트 관리를 위해 Vector 사용
    private Vector<UserService> UserVec = new Vector<>();

    // 방 정보를 저장하는 맵 (방ID -> 방정보 문자열)
    private Map<String, String> rooms = new HashMap<>();

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

        // 상단 패널
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

        // 서버 시작 버튼
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

        // 로그 영역
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

    private void updateClientCount() {
        SwingUtilities.invokeLater(() -> {
            lblConnectedClients.setText(String.valueOf(UserVec.size()));
        });
    }

    // 클라이언트 접속 처리 스레드
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

    // 클라이언트별 통신 스레드
    class UserService extends Thread {
        private Socket clientSocket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String userName = "";
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

        public void WriteAllExceptMe(String str) {
            for (UserService user : UserVec) {
                if (user != this) {
                    user.WriteOne(str);
                }
            }
        }

        // 같은 방에 있는 사람들에게만 메시지 전송
        public void WriteToRoom(String roomId, String msg) {
            for (UserService user : UserVec) {
                if (roomId.equals(user.currentRoomId)) {
                    user.WriteOne(msg);
                }
            }
        }

        // 같은 방의 다른 사람들에게 메시지 전송
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
                    // 방에서 퇴장 알림
                    WriteToRoomExceptMe(currentRoomId, "/playerLeft " + userName);
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
                // 첫 메시지로 로그인 처리
                String firstMsg = dis.readUTF();
                if (firstMsg.startsWith("/login ")) {
                    userName = firstMsg.substring(7).trim();
                    AppendText("[로그인] " + userName);
                    WriteOne("/loginOK");
                }

                // 메시지 처리 루프
                while (true) {
                    String msg = dis.readUTF().trim();

                    // 방 생성 요청
                    if (msg.startsWith("/createRoom ")) {
                        String roomData = msg.substring(12);
                        String[] parts = roomData.split("\\|");
                        String roomId = parts[0];

                        rooms.put(roomId, roomData);
                        currentRoomId = roomId;

                        AppendText("[방 생성] " + parts[1] + " (by " + userName + ")");
                        WriteOne("/roomCreated " + roomId);
                    }
                    // 방 목록 요청
                    else if (msg.equals("/getRoomList")) {
                        StringBuilder roomList = new StringBuilder("/roomList ");
                        for (String roomData : rooms.values()) {
                            roomList.append(roomData).append(";;");
                        }
                        WriteOne(roomList.toString());
                    }
                    // [수정됨] 방 참가 요청
                    else if (msg.startsWith("/joinRoom ")) {
                        String roomId = msg.substring(10);
                        currentRoomId = roomId;

                        AppendText("[방 참가] " + userName + " -> " + roomId);

                        // 1. 나에게: 방 참가 성공 알림
                        WriteOne("/joinedRoom " + roomId);

                        // 2. 기존 멤버들에게: "새로운 사람(나) 들어옴" 알림
                        WriteToRoomExceptMe(roomId, "/playerJoined " + userName);

                        // 3. [추가된 로직] 나에게: "기존에 누가 있는지" 알려줌
                        // 전체 유저 목록을 순회하며 같은 방 유저 찾기
                        for (UserService user : UserVec) {
                            // 나 자신은 제외하고, 방 ID가 같은 유저를 찾음
                            if (user != this && roomId.equals(user.currentRoomId)) {
                                // 나에게 기존 유저의 이름을 전송 -> 내 화면 UI에 추가됨
                                WriteOne("/playerJoined " + user.userName);
                            }
                        }
                    }
                    // 방 나가기
                    else if (msg.startsWith("/leaveRoom")) {
                        if (currentRoomId != null) {
                            AppendText("[방 퇴장] " + userName + " <- " + currentRoomId);
                            WriteToRoomExceptMe(currentRoomId, "/playerLeft " + userName);
                            currentRoomId = null;
                        }
                    }
                    // 게임 시작 (방장이 전송)
                    else if (msg.startsWith("/gameStart")) {
                        AppendText("[게임 시작] " + currentRoomId);
                        if (currentRoomId != null) {
                            WriteToRoom(currentRoomId, "/gameStart");
                        }
                    }
                    // 그리기 데이터
                    else if (msg.startsWith("/draw") || msg.startsWith("/clear")) {
                        if (currentRoomId != null) {
                            WriteToRoomExceptMe(currentRoomId, msg);
                        }
                    }
                    // 일반 채팅
                    else {
                        if (currentRoomId != null) {
                            WriteToRoom(currentRoomId, msg);
                        }
                    }
                }
            } catch (IOException e) {
                closeConnection();
            }
        }
    }
}