package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * 라이어 정답 입력 UI - 수정됨
 * 1. 배경 이미지 (/PlayUI/InsertAnswer.png) 적용
 * 2. 불필요한 텍스트/라벨 제거
 * 3. 제출 버튼 이미지 (/PlayUI/Submit.png) 적용
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

        // 배경 이미지를 그리는 패널 설정
        JPanel contentPane = new JPanel() {
            private BufferedImage bgImage;

            {
                try {
                    URL imageUrl = getClass().getResource("/PlayUI/InsertAnswer.png");
                    if (imageUrl != null) {
                        bgImage = ImageIO.read(imageUrl);
                    } else {
                        System.err.println("이미지 리소스를 찾을 수 없습니다: /PlayUI/InsertAnswer.png");
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
                    g.setColor(new Color(255, 230, 230));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        // 중앙 입력 패널 (투명)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(180, 80, 20, 80));

        // 입력 필드 설정
        txtAnswer = new JTextField();
        txtAnswer.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        txtAnswer.setHorizontalAlignment(JTextField.CENTER);
        txtAnswer.setBackground(Color.WHITE);
        txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 53, 69), 3, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        txtAnswer.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 100, 100), 3, true),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 53, 69), 3, true),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }
        });

        txtAnswer.addActionListener(e -> submitAnswer());
        centerPanel.add(txtAnswer, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼 패널 (투명)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 40, 0));

        //이미지 버튼 생성
        btnSubmit = new JButton();
        btnSubmit.setPreferredSize(new Dimension(100, 30));

        try {
            URL btnUrl = getClass().getResource("/PlayUI/Submit.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(100, 30, Image.SCALE_SMOOTH);
                btnSubmit.setIcon(new ImageIcon(img));
                // ★★★ [수정] UIUtils의 공용 메소드 호출 ★★★
                UIUtils.applyButtonEffects(btnSubmit);
            } else {
                btnSubmit.setText("정답 제출");
                System.err.println("이미지를 찾을 수 없습니다: /PlayUI/Submit.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnSubmit.setText("정답 제출");
        }

        btnSubmit.addActionListener(e -> submitAnswer());
        bottomPanel.add(btnSubmit);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);

        SwingUtilities.invokeLater(() -> txtAnswer.requestFocus());
    }

    private void submitAnswer() {
        if (hasSubmitted) {
            return;
        }

        String answer = txtAnswer.getText().trim();

        if (answer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "정답을 입력해주세요!", "알림", JOptionPane.WARNING_MESSAGE);
            txtAnswer.requestFocus();
            return;
        }

        try {
            dos.writeUTF("/liarAnswer " + answer);
            hasSubmitted = true;

            btnSubmit.setEnabled(false);
            txtAnswer.setEnabled(false);

            JOptionPane.showMessageDialog(this, "정답을 제출했습니다.\n결과를 확인하는 중...", "제출 완료", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            hasSubmitted = false;
            JOptionPane.showMessageDialog(this, "정답 전송 중 오류가 발생했습니다.\n다시 시도해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            btnSubmit.setEnabled(true);
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
                    JOptionPane.showMessageDialog(InsertAnswerUI.this, "서버와의 연결이 끊어졌습니다.", "연결 오류", JOptionPane.ERROR_MESSAGE);
                    dispose();
                });
            }
        }
    }
}
