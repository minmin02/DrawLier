package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * 방 목록 UI - GameRoom 턴 시스템과 통합
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

    /**
     * 이미지를 로드하여 버튼을 생성하고 스타일을 적용합니다.
     */
    private JButton createImageButton(String imagePath, String fallbackText) {
        JButton btn = new JButton();
        final int BTN_WIDTH = 130;
        final int BTN_HEIGHT = 45;

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            if (icon.getIconWidth() == -1) {
                throw new Exception("이미지를 찾을 수 없습니다: " + imagePath);
            }

            Image img = icon.getImage().getScaledInstance(BTN_WIDTH, BTN_HEIGHT, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));

            // 이미지 버튼 스타일 적용: 투명, 테두리 없음
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setPreferredSize(new Dimension(BTN_WIDTH, BTN_HEIGHT));

        } catch (Exception e) {
            // 이미지 로드 실패 시 텍스트 버튼으로 대체
            btn.setText(fallbackText);
            btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            btn.setPreferredSize(new Dimension(BTN_WIDTH, BTN_HEIGHT));
            // 대체 텍스트 버튼 스타일 (기존 styleButton 스타일 적용)
            Color bg = (fallbackText.equals("새로고침")) ? new Color(255, 255, 255) :
                    (fallbackText.equals("방 참가")) ? new Color(66, 133, 244) : new Color(220, 53, 69);
            Color fg = (fallbackText.equals("새로고침")) ? Color.BLACK : Color.WHITE;
            btn.setBackground(bg);
            btn.setForeground(fg);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,50), 1));
            System.err.println("경고: " + e.getMessage());
        }
        return btn;
    }

    /**
     * 이미지를 로드하여 제목 라벨을 생성합니다.
     */
    private JLabel createImageTitle(String imagePath, String fallbackText) {
        JLabel label = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            if (icon.getIconWidth() == -1) {
                throw new Exception("이미지를 찾을 수 없습니다: " + imagePath);
            }

            // 이미지 크기는 UI에 맞게 적절히 조정 (예: 250x50)
            Image img = icon.getImage().getScaledInstance(250, 50, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(img));
            label.setBorder(new EmptyBorder(0, 30, 0, 0)); // 좌측 여백 유지

        } catch (Exception e) {
            // 이미지 로드 실패 시 텍스트로 대체 (기존 스타일 유지)
            label.setText("<html><span style='text-shadow: 2px 2px 4px #000000;'>게임 방 목록</span></html>");
            label.setFont(new Font("맑은 고딕", Font.BOLD, 30));
            label.setForeground(Color.WHITE);
            label.setBorder(new EmptyBorder(0, 30, 0, 0));
            System.err.println("경고: " + e.getMessage());
        }
        return label;
    }

    private void initializeUI() {
        setTitle("DrawLier - 방 목록");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. 배경 패널 설정 (기존 코드 유지)
        JPanel contentPane = new JPanel() {
            Image bgImage = null;
            {
                try {
                    // 배경 이미지 경로는 /RoomList/RoomListBackGround.png로 가정합니다.
                    bgImage = new ImageIcon(getClass().getResource("/RoomList/RoomListBackGround.png")).getImage();
                } catch (Exception e) {
                    // 이미지 없으면 배경색으로 대체
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    // 기본 그라데이션 배경
                    Graphics2D g2d = (Graphics2D) g;
                    GradientPaint gp = new GradientPaint(0, 0, new Color(100, 150, 200),
                            0, getHeight(), new Color(50, 100, 150));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        // 전체 여백
        contentPane.setBorder(new EmptyBorder(80, 50, 10, 50));
        contentPane.setLayout(new BorderLayout(20, 20));
        setContentPane(contentPane);

        // 2. 상단 패널 (제목)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        // --- [수정] 제목을 이미지로 대체 ---
        JLabel titleLabel = createImageTitle("/RoomList/GameListTitle.png", "게임 방 목록");
        topPanel.add(titleLabel, BorderLayout.WEST);
        // ------------------------------

        JLabel userInfoLabel = new JLabel("접속자: " + userName);
        userInfoLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        userInfoLabel.setForeground(Color.WHITE);
        userInfoLabel.setText("<html><span style='text-shadow: 1px 1px 2px #000000;'>접속자: " + userName + "</span></html>");
        userInfoLabel.setBorder(new EmptyBorder(0, 0, 0, 400));
        topPanel.add(userInfoLabel, BorderLayout.EAST);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // 3. 중앙 테이블 영역 (기존 코드 유지)
// 3. 중앙 테이블 영역 (수정된 부분)
        String[] columnNames = {"방 이름", "방장", "인원", "카테고리", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        roomTable = new JTable(tableModel);
        roomTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        roomTable.setRowHeight(35);

        roomTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));

// 연한 하늘색 배경
        roomTable.getTableHeader().setBackground(new Color(173, 216, 230));
        roomTable.getTableHeader().setOpaque(true);

        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.getViewport().setBackground(new Color(255, 255, 255, 180));
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(scrollPane, BorderLayout.CENTER);
// 왼쪽 여백 40, 오른쪽 여백 350으로 설정하여 테이블을 왼쪽으로 이동하고 너비 축소
        tableWrapper.setBorder(new EmptyBorder(0, 40, 0, 350));
        contentPane.add(tableWrapper, BorderLayout.CENTER);

        // 4. 하단 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(70, 0, 10, 0));

        // --- [수정] 버튼들을 이미지 버튼으로 대체 ---
        btnRefresh = createImageButton("/RoomList/Reroad.png", "새로고침");
        btnRefresh.addActionListener(e -> requestRoomList());

        btnJoinRoom = createImageButton("/RoomList/EnterRoomBtn.png", "방 참가");
        btnJoinRoom.addActionListener(e -> joinSelectedRoom());

        btnCreateRoom = createImageButton("/RoomList/CreateRoomBtn.png", "방 만들기");
        btnCreateRoom.addActionListener(e -> openCreateRoomDialog());
        // ----------------------------------------

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

    // styleButton 메서드는 더 이상 사용되지 않으므로 제거하거나 주석 처리합니다.
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
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblRoomName = new JLabel("방 이름:");
        JTextField txtRoomName = new JTextField();

        JLabel lblCategory = new JLabel("카테고리:");
        // GameCategory.getCategoryNames()이 정의되어 있다고 가정
        String[] categories = new String[]{"기본", "동물", "음식"}; // 임시 카테고리
        // String[] categories = GameCategory.getCategoryNames();
        JComboBox<String> cmbCategory = new JComboBox<>(categories);

        JButton btnCreate = new JButton("생성");
        JButton btnCancel = new JButton("취소");

        panel.add(lblRoomName);
        panel.add(txtRoomName);
        panel.add(lblCategory);
        panel.add(cmbCategory);
        panel.add(btnCancel);
        panel.add(btnCreate);

        btnCreate.addActionListener(e -> {
            String roomName = txtRoomName.getText().trim();
            String category = (String) cmbCategory.getSelectedItem();

            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "방 이름을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            createRoom(roomName, category);
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void createRoom(String roomName, String category) {
        try {
            String roomId = UUID.randomUUID().toString().substring(0, 8);
            // GameRoom 클래스가 정의되어 있다고 가정
            GameRoom newRoom = new GameRoom(roomId, roomName, userName, category, 4);
            newRoom.addPlayer(userName); // 방장을 미리 추가
            this.pendingRoom = newRoom;

            // 프로토콜: roomId|roomName|hostName|currentPlayers|maxPlayers|category|timeLimit|status
            String roomData = String.format("%s|%s|%s|%d|%d|%s|60|WAITING",
                    roomId, roomName, userName, 1, 4, category);

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

        // 인원 체크
        if (selectedRoom.getPlayers().size() >= 4) {
            JOptionPane.showMessageDialog(this, "방이 가득 찼습니다. (4/4)", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedRoom.isGameRunning()) {
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
            // JavaChatClientView 클래스가 정의되어 있다고 가정
            JavaChatClientView gameView = new JavaChatClientView(
                    userName, socket, dis, dos, room, isHost, serverIp, serverPort);
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
                room.getPlayers().size() + "/" + room.getMaxPlayers(),
                room.getCategory(),
                room.isGameRunning() ? "게임중" : "대기중"
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

                        if (pendingRoom != null && pendingRoom.getRoomId().equals(roomId)) {
                            openGameView(pendingRoom, true);
                            break;
                        }
                    }
                    else if (msg.startsWith("/joinedRoom ")) {
                        String data = msg.substring(12);
                        String[] parts = data.split("\\|");
                        String roomId = parts[0];

                        GameRoom listRoom = roomMap.get(roomId);
                        if (listRoom != null) {
                            // 더미 데이터 없는 새 GameRoom 생성
                            GameRoom cleanRoom = new GameRoom(
                                    listRoom.getRoomId(),
                                    listRoom.getRoomName(),
                                    listRoom.getHostName(),
                                    listRoom.getCategory(),
                                    listRoom.getMaxPlayers()
                            );

                            // ★ 서버에서 받은 전체 플레이어 목록 추가
                            if (parts.length > 1 && !parts[1].isEmpty()) {
                                String[] players = parts[1].split(",");
                                for (String player : players) {
                                    if (!player.trim().isEmpty()) {
                                        cleanRoom.addPlayer(player.trim());
                                    }
                                }
                            }

                            openGameView(cleanRoom, false);
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

                try {
                    // 프로토콜: roomId|roomName|hostName|currentPlayers|maxPlayers|category|timeLimit|status
                    String[] parts = roomStr.split("\\|");
                    if (parts.length >= 6) {
                        String roomId = parts[0];
                        String roomName = parts[1];
                        String hostName = parts[2];
                        int currentPlayers = Integer.parseInt(parts[3]);
                        int maxPlayers = Integer.parseInt(parts[4]);
                        String category = parts[5];

                        // GameRoom 클래스가 정의되어 있다고 가정
                        GameRoom room = new GameRoom(roomId, roomName, hostName, category, maxPlayers);

                        // ★ 방 목록 표시를 위해 더미 플레이어 추가 (UI용)
                        for (int i = 0; i < currentPlayers; i++) {
                            if (i == 0) {
                                room.addPlayer(hostName);
                            } else {
                                // 실제 서버에서 플레이어 목록을 주지 않으므로, 더미 데이터로 표시 인원만 맞춤
                                room.addPlayer("Player" + i);
                            }
                        }

                        roomMap.put(roomId, room);
                        addRoomToTable(room);
                    }
                } catch (Exception e) {
                    System.err.println("방 정보 파싱 오류: " + roomStr);
                    e.printStackTrace();
                }
            }
        });
    }
}