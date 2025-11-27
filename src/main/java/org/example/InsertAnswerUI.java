package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 라이어 정답 입력 UI
 * 라이어가 최다 득표되었을 때, 정답을 맞출 마지막 기회
 */
public class InsertAnswerUI extends JFrame {
    private String userName;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String roomId;
    private String serverIp;
    private String serverPort;

    private JTextField txtAnswer;
    private JButton btnSubmit;
    private boolean hasSubmitted = false;

    public InsertAnswerUI(String userName, DataOutputStream dos, DataInputStream dis, String roomId, String serverIp, String serverPort) {
        this.userName = userName;
        this.dos = dos;
        this.dis = dis;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        initializeUI();
        new ListenResult().start();
    }

    private void initializeUI() {
        setTitle("DrawLier - 마지막 기회!");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 550, 400);

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(30, 30, 30, 30));
        contentPane.setLayout(new BorderLayout(20, 20));

        // 그라데이션 배경
        contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(255, 230, 230),
                        0, getHeight(), new Color(255, 200, 200)
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPane.setBorder(new EmptyBorder(30, 30, 30, 30));
        contentPane.setLayout(new BorderLayout(20, 20));
        setContentPane(contentPane);

        // 상단 안내 패널
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel lblIcon = new JLabel("🎭", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("라이어가 적발되었습니다!");
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        lblTitle.setForeground(new Color(220, 53, 69));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel lblInfo = new JLabel("정답을 맞추면 역전 승리!");
        lblInfo.setFont(new Font("맑은 고딕", Font.BOLD, 17));
        lblInfo.setForeground(new Color(150, 50, 50));
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblInfo.setBorder(new EmptyBorder(5, 0, 0, 0));

        JLabel lblHint = new JLabel("(힌트: 카테고리를 참고하세요)");
        lblHint.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        lblHint.setForeground(new Color(100, 100, 100));
        lblHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblHint.setBorder(new EmptyBorder(3, 0, 0, 0));

        topPanel.add(lblIcon);
        topPanel.add(lblTitle);
        topPanel.add(lblInfo);
        topPanel.add(lblHint);
        contentPane.add(topPanel, BorderLayout.NORTH);

        // 중앙 입력 패널
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel lblPrompt = new JLabel("정답 키워드를 입력하세요:");
        lblPrompt.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        lblPrompt.setForeground(new Color(50, 50, 50));
        lblPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtAnswer = new JTextField();
        txtAnswer.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        txtAnswer.setMaximumSize(new Dimension(400, 55));
        txtAnswer.setHorizontalAlignment(JTextField.CENTER);
        txtAnswer.setBackground(Color.WHITE);
        txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 53, 69), 3, true),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // 입력 필드 포커스 효과
        txtAnswer.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 100, 100), 3, true),
                        new EmptyBorder(12, 20, 12, 20)
                ));
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 53, 69), 3, true),
                        new EmptyBorder(12, 20, 12, 20)
                ));
            }
        });

        txtAnswer.addActionListener(e -> submitAnswer());

        centerPanel.add(lblPrompt);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(txtAnswer);

        contentPane.add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);

        btnSubmit = new JButton("정답 제출하기");
        btnSubmit.setPreferredSize(new Dimension(220, 55));
        btnSubmit.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        btnSubmit.setBackground(new Color(220, 53, 69));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 버튼 호버 효과
        btnSubmit.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btnSubmit.isEnabled()) {
                    btnSubmit.setBackground(new Color(200, 35, 51));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btnSubmit.isEnabled()) {
                    btnSubmit.setBackground(new Color(220, 53, 69));
                }
            }
        });

        btnSubmit.addActionListener(e -> submitAnswer());

        bottomPanel.add(btnSubmit);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);

        // 입력 필드에 포커스
        SwingUtilities.invokeLater(() -> txtAnswer.requestFocus());
    }

    private void submitAnswer() {
        if (hasSubmitted) {
            return;
        }

        String answer = txtAnswer.getText().trim();

        if (answer.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "정답을 입력해주세요!",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            txtAnswer.requestFocus();
            return;
        }

        try {
            dos.writeUTF("/liarAnswer " + answer);
            hasSubmitted = true;

            btnSubmit.setEnabled(false);
            btnSubmit.setBackground(Color.GRAY);
            btnSubmit.setText("제출 완료...");
            txtAnswer.setEnabled(false);

            // 대기 메시지 표시
            JOptionPane.showMessageDialog(this,
                    "정답을 제출했습니다.\n결과를 확인하는 중...",
                    "제출 완료",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            hasSubmitted = false;
            JOptionPane.showMessageDialog(this,
                    "정답 전송 중 오류가 발생했습니다.\n다시 시도해주세요.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            btnSubmit.setEnabled(true);
            btnSubmit.setBackground(new Color(220, 53, 69));
            btnSubmit.setText("정답 제출하기");
            txtAnswer.setEnabled(true);
        }
    }

    class ListenResult extends Thread {
        public void run() {
            try {
                System.out.println("[InsertAnswerUI] 결과 대기 시작");
                while (true) {
                    String msg = dis.readUTF();
                    System.out.println("[InsertAnswerUI] 수신한 메시지: " + msg);

                    if (msg.startsWith("/finalResult ")) {
                        // "/finalResult 승리팀|라이어이름|메시지"
                        String[] parts = msg.substring(13).split("\\|", 3);
                        boolean citizenWin = parts[0].equals("CITIZEN");
                        String liarName = parts[1];
                        String message = parts[2];

                        System.out.println("[InsertAnswerUI] 최종 결과 - 시민승리: " + citizenWin);

                        SwingUtilities.invokeLater(() -> {
                            new ResultUI(userName, citizenWin, message, dos, roomId, serverIp, serverPort);
                            dispose();
                        });
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("결과 수신 오류: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(InsertAnswerUI.this,
                            "서버와의 연결이 끊어졌습니다.",
                            "연결 오류",
                            JOptionPane.ERROR_MESSAGE);
                    dispose();
                });
            }
        }
    }
}