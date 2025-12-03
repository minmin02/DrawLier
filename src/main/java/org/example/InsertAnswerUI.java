package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class InsertAnswerUI extends JFrame {
    private String userName;
    private String roomId;
    private String serverIp;
    private String serverPort;

    // ★★★ [추가] 소켓 및 스트림 필드
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private JTextField txtAnswer;
    private JButton btnSubmit;
    private boolean hasSubmitted = false;

    // ★★★ [수정] 생성자에서 소켓과 스트림을 받도록 변경
    public InsertAnswerUI(String userName, String roomId, String serverIp, String serverPort, Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.userName = userName;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        initializeUI();
        new ListenResult().start();
    }

    private void initializeUI() {
        setTitle("DrawLier - 마지막 기회!");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 550, 400);

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

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(180, 80, 20, 80));

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

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 40, 0));

        btnSubmit = new JButton();
        btnSubmit.setPreferredSize(new Dimension(100, 30));
        try {
            URL btnUrl = getClass().getResource("/PlayUI/Submit.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(100, 30, Image.SCALE_SMOOTH);
                btnSubmit.setIcon(new ImageIcon(img));
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
        if (hasSubmitted) return;
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
                        String message = parts[2];
                        System.out.println("[InsertAnswerUI] 최종 결과 - 시민승리: " + citizenWin);
                        SwingUtilities.invokeLater(() -> {
                            // ★★★ [수정] ResultUI 생성자에 소켓과 스트림 전달
                            new ResultUI(userName, citizenWin, message, roomId, serverIp, serverPort, socket, dis, dos);
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
