package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * 방 목록을 보여주고 방 생성/참가 기능을 제공하는 UI
 */
public class RoomListUI extends JFrame {

    // ... (기존 필드 변수들 동일) ...
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton btnCreateRoom;
    private JButton btnJoinRoom;
    private JButton btnRefresh;
    private String userName;
    private String serverIp;
    private String serverPort;
    private Map<String, GameRoom> roomMap;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    // 스레드 제어용 플래그 추가
    private boolean isRunning = true;

    public RoomListUI(String userName, String serverIp, String serverPort) {
        this.userName = userName;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.roomMap = new HashMap<>();

        connectToServer();
        initializeUI();
        requestRoomList();
    }

    // ... (중간 코드 생략, connectToServer, initializeUI, createRoom 등 기존과 동일) ...

    private void connectToServer() {
        try {
            socket = new Socket(serverIp, Integer.parseInt(serverPort));
            InputStream is = socket.getInputStream();
            dis = new DataInputStream(is);
            OutputStream os = socket.getOutputStream();
            dos = new DataOutputStream(os);

            dos.writeUTF("/login " + userName);
            String response = dis.readUTF();

            if (!response.equals("/loginOK")) {
                throw new Exception("로그인 실패");
            }

            new ListenNetwork().start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패: " + e.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    // ... (initializeUI, requestRoomList, openCreateRoomDialog, createRoom, joinSelectedRoom 메서드는 기존 그대로 유지) ...

    private void initializeUI() {
        // 기존 코드 유지
        setTitle("DrawLier - 방 목록");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBackground(Color.WHITE);
        setContentPane(contentPane);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("게임 방 목록");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JLabel userInfoLabel = new JLabel("접속자: " + userName);
        userInfoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        userInfoLabel.setForeground(new Color(100, 100, 100));
        topPanel.add(userInfoLabel, BorderLayout.EAST);

        contentPane.add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"방 이름", "방장", "인원", "카테고리", "제한시간", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        roomTable = new JTable(tableModel);
        roomTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        roomTable.setRowHeight(35);
        roomTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
        roomTable.getTableHeader().setBackground(new Color(66, 133, 244));
        roomTable.getTableHeader().setForeground(Color.WHITE);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);

        btnRefresh = new JButton("새로고침");
        btnRefresh.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        btnRefresh.setPreferredSize(new Dimension(120, 40));
        btnRefresh.addActionListener(e -> requestRoomList());

        btnJoinRoom = new JButton("방 참가");
        btnJoinRoom.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btnJoinRoom.setPreferredSize(new Dimension(120, 40));
        btnJoinRoom.setBackground(new Color(66, 133, 244));
        btnJoinRoom.setForeground(Color.WHITE);
        btnJoinRoom.setFocusPainted(false);
        btnJoinRoom.addActionListener(e -> joinSelectedRoom());

        btnCreateRoom = new JButton("방 만들기");
        btnCreateRoom.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btnCreateRoom.setPreferredSize(new Dimension(120, 40));
        btnCreateRoom.setBackground(new Color(220, 53, 69));
        btnCreateRoom.setForeground(Color.WHITE);
        btnCreateRoom.setFocusPainted(false);
        btnCreateRoom.addActionListener(e -> openCreateRoomDialog());

        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnJoinRoom);
        buttonPanel.add(btnCreateRoom);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    if (dos != null) dos.close();
                    if (dis != null) dis.close();
                    if (socket != null) socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void requestRoomList() {
        try {
            dos.writeUTF("/getRoomList");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openCreateRoomDialog() {
        JDialog dialog = new JDialog(this, "방 만들기", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblRoomName = new JLabel("방 이름:");
        JTextField txtRoomName = new JTextField();

        JLabel lblCategory = new JLabel("카테고리:");
        String[] categories = {"직업", "동물", "음식"};
        JComboBox<String> cmbCategory = new JComboBox<>(categories);

        JLabel lblTime = new JLabel("제한 시간 (초):");
        JTextField txtTime = new JTextField("300");

        JButton btnCreate = new JButton("생성");
        JButton btnCancel = new JButton("취소");

        panel.add(lblRoomName);
        panel.add(txtRoomName);
        panel.add(lblCategory);
        panel.add(cmbCategory);
        panel.add(lblTime);
        panel.add(txtTime);
        panel.add(btnCancel);
        panel.add(btnCreate);

        btnCreate.addActionListener(e -> {
            String roomName = txtRoomName.getText().trim();
            String category = (String) cmbCategory.getSelectedItem();
            String timeStr = txtTime.getText().trim();

            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "방 이름을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                int time = Integer.parseInt(timeStr);
                if (time <= 0) throw new NumberFormatException();
                createRoom(roomName, category, time);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "올바른 시간을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void createRoom(String roomName, String category, int timeLimit) {
        try {
            String roomId = UUID.randomUUID().toString().substring(0, 8);
            GameRoom newRoom = new GameRoom(roomId, roomName, userName, category, timeLimit);
            String roomData = newRoom.toProtocolString();
            dos.writeUTF("/createRoom " + roomData);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "방 생성 실패: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void joinSelectedRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "참가할 방을 선택해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String roomName = (String) tableModel.getValueAt(selectedRow, 0);
        GameRoom selectedRoom = null;
        for (GameRoom room : roomMap.values()) {
            if (room.getRoomName().equals(roomName)) {
                selectedRoom = room;
                break;
            }
        }

        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this, "방을 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedRoom.isFull()) {
            JOptionPane.showMessageDialog(this, "방이 가득 찼습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedRoom.getStatus() != GameRoom.RoomStatus.WAITING) {
            JOptionPane.showMessageDialog(this, "이미 게임이 시작된 방입니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            dos.writeUTF("/joinRoom " + selectedRoom.getRoomId());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "방 참가 실패: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openGameView(GameRoom room, boolean isHost) {
        try {
            // 1. 현재 스레드 루프 종료를 위해 플래그 설정 (안전장치)
            isRunning = false;

            // 2. 게임 뷰 생성 (게임 뷰 내부에서 새로운 ListenNetwork가 시작됨)
            JavaChatClientView gameView = new JavaChatClientView(
                    userName, socket, dis, dos, room, isHost);
            gameView.setVisible(true);

            // 3. 현재 창 닫기
            this.dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "게임 화면 열기 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // ... (addRoomToTable 메서드 기존 동일) ...
    private void addRoomToTable(GameRoom room) {
        Object[] rowData = {
                room.getRoomName(),
                room.getHostName(),
                room.getCurrentPlayers() + "/" + room.getMaxPlayers(),
                room.getCategory(),
                room.getTimeLimit() + "초",
                room.getStatus() == GameRoom.RoomStatus.WAITING ? "대기중" : "게임중"
        };
        tableModel.addRow(rowData);
    }

    /**
     * 서버 메시지 수신 스레드 (수정됨)
     */
    class ListenNetwork extends Thread {
        public void run() {
            // isRunning 플래그 확인
            while (isRunning) {
                try {
                    String msg = dis.readUTF();

                    if (msg.startsWith("/roomList ")) {
                        String roomData = msg.substring(10);
                        updateRoomList(roomData);
                    }
                    else if (msg.startsWith("/roomCreated ")) {
                        String roomId = msg.substring(13);
                        GameRoom createdRoom = null;
                        for (GameRoom room : roomMap.values()) {
                            if (room.getRoomId().equals(roomId)) {
                                createdRoom = room;
                                break;
                            }
                        }
                        if (createdRoom != null) {
                            openGameView(createdRoom, true);
                            break; // [중요] 게임 화면으로 넘어가면 이 스레드는 즉시 종료해야 함
                        }
                    }
                    else if (msg.startsWith("/joinedRoom ")) {
                        String roomId = msg.substring(12);
                        GameRoom joinedRoom = roomMap.get(roomId);
                        if (joinedRoom != null) {
                            joinedRoom.addPlayer(userName);
                            openGameView(joinedRoom, false);
                            break; // [중요] 게임 화면으로 넘어가면 이 스레드는 즉시 종료해야 함
                        }
                    }
                } catch (IOException e) {
                    System.err.println("서버 연결 끊김 (RoomListUI)");
                    break;
                }
            }
        }
    }

    // ... (updateRoomList 메서드 기존 동일) ...
    private void updateRoomList(String roomData) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            roomMap.clear();

            if (roomData.trim().isEmpty()) {
                return;
            }

            String[] rooms = roomData.split(";;");
            for (String roomStr : rooms) {
                if (roomStr.trim().isEmpty()) continue;

                GameRoom room = GameRoom.fromProtocolString(roomStr);
                if (room != null) {
                    roomMap.put(room.getRoomId(), room);
                    addRoomToTable(room);
                }
            }
        });
    }
}