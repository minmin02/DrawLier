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
 * 수정사항: 방 인원수 실시간 갱신 로직 추가 (2번 문제 해결)
 */
public class DrawServer extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtPortNumber;
    private JLabel lblConnectedClients;
    private JTextArea textArea;
    private ServerSocket socket;
    private Vector<UserService> UserVec = new Vector<>();

    // 방 정보를 저장하는 맵 (방ID -> 방정보 문자열)
    private Map<String, String> rooms = new HashMap<>();

    // 방장 정보를 저장하는 맵 (방ID -> 방장이름)
    private Map<String, String> roomOwners = new HashMap<>();

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

    // [핵심 추가] 방 인원수 업데이트 메서드
    private synchronized void updateRoomCount(String roomId, int change) {
        String roomInfo = rooms.get(roomId);
        if (roomInfo != null) {
            String[] parts = roomInfo.split("\\|");
            // parts[3]이 현재 인원수라고 가정 (GameRoom.toProtocolString 순서 참고)
            try {
                int currentCount = Integer.parseInt(parts[3]);
                int newCount = currentCount + change;

                // 인원수 갱신
                parts[3] = String.valueOf(newCount);

                // 다시 문자열로 합치기
                String newRoomInfo = String.join("|", parts);
                rooms.put(roomId, newRoomInfo);

                // 디버깅용 로그
                // AppendText("[인원변경] " + roomId + ": " + currentCount + " -> " + newCount);
            } catch (Exception e) {
                AppendText("방 인원 업데이트 오류: " + e.getMessage());
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
                    // [수정] 나갈 때 인원수 감소
                    updateRoomCount(currentRoomId, -1);

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

                        // [수정] 입장 시 인원수 증가
                        updateRoomCount(roomId, 1);

                        AppendText("[방 참가] " + userName + " -> " + roomId);

                        WriteOne("/joinedRoom " + roomId);
                        WriteToRoomExceptMe(roomId, "/playerJoined " + userName);

                        for (UserService user : UserVec) {
                            if (user != this && roomId.equals(user.currentRoomId)) {
                                WriteOne("/playerJoined " + user.userName);
                            }
                        }
                    }
                    else if (msg.startsWith("/leaveRoom")) {
                        if (currentRoomId != null) {
                            // [수정] 퇴장 시 인원수 감소
                            updateRoomCount(currentRoomId, -1);

                            AppendText("[방 퇴장] " + userName + " <- " + currentRoomId);
                            WriteToRoomExceptMe(currentRoomId, "/playerLeft " + userName);
                            currentRoomId = null;
                        }
                    }
                    else if (msg.startsWith("/gameStart")) {
                        String owner = roomOwners.get(currentRoomId);
                        if (owner != null && owner.equals(userName)) {
                            AppendText("[게임 시작] 방: " + currentRoomId + " (by " + userName + ")");
                            // [추가] 상태를 PLAYING으로 변경해줘야 리스트에서도 '게임중'으로 뜸
                            String roomInfo = rooms.get(currentRoomId);
                            if(roomInfo != null) {
                                String[] parts = roomInfo.split("\\|");
                                parts[7] = "PLAYING"; // GameRoom Enum 순서에 따라 상태 변경
                                rooms.put(currentRoomId, String.join("|", parts));
                            }

                            WriteToRoom(currentRoomId, "/gameStart");
                        } else {
                            AppendText("[권한 없음] " + userName + "이(가) 게임 시작 시도함");
                        }
                    }
                    else if (msg.startsWith("/draw") || msg.startsWith("/clear")) {
                        if (currentRoomId != null) {
                            WriteToRoomExceptMe(currentRoomId, msg);
                        }
                    }
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