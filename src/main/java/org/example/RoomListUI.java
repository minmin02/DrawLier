package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
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

    // 로그인 시 사용하는 생성자
    public RoomListUI(String userName, String serverIp, String serverPort) {
        this.userName = userName;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.roomMap = new HashMap<>();

        connectToServer();
        initializeUI();
        requestRoomList();
    }

    // ★★★ [추가] 게임 방에서 로비로 돌아올 때 사용하는 생성자 ★★★
    public RoomListUI(String userName, String serverIp, String serverPort, Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.userName = userName;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.roomMap = new HashMap<>();

        // 서버에 다시 연결하거나 로그인할 필요 없음
        new ListenNetwork().start();
        initializeUI();
        requestRoomList();
    }


    private void connectToServer() {
        try {
            socket = new Socket(serverIp, Integer.parseInt(serverPort));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

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
            btn.setPreferredSize(new Dimension(BTN_WIDTH, BTN_HEIGHT));

            UIUtils.applyButtonEffects(btn);

        } catch (Exception e) {
            btn.setText(fallbackText);
            btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            btn.setPreferredSize(new Dimension(BTN_WIDTH, BTN_HEIGHT));
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

            Image img = icon.getImage().getScaledInstance(250, 50, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(img));
            label.setBorder(new EmptyBorder(0, 30, 0, 0));

        } catch (Exception e) {
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

        JPanel contentPane = new JPanel() {
            Image bgImage = null;
            {
                try {
                    bgImage = new ImageIcon(getClass().getResource("/RoomList/RoomListBackGround.png")).getImage();
                } catch (Exception e) {
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    Graphics2D g2d = (Graphics2D) g;
                    GradientPaint gp = new GradientPaint(0, 0, new Color(100, 150, 200),
                            0, getHeight(), new Color(50, 100, 150));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        contentPane.setBorder(new EmptyBorder(80, 50, 10, 50));
        contentPane.setLayout(new BorderLayout(20, 20));
        setContentPane(contentPane);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = createImageTitle("/RoomList/GameListTitle.png", "게임 방 목록");
        topPanel.add(titleLabel, BorderLayout.WEST);

        JLabel userInfoLabel = new JLabel("접속자: " + userName);
        userInfoLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        userInfoLabel.setForeground(Color.WHITE);
        userInfoLabel.setText("<html><span style='text-shadow: 1px 1px 2px #000000;'>접속자: " + userName + "</span></html>");
        userInfoLabel.setBorder(new EmptyBorder(0, 0, 0, 400));
        topPanel.add(userInfoLabel, BorderLayout.EAST);

        contentPane.add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"방 이름", "방장", "인원", "카테고리", "상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        roomTable = new JTable(tableModel);
        roomTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        roomTable.setRowHeight(35);

        roomTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
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
        tableWrapper.setBorder(new EmptyBorder(0, 40, 0, 350));
        contentPane.add(tableWrapper, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(70, 0, 10, 0));

        btnRefresh = createImageButton("/RoomList/Reroad.png", "새로고침");
        btnRefresh.addActionListener(e -> requestRoomList());

        btnJoinRoom = createImageButton("/RoomList/EnterRoomBtn.png", "방 참가");
        btnJoinRoom.addActionListener(e -> joinSelectedRoom());

        btnCreateRoom = createImageButton("/RoomList/CreateRoomBtn.png", "방 만들기");
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
        dialog.setSize(543, 369);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel(new GridBagLayout()) {
            Image bgImage = null;
            private final int CORNER_RADIUS = 30;

            {
                try {
                    bgImage = new ImageIcon(getClass().getResource("/RoomList/createRoom.png")).getImage();
                } catch (Exception e) {
                    System.err.println("경고: 배경 이미지를 찾을 수 없습니다: /RoomList/createRoom.png");
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS));
                if (bgImage != null) {
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2.setColor(new Color(240, 240, 240));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 60, 20, 60));

        JLabel lblRoomName = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/RoomList/RoomName.png"));
            Image img = icon.getImage().getScaledInstance(120, 30, Image.SCALE_SMOOTH);
            lblRoomName.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            lblRoomName.setText("방 이름:");
            System.err.println("경고: 방 이름 이미지를 찾을 수 없습니다.");
        }

        JTextField txtRoomName = new JTextField();

        JLabel lblCategory = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/RoomList/category.png"));
            Image img = icon.getImage().getScaledInstance(120, 30, Image.SCALE_SMOOTH);
            lblCategory.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            lblCategory.setText("카테고리:");
            System.err.println("경고: 카테고리 이미지를 찾을 수 없습니다.");
        }

        String[] categories = GameCategory.getCategoryNames();
        JComboBox<String> cmbCategory = new JComboBox<>(categories);

        JButton btnCreate = new JButton();
        JButton btnCancel = new JButton();

        Dimension buttonSize = new Dimension(120, 56);
        btnCreate.setPreferredSize(buttonSize);
        btnCancel.setPreferredSize(buttonSize);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/RoomList/createBtn.png"));
            Image img = icon.getImage().getScaledInstance(buttonSize.width, buttonSize.height, Image.SCALE_SMOOTH);
            btnCreate.setIcon(new ImageIcon(img));
            UIUtils.applyButtonEffects(btnCreate);
        } catch (Exception e) {
            btnCreate.setText("생성");
        }

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/RoomList/cancelBtn.png"));
            Image img = icon.getImage().getScaledInstance(buttonSize.width, buttonSize.height, Image.SCALE_SMOOTH);
            btnCancel.setIcon(new ImageIcon(img));
            UIUtils.applyButtonEffects(btnCancel);
        } catch (Exception e) {
            btnCancel.setText("취소");
        }

        lblRoomName.setOpaque(false);
        lblCategory.setOpaque(false);
        cmbCategory.setOpaque(false);

        Dimension inputSize = new Dimension(200, 30);
        txtRoomName.setPreferredSize(inputSize);
        cmbCategory.setPreferredSize(inputSize);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 0.3;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel.add(Box.createVerticalGlue(), gbc);

        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(lblRoomName, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(txtRoomName, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(50, 5, 10, 5);
        panel.add(lblCategory, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(50, 5, 10, 5);
        panel.add(cmbCategory, gbc);

        gbc.insets = new Insets(10, 5, 10, 5);
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonContainer.setOpaque(false);
        buttonContainer.add(btnCancel);
        buttonContainer.add(btnCreate);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.weighty = 1.0; gbc.fill = GridBagConstraints.NONE;
        panel.add(buttonContainer, gbc);

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
            GameRoom newRoom = new GameRoom(roomId, roomName, userName, category, 4);
            newRoom.addPlayer(userName);
            this.pendingRoom = newRoom;

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
                            GameRoom cleanRoom = new GameRoom(
                                    listRoom.getRoomId(),
                                    listRoom.getRoomName(),
                                    listRoom.getHostName(),
                                    listRoom.getCategory(),
                                    listRoom.getMaxPlayers()
                            );

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
                    String[] parts = roomStr.split("\\|");
                    if (parts.length >= 6) {
                        String roomId = parts[0];
                        String roomName = parts[1];
                        String hostName = parts[2];
                        int currentPlayers = Integer.parseInt(parts[3]);
                        int maxPlayers = Integer.parseInt(parts[4]);
                        String category = parts[5];

                        GameRoom room = new GameRoom(roomId, roomName, hostName, category, maxPlayers);

                        for (int i = 0; i < currentPlayers; i++) {
                            if (i == 0) {
                                room.addPlayer(hostName);
                            } else {
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
