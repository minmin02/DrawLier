package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * 방 목록 UI
 * 수정사항: 버튼 클릭 이벤트 리스너 복구 및 디자인 유지
 */
public class RoomListUI extends JFrame {

    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton btnCreateRoom;
    private JButton btnJoinRoom;
    private JButton btnRefresh;

    private String userName;
    private String serverIp;
    private String serverPort;

    private Map<String, GameRoom> roomMap;
    private GameRoom pendingRoom;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

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

    private void initializeUI() {
        setTitle("DrawLier - 방 목록");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 배경 패널 설정
        JPanel contentPane = new JPanel() {
            Image bgImage = new ImageIcon(getClass().getResource("/pino.jpg")).getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        // 전체 여백
        contentPane.setBorder(new EmptyBorder(80, 50, 10, 50));
        contentPane.setLayout(new BorderLayout(20, 20));
        setContentPane(contentPane);

        // 2. 상단 패널 (제목)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("게임 방 목록");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setText("<html><span style='text-shadow: 2px 2px 4px #000000;'>게임 방 목록</span></html>");
        titleLabel.setBorder(new EmptyBorder(0, 30, 0, 0));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JLabel userInfoLabel = new JLabel("접속자: " + userName);
        userInfoLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        userInfoLabel.setForeground(Color.WHITE);
        userInfoLabel.setText("<html><span style='text-shadow: 1px 1px 2px #000000;'>접속자: " + userName + "</span></html>");
        userInfoLabel.setBorder(new EmptyBorder(0, 0, 0, 300));
        topPanel.add(userInfoLabel, BorderLayout.EAST);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // 3. 중앙 테이블 영역
        String[] columnNames = {"방 이름", "방장", "인원", "카테고리", "시간", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        roomTable = new JTable(tableModel);
        roomTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        roomTable.setRowHeight(35);
        roomTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
        roomTable.getTableHeader().setBackground(new Color(0, 0, 0, 0));
        roomTable.getTableHeader().setOpaque(false);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.getViewport().setBackground(new Color(255, 255, 255, 180));
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(scrollPane, BorderLayout.CENTER);
        tableWrapper.setBorder(new EmptyBorder(0, 40, 0, 300));

        contentPane.add(tableWrapper, BorderLayout.CENTER);

        // 4. 하단 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(70, 0, 10, 0));

        btnRefresh = new JButton("새로고침");
        styleButton(btnRefresh, new Color(255, 255, 255), Color.BLACK);
        // [복구됨] 새로고침 버튼 기능
        btnRefresh.addActionListener(e -> requestRoomList());

        btnJoinRoom = new JButton("방 참가");
        styleButton(btnJoinRoom, new Color(66, 133, 244), Color.WHITE);
        // [복구됨] 방 참가 버튼 기능
        btnJoinRoom.addActionListener(e -> joinSelectedRoom());

        btnCreateRoom = new JButton("방 만들기");
        styleButton(btnCreateRoom, new Color(220, 53, 69), Color.WHITE);
        // [복구됨] 방 만들기 버튼 기능
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

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(130, 45));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,50), 1));
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
            this.pendingRoom = newRoom;
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
            isRunning = false;
            JavaChatClientView gameView = new JavaChatClientView(
                    userName, socket, dis, dos, room, isHost);
            gameView.setVisible(true);
            this.dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "게임 화면 열기 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

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

    class ListenNetwork extends Thread {
        public void run() {
            while (isRunning) {
                try {
                    String msg = dis.readUTF();

                    if (msg.startsWith("/roomList ")) {
                        String roomData = msg.substring(10);
                        updateRoomList(roomData);
                    }
                    else if (msg.startsWith("/roomCreated ")) {
                        String roomId = msg.substring(13);
                        GameRoom createdRoom = roomMap.get(roomId);
                        if (createdRoom == null && pendingRoom != null && pendingRoom.getRoomId().equals(roomId)) {
                            createdRoom = pendingRoom;
                        }
                        if (createdRoom != null) {
                            openGameView(createdRoom, true);
                            break;
                        }
                    }
                    else if (msg.startsWith("/joinedRoom ")) {
                        String roomId = msg.substring(12);
                        GameRoom joinedRoom = roomMap.get(roomId);
                        if (joinedRoom != null) {
                            joinedRoom.addPlayer(userName);
                            openGameView(joinedRoom, false);
                            break;
                        }
                    }
                    else if (msg.startsWith("/playerJoined") || msg.startsWith("/playerLeft")) {
                        requestRoomList();
                    }
                } catch (IOException e) {
                    System.err.println("서버 연결 끊김 (RoomListUI)");
                    break;
                }
            }
        }
    }

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