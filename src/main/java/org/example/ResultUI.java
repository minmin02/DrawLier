package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

public class ResultUI extends JFrame {
    private String userName;
    private boolean citizenWin;
    private String resultMessage;
    private DataOutputStream dos;
    private String roomId;
    private String serverIp;
    private String serverPort;

    public ResultUI(String userName, boolean citizenWin, String resultMessage, DataOutputStream dos, String roomId, String serverIp, String serverPort) {
        this.userName = userName;
        this.citizenWin = citizenWin;
        this.resultMessage = resultMessage;
        this.dos = dos;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        initializeUI();
    }

    private void initializeUI() {
        setTitle("DrawLier - 게임 결과");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 650, 550);

        // 배경 이미지를 그리는 패널 설정
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

        // 중앙 메시지 패널 (투명)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);

        centerPanel.setBorder(new EmptyBorder(200, 175, 60, 100));

        // 결과 메시지를 담을 패널
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setOpaque(false); // 배경 투명

        // 메시지 제목
        JLabel lblMessageTitle = new JLabel("게임 결과 상세");
        lblMessageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblMessageTitle.setForeground(Color.WHITE);
        lblMessageTitle.setBorder(new EmptyBorder(0, 0, 5, 0));

        // 결과 메시지 텍스트 영역
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

        // 하단 버튼 패널
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setBorder(new EmptyBorder(0, 0, 30, 0));

        // 이미지 버튼 생성
        JButton btnConfirm = new JButton();
        int btnWidth = 180;
        int btnHeight = 50;
        btnConfirm.setPreferredSize(new Dimension(btnWidth, btnHeight));

        try {
            URL btnUrl = getClass().getResource("/PlayUI/GoToLobby.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(btnWidth, btnHeight, Image.SCALE_SMOOTH);
                btnConfirm.setIcon(new ImageIcon(img));
            } else {
                btnConfirm.setText("로비로 이동");
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnConfirm.setText("로비로 이동");
        }

        // 이미지 버튼 스타일 적용 (투명화)
        btnConfirm.setBorderPainted(false);
        btnConfirm.setContentAreaFilled(false);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setOpaque(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));

        //로비로 이동
        btnConfirm.addActionListener(e -> {
            try {
                // 서버에 방 나가기 신호 전송
                if (dos != null) {
                    dos.writeUTF("/leaveRoom");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // 즉시 UI 전환
            SwingUtilities.invokeLater(() -> {
                RoomListUI roomListUI = new RoomListUI(userName, serverIp, serverPort);
                roomListUI.setVisible(true);
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
                    }
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