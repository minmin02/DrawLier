package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * 라이어 게임 클라이언트 로그인 UI 클래스
 * 로그인 후 방 목록 화면으로 이동
 */
public class LiarGameClientLoginUI extends JFrame {

    private BackgroundPanel contentPane;
    private JTextField txtIpAddress;
    private JTextField txtNickname;
    private JTextField txtPort;
    private JButton btnEnterGame;

    // 플레이스홀더 텍스트 필드 (기존 코드 유지)
    class PlaceholderTextField extends JTextField implements FocusListener {
        private String placeholder;
        private boolean isEmpty;

        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            this.isEmpty = true;
            setText(placeholder);
            setForeground(new Color(150, 150, 150));
            addFocusListener(this);

            // 가로 크기 줄이기
            setColumns(20); // 15에서 더 작게 조절 (원하는 크기로)
            setMaximumSize(new Dimension(400, 40)); // 최대 너비 제한
            setPreferredSize(new Dimension(350, 40)); // 선호 크기 설정

            setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            setBackground(new Color(255, 255, 255, 200));
            setOpaque(true);
            setCaretColor(Color.BLACK);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
            ));
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (isEmpty) {
                setText("");
                setForeground(Color.BLACK);
                isEmpty = false;
            }
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(66, 133, 244), 2, true),
                    BorderFactory.createEmptyBorder(7, 14, 7, 14)
            ));
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (getText().trim().isEmpty()) {
                setText(placeholder);
                setForeground(new Color(150, 150, 150));
                isEmpty = true;
            }
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
            ));
        }

        @Override
        public String getText() {
            if (isEmpty) {
                return "";
            }
            return super.getText();
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                LiarGameClientLoginUI frame = new LiarGameClientLoginUI();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LiarGameClientLoginUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("DrawLier - Liar Game Client");

        // [수정 1] 가로 너비를 400 -> 600으로 변경
        setBounds(100, 100, 1100, 900);

        contentPane = new BackgroundPanel("UserStart.jpg");
        contentPane.setBorder(new EmptyBorder(30, 80, 30, 20)); // 너비가 넓어졌으므로 좌우 여백을 조금 더 줌
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 20));



        // 입력 필드 패널
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        inputPanel.setLayout(new GridLayout(6, 1, 0, 8)); // 세로 간격을 12 -> 8로 줄임
        inputPanel.setBorder(new EmptyBorder(300, 150, 50, 150)); // 상단 여백을 늘려서 아래로 내림 (20->300)

        JLabel lblIp = new JLabel("Server IP");
        lblIp.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblIp.setForeground(Color.BLACK);
        inputPanel.add(lblIp);
        txtIpAddress = new PlaceholderTextField("Enter server IP address");
        txtIpAddress.setText("127.0.0.1");
        txtIpAddress.setForeground(Color.BLACK);
        ((PlaceholderTextField)txtIpAddress).isEmpty = false;
        inputPanel.add(txtIpAddress);

        JLabel lblNickname = new JLabel("Nickname");
        lblNickname.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblNickname.setForeground(Color.BLACK);
        inputPanel.add(lblNickname);
        txtNickname = new PlaceholderTextField("Enter your nickname");
        inputPanel.add(txtNickname);

        JLabel lblPort = new JLabel("Port");
        lblPort.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblPort.setForeground(Color.BLACK);
        inputPanel.add(lblPort);
        txtPort = new PlaceholderTextField("Enter port number");
        txtPort.setText("30000");
        txtPort.setForeground(Color.BLACK);
        ((PlaceholderTextField)txtPort).isEmpty = false;
        inputPanel.add(txtPort);

        contentPane.add(inputPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

        // [수정 2] 버튼을 이미지로 교체
        btnEnterGame = new JButton(); // 텍스트 제거

        try {
            // 이미지 로드 및 크기 조절
            ImageIcon icon = new ImageIcon(getClass().getResource("/EnterButton.png"));

            // 이미지가 제대로 로드되었는지 확인
            if (icon.getIconWidth() == -1) {
                throw new Exception("이미지를 찾을 수 없습니다.");
            }

            Image img = icon.getImage().getScaledInstance(220, 60, Image.SCALE_SMOOTH);
            btnEnterGame.setIcon(new ImageIcon(img));

            btnEnterGame.setBorderPainted(false);
            btnEnterGame.setContentAreaFilled(false);
            btnEnterGame.setFocusPainted(false);
            btnEnterGame.setOpaque(false);

        } catch (Exception e) {
            // 이미지가 없을 경우 텍스트 버튼으로 대체
            btnEnterGame.setText("시작하기 ≫");
            btnEnterGame.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            btnEnterGame.setBackground(Color.BLACK);
            btnEnterGame.setForeground(Color.WHITE);
            btnEnterGame.setPreferredSize(new Dimension(220, 60));
            System.err.println("EnterButton.png 이미지를 찾을 수 없습니다: " + e.getMessage());
        }
        btnEnterGame.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 이미지 버튼이므로 기존의 배경색 변경 마우스 리스너는 제거하거나,
        // 필요하다면 이미지를 바꾸는 로직(롤오버 이미지 등)으로 변경해야 함.
        // 여기서는 단순화를 위해 기존 색상 변경 리스너 제거.

        buttonPanel.add(btnEnterGame);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // 이벤트 리스너
        Myaction action = new Myaction();
        btnEnterGame.addActionListener(action);

        setLocationRelativeTo(null);
    }

    class Myaction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnEnterGame) {
                String ip_addr = txtIpAddress.getText().trim();
                String nickname = txtNickname.getText().trim();
                String port_no = txtPort.getText().trim();

                if (nickname.isEmpty() || ip_addr.isEmpty() || port_no.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "모든 정보를 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    // 방 목록 화면으로 이동
                    RoomListUI roomListUI = new RoomListUI(nickname, ip_addr, port_no);
                    roomListUI.setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "연결 오류: " + ex.getMessage(), "연결 오류", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }
}