package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataOutputStream;

/**
 * 게임 결과 UI
 * 시민 승리 또는 라이어 승리 표시
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

        JPanel contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // 승리 팀에 따라 그라데이션 배경 변경
                GradientPaint gp;
                if (citizenWin) {
                    gp = new GradientPaint(
                            0, 0, new Color(220, 240, 255),
                            0, getHeight(), new Color(180, 220, 255)
                    );
                } else {
                    gp = new GradientPaint(
                            0, 0, new Color(255, 235, 240),
                            0, getHeight(), new Color(255, 210, 220)
                    );
                }
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        contentPane.setBorder(new EmptyBorder(40, 40, 40, 40));
        contentPane.setLayout(new BorderLayout(20, 20));
        setContentPane(contentPane);

        // 상단 결과 패널
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        // 결과 아이콘 - 애니메이션 효과를 위한 크기 변화
        JLabel lblIcon = new JLabel(citizenWin ? "✅" : "🎭");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 90));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 승리 팀 표시
        JLabel lblWinner = new JLabel(citizenWin ? "시민 승리!" : "라이어 승리!");
        lblWinner.setFont(new Font("맑은 고딕", Font.BOLD, 42));
        lblWinner.setForeground(citizenWin ? new Color(40, 167, 69) : new Color(220, 53, 69));
        lblWinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblWinner.setBorder(new EmptyBorder(15, 0, 10, 0));

        // 서브 타이틀
        JLabel lblSubtitle = new JLabel(citizenWin ? "시민들이 라이어를 찾아냈습니다!" : "라이어가 시민들을 속였습니다!");
        lblSubtitle.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        lblSubtitle.setForeground(citizenWin ? new Color(50, 120, 70) : new Color(180, 50, 60));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(lblIcon);
        topPanel.add(lblWinner);
        topPanel.add(lblSubtitle);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // 중앙 메시지 패널
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 결과 메시지를 담을 패널
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(new Color(255, 255, 255, 230));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(citizenWin ? new Color(40, 167, 69) : new Color(220, 53, 69), 3, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // 메시지 제목
        JLabel lblMessageTitle = new JLabel("게임 결과 상세");
        lblMessageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblMessageTitle.setForeground(new Color(50, 50, 50));
        lblMessageTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        // 결과 메시지
        JTextArea txtMessage = new JTextArea(resultMessage);
        txtMessage.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        txtMessage.setForeground(new Color(70, 70, 70));
        txtMessage.setEditable(false);
        txtMessage.setOpaque(false);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        txtMessage.setBorder(new EmptyBorder(5, 0, 0, 0));

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(lblMessageTitle, BorderLayout.NORTH);
        textPanel.add(txtMessage, BorderLayout.CENTER);

        messagePanel.add(textPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        centerPanel.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 0));

        // 확인 버튼
        JButton btnConfirm = new JButton("확인");
        btnConfirm.setPreferredSize(new Dimension(200, 55));
        btnConfirm.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        btnConfirm.setBackground(citizenWin ? new Color(40, 167, 69) : new Color(220, 53, 69));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setBorderPainted(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 버튼 호버 효과
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
                    // 서버에 방 나가기 알림
                    if (dos != null) {
                        dos.writeUTF("/leaveRoom");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // 프로그램 종료 (실제로는 로비로 돌아가는 로직 필요)
                SwingUtilities.invokeLater(() -> {
                    RoomListUI roomListUI = new RoomListUI(userName, serverIp, serverPort);
                    roomListUI.setVisible(true);
                    dispose();
//                    JOptionPane.showMessageDialog(this,
//                            "게임을 종료합니다.\n" +
//                                    "다시 플레이하려면 프로그램을 재시작해주세요.",
//                            "게임 종료",
//                            JOptionPane.INFORMATION_MESSAGE);
//                    System.exit(0);
                });
            }
        });

        bottomPanel.add(btnConfirm);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        // 창 닫기 이벤트 처리
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

        // 결과 표시 애니메이션 효과
        animateResult(lblIcon);
    }

    // 간단한 아이콘 애니메이션
    private void animateResult(JLabel iconLabel) {
        Timer timer = new Timer(100, null);
        final int[] size = {70};
        final boolean[] growing = {true};

        timer.addActionListener(e -> {
            if (growing[0]) {
                size[0] += 5;
                if (size[0] >= 100) {
                    growing[0] = false;
                }
            } else {
                size[0] -= 5;
                if (size[0] <= 90) {
                    timer.stop();
                }
            }
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, size[0]));
        });

        timer.start();
    }
}