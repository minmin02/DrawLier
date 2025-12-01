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
    // 버튼에 호버(밝게) 및 클릭(어둡게) 효과를 자동으로 적용하는 메서드
    private void applyButtonEffects(JButton button) {
        if (button.getIcon() == null) return;

        ImageIcon originalIcon = (ImageIcon) button.getIcon();
        Image originalImage = originalIcon.getImage();

        //BufferedImage로 변환
        int w = originalImage.getWidth(null);
        int h = originalImage.getHeight(null);
        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(originalImage, 0, 0, null);
        g2.dispose();

        //호버 효과 (밝게: 1.2배)
        java.awt.image.RescaleOp hoverFilter = new java.awt.image.RescaleOp(1.2f, 0, null);
        java.awt.image.BufferedImage hoverImage = hoverFilter.filter(bufferedImage, null);
        button.setRolloverIcon(new ImageIcon(hoverImage));

        //클릭 효과 (어둡게: 0.8배)
        java.awt.image.RescaleOp pressFilter = new java.awt.image.RescaleOp(0.8f, 0, null);
        java.awt.image.BufferedImage pressImage = pressFilter.filter(bufferedImage, null);
        button.setPressedIcon(new ImageIcon(pressImage));

        //기본 설정 강제 (투명화 등)
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
    }

    private void initializeUI() {
        setTitle("DrawLier - 마지막 기회!");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 550, 400);

        // ★★★ [수정] 배경 이미지를 그리는 패널 설정 ★★★
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
                    // 이미지 없을 시 기본 배경
                    g.setColor(new Color(255, 230, 230));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        // 여백 설정
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        // 중앙 입력 패널 (투명)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setOpaque(false);

        // 입력창 위치 조정을 위한 여백 (배경 이미지 디자인에 맞춰 숫자를 조절하세요)
        // 위쪽 여백을 많이 주어 입력창을 화면 중앙/하단으로 내림
        centerPanel.setBorder(new EmptyBorder(180, 80, 20, 80));

        // 입력 필드 설정
        txtAnswer = new JTextField();
        txtAnswer.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        txtAnswer.setHorizontalAlignment(JTextField.CENTER);
        txtAnswer.setBackground(Color.WHITE);

        // 입력 필드 테두리 스타일
        txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 53, 69), 3, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // 입력 필드 포커스 효과
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

        // 중앙 패널에 입력창 추가
        centerPanel.add(txtAnswer, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼 패널 (투명)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 40, 0)); // 하단 여백

        //이미지 버튼 생성
        btnSubmit = new JButton();
        int btnWidth = 220;
        int btnHeight = 55;
        btnSubmit.setPreferredSize(new Dimension(btnWidth, btnHeight));

        try {
            URL btnUrl = getClass().getResource("/PlayUI/Submit.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(btnWidth, btnHeight, Image.SCALE_SMOOTH);
                btnSubmit.setIcon(new ImageIcon(img));
                applyButtonEffects(btnSubmit); //버튼 효과 적용
            } else {
                btnSubmit.setText("정답 제출");
                System.err.println("이미지를 찾을 수 없습니다: /PlayUI/Submit.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnSubmit.setText("정답 제출");
        }

        // 버튼 스타일 투명화
        btnSubmit.setBorderPainted(false);
        btnSubmit.setContentAreaFilled(false);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setOpaque(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));

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
            // 이미지가 아닌 텍스트 변경은 제거하거나, 이미지를 흑백 처리하는 로직 등이 필요할 수 있음
            // 여기서는 단순 비활성화 처리
            txtAnswer.setEnabled(false);

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