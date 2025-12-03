package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.Socket;
import java.net.URL;

public class ResultUI extends JFrame {
    private String userName;
    private boolean citizenWin;
    private String resultMessage;
    private String roomId;
    private String serverIp;
    private String serverPort;

    // ★★★ [추가] 소켓 및 스트림 필드
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    // ★★★ [수정] 생성자에서 소켓과 스트림을 받도록 변경
    public ResultUI(String userName, boolean citizenWin, String resultMessage, String roomId, String serverIp, String serverPort, Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.userName = userName;
        this.citizenWin = citizenWin;
        this.resultMessage = resultMessage;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        initializeUI();
    }

    private void initializeUI() {
        setTitle("DrawLier - 게임 결과");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 650, 550);

        JPanel contentPane = new JPanel() {
            private BufferedImage bgImage;
            {
                try {
                    String fileName = citizenWin ? "CitizenWin.png" : "LiarWin.png";
                    URL imageUrl = getClass().getResource("/ResultTabUI/" + fileName);
                    if (imageUrl != null) {
                        bgImage = ImageIO.read(imageUrl);
                    } else {
                        System.err.println("이미지 리소스를 찾을 수 없습니다: /ResultTabUI/" + fileName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(200, 175, 60, 100));

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);

        JLabel lblMessageTitle = new JLabel("게임 결과 상세");
        lblMessageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblMessageTitle.setForeground(Color.WHITE);
        lblMessageTitle.setBorder(new EmptyBorder(0, 0, 5, 0));

        JTextArea txtMessage = new JTextArea(resultMessage);
        txtMessage.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        txtMessage.setForeground(Color.WHITE);
        txtMessage.setEditable(false);
        txtMessage.setOpaque(false);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(lblMessageTitle, BorderLayout.NORTH);
        textPanel.add(txtMessage, BorderLayout.CENTER);
        messagePanel.add(textPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 30, 0));

        JButton btnConfirm = new JButton();
        btnConfirm.setPreferredSize(new Dimension(100, 30));
        try {
            URL btnUrl = getClass().getResource("/PlayUI/GoToLobby.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(100, 30, Image.SCALE_SMOOTH);
                btnConfirm.setIcon(new ImageIcon(img));
                UIUtils.applyButtonEffects(btnConfirm);
            } else {
                btnConfirm.setText("로비로 이동");
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnConfirm.setText("로비로 이동");
        }

        // ★★★ [수정] 로비로 이동 시 기존 연결 사용 ★★★
        btnConfirm.addActionListener(e -> {
            try {
                if (dos != null) {
                    dos.writeUTF("/leaveRoom");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                // RoomListUI의 새 생성자 호출
                new RoomListUI(userName, serverIp, serverPort, socket, dis, dos).setVisible(true);
                dispose();
            });
        });

        bottomPanel.add(btnConfirm);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    if (dos != null) {
                        dos.writeUTF("/leaveRoom");
                        dos.close();
                    }
                    if(dis != null) dis.close();
                    if(socket != null) socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }
}
