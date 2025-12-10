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
    //사용자 및 서버 정보
    private String userName;
    private String roomId;
    private String serverIp;
    private String serverPort;

    //네트워크 관련 필드
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    //UI 컴포넌트
    private JTextField txtAnswer;
    private JButton btnSubmit;
    private boolean hasSubmitted = false; //정답 제출 여부 (중복 제출 방지)

    //생성자: 라이어가 정답을 입력할 때 호출
    public InsertAnswerUI(String userName, String roomId, String serverIp, String serverPort, Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.userName = userName;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        initializeUI();
        new ListenResult().start(); //최종 결과를 수신하는 스레드 시작
    }

    //정답 입력 UI를 초기화하는 메소드
    private void initializeUI() {
        setTitle("DrawLier - 마지막 기회!");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 550, 400);

        //배경 이미지를 그리는 패널 설정
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

        //중앙 입력 패널 (투명)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(180, 80, 20, 80)); //배경 이미지에 맞게 여백 설정

        //정답 입력 필드
        txtAnswer = new JTextField();
        txtAnswer.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        txtAnswer.setHorizontalAlignment(JTextField.CENTER);
        txtAnswer.setBackground(Color.WHITE);
        txtAnswer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 53, 69), 3, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        //입력 필드에 포커스가 갔을 때와 잃었을 때 테두리 색상 변경 효과
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
        txtAnswer.addActionListener(e -> submitAnswer()); //Enter 키로 제출 가능
        centerPanel.add(txtAnswer, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        //하단 버튼 패널
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 40, 0));

        //'정답 제출' 버튼 생성
        btnSubmit = new JButton();
        btnSubmit.setPreferredSize(new Dimension(100, 30));
        try {
            URL btnUrl = getClass().getResource("/PlayUI/Submit.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(100, 30, Image.SCALE_SMOOTH);
                btnSubmit.setIcon(new ImageIcon(img));
                UIUtils.applyButtonEffects(btnSubmit); //공용 버튼 효과 적용
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
        SwingUtilities.invokeLater(() -> txtAnswer.requestFocus()); //창이 열리면 바로 입력 필드에 포커스
    }

    //정답을 서버로 제출하는 메소드
    private void submitAnswer() {
        if (hasSubmitted) return; //이미 제출했으면 중복 방지

        String answer = txtAnswer.getText().trim();
        if (answer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "정답을 입력해주세요!", "알림", JOptionPane.WARNING_MESSAGE);
            txtAnswer.requestFocus();
            return;
        }

        try {
            dos.writeUTF("/liarAnswer " + answer); //라이어 정답 프로토콜 전송
            hasSubmitted = true;

            //제출 후 버튼과 입력창 비활성화
            btnSubmit.setEnabled(false);
            txtAnswer.setEnabled(false);

            JOptionPane.showMessageDialog(this, "정답을 제출했습니다.\n결과를 확인하는 중...", "제출 완료", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            hasSubmitted = false; //전송 실패 시 다시 제출 가능하도록
            JOptionPane.showMessageDialog(this, "정답 전송 중 오류가 발생했습니다.\n다시 시도해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            btnSubmit.setEnabled(true);
            txtAnswer.setEnabled(true);
        }
    }

    //서버로부터 최종 게임 결과를 수신하는 리스너 스레드
    class ListenResult extends Thread {
        public void run() {
            try {
                System.out.println("[InsertAnswerUI] 결과 대기 시작");
                while (true) {
                    String msg = dis.readUTF();
                    System.out.println("[InsertAnswerUI] 수신한 메시지: " + msg);
                    //최종 결과 프로토콜 수신
                    if (msg.startsWith("/finalResult ")) {
                        String[] parts = msg.substring(13).split("\\|", 3);
                        boolean citizenWin = parts[0].equals("CITIZEN"); //시민 승리 여부
                        String message = parts[2]; //결과 메시지
                        System.out.println("[InsertAnswerUI] 최종 결과 - 시민승리: " + citizenWin);
                        SwingUtilities.invokeLater(() -> {
                            //기존 연결을 유지한 채 결과 UI로 전환
                            new ResultUI(userName, citizenWin, message, roomId, serverIp, serverPort, socket, dis, dos);
                            dispose(); //현재 정답 입력 창 닫기
                        });
                        break; //스레드 종료
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
