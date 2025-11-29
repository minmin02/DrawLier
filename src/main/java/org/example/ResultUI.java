package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * 게임 결과 UI - 수정됨
 * 메시지 박스 크기를 배경 이미지의 주황색 영역에 맞춰 조정
 */
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
                // 초기화 블록에서 이미지 로드
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
                    System.err.println("이미지 로드 중 에러 발생");
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    // 이미지를 패널 크기에 맞춰서 꽉 차게 그리기
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0)); // 전체 패널 여백 제거
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        // 중앙 메시지 패널 (투명)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);

        centerPanel.setBorder(new EmptyBorder(200, 130, 60, 130));

        // 결과 메시지를 담을 패널 (흰색 반투명 박스)
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(new Color(255, 255, 255, 200));

        // 테두리 두께 및 색상 유지
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(citizenWin ? new Color(40, 167, 69) : new Color(220, 53, 69), 2, true),
                new EmptyBorder(10, 15, 10, 15) // 내부 텍스트 여백
        ));

        // 메시지 제목
        JLabel lblMessageTitle = new JLabel("게임 결과 상세");
        lblMessageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblMessageTitle.setForeground(new Color(50, 50, 50));
        lblMessageTitle.setBorder(new EmptyBorder(0, 0, 5, 0));

        // 결과 메시지 텍스트 영역
        JTextArea txtMessage = new JTextArea(resultMessage);
        txtMessage.setFont(new Font("맑은 고딕", Font.PLAIN, 13)); // 폰트 크기 약간 조정
        txtMessage.setForeground(new Color(70, 70, 70));
        txtMessage.setEditable(false);
        txtMessage.setOpaque(false);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(lblMessageTitle, BorderLayout.NORTH);
        textPanel.add(txtMessage, BorderLayout.CENTER);

        messagePanel.add(textPanel, BorderLayout.CENTER);

        // 스크롤판 없이 바로 붙이거나, 스크롤바가 필요하다면 작게 설정
        // 박스가 작아졌으므로 스크롤이 생길 수 있게 처리
        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        // 스크롤바 안 보이게 설정 (깔끔함을 위해)
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        centerPanel.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setBorder(new EmptyBorder(0, 0, 30, 0)); // 하단에서 30px 띄움

        // 확인 버튼
        JButton btnConfirm = new JButton("확인");
        btnConfirm.setPreferredSize(new Dimension(180, 50));
        btnConfirm.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        btnConfirm.setBackground(citizenWin ? new Color(40, 167, 69) : new Color(220, 53, 69));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setBorderPainted(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConfirm.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (citizenWin) {
                    btnConfirm.setBackground(new Color(33, 140, 58));
                } else {
                    btnConfirm.setBackground(new Color(200, 35, 51));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnConfirm.setBackground(citizenWin ? new Color(40, 167, 69) : new Color(220, 53, 69));
            }
        });

        btnConfirm.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "게임을 종료하고 로비로 돌아가시겠습니까?",
                    "게임 종료",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    if (dos != null) {
                        dos.writeUTF("/leaveRoom");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                SwingUtilities.invokeLater(() -> {
                    RoomListUI roomListUI = new RoomListUI(userName, serverIp, serverPort);
                    roomListUI.setVisible(true);
                    dispose();
                });
            }
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