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

    // 플레이스홀더 텍스트 필드
    class PlaceholderTextField extends JTextField implements FocusListener {
        private String placeholder;
        private boolean isEmpty;

        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            this.isEmpty = true;
            setText(placeholder);
            setForeground(new Color(150, 150, 150));
            addFocusListener(this);
            setColumns(15);
            setPreferredSize(new Dimension(getPreferredSize().width, 42));
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
        setBounds(100, 100, 400, 550);

        contentPane = new BackgroundPanel("UserStart.jpg");
        contentPane.setBorder(new EmptyBorder(30, 30, 30, 30));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 20));

        // 상단 제목 패널
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel lblMainTitle = new JLabel();
        try {
            ImageIcon titleIcon = new ImageIcon(getClass().getResource("/DrawLierLogo.png"));
            Image titleImage = titleIcon.getImage().getScaledInstance(250, 80, Image.SCALE_SMOOTH);
            lblMainTitle.setIcon(new ImageIcon(titleImage));
        } catch (Exception e) {
            lblMainTitle.setText("DrawLier");
            lblMainTitle.setFont(new Font("맑은 고딕", Font.BOLD, 38));
            lblMainTitle.setForeground(Color.WHITE);
        }
        lblMainTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(lblMainTitle);

        titlePanel.add(Box.createVerticalStrut(5));

        JLabel lblSubTitle = new JLabel("Enter the Game");
        lblSubTitle.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        lblSubTitle.setForeground(new Color(230, 230, 230));
        lblSubTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(lblSubTitle);

        contentPane.add(titlePanel, BorderLayout.NORTH);

        // 입력 필드 패널
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        inputPanel.setLayout(new GridLayout(6, 1, 0, 12));
        inputPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel lblIp = new JLabel("Server IP");
        lblIp.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblIp.setForeground(Color.WHITE);
        inputPanel.add(lblIp);
        txtIpAddress = new PlaceholderTextField("Enter server IP address");
        txtIpAddress.setText("127.0.0.1");
        txtIpAddress.setForeground(Color.BLACK);
        ((PlaceholderTextField)txtIpAddress).isEmpty = false;
        inputPanel.add(txtIpAddress);

        JLabel lblNickname = new JLabel("Nickname");
        lblNickname.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblNickname.setForeground(Color.WHITE);
        inputPanel.add(lblNickname);
        txtNickname = new PlaceholderTextField("Enter your nickname");
        inputPanel.add(txtNickname);

        JLabel lblPort = new JLabel("Port");
        lblPort.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        lblPort.setForeground(Color.WHITE);
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

        btnEnterGame = new JButton("입장하기");
        btnEnterGame.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnEnterGame.setBackground(new Color(220, 53, 69));
        btnEnterGame.setForeground(Color.WHITE);
        btnEnterGame.setFocusPainted(false);
        btnEnterGame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 43, 59), 2, true),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)
        ));
        btnEnterGame.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnEnterGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEnterGame.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnEnterGame.setBackground(new Color(200, 43, 59));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnEnterGame.setBackground(new Color(220, 53, 69));
            }
        });
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