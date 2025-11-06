package org.example;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class LiarGameClientLoginUI extends JFrame {

    private BackgroundPanel contentPane; // [수정] JPanel -> BackgroundPanel
    private JTextField txtIpAddress;
    private JTextField txtNickname;
    private JTextField txtPort;
    private JButton btnEnterGame;
    private JButton btnConnectAsAdmin;

    // 플레이스홀더 텍스트를 위한 내부 클래스
    class PlaceholderTextField extends JTextField implements FocusListener {
        private String placeholder;
        private boolean isEmpty;

        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            this.isEmpty = true;
            setText(placeholder);
            setForeground(Color.LIGHT_GRAY);
            addFocusListener(this);
            setColumns(15);
            setPreferredSize(new Dimension(getPreferredSize().width, 36));
            setFont(new Font("맑은 고딕", Font.PLAIN, 14));

            setOpaque(false); // [수정] 텍스트 필드를 투명하게
            setCaretColor(Color.WHITE); // [추가] 커서 색상을 흰색으로 (배경이 어두울 경우)
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (isEmpty) {
                setText("");
                setForeground(Color.BLACK); // 실제 입력 텍스트 색상 (흰색으로 바꿔도 됩니다)
                isEmpty = false;
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (getText().trim().isEmpty()) {
                setText(placeholder);
                setForeground(Color.LIGHT_GRAY);
                isEmpty = true;
            }
        }

        @Override
        public String getText() {
            if (isEmpty) {
                return "";
            }
            return super.getText();
        }
    }


    /**
     * Launch the application.
     */
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

    /**
     * Create the frame.
     */
    public LiarGameClientLoginUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("라이어 게임 클라이언트");
        setBounds(100, 100, 400, 500);

        // [수정] JPanel 대신 BackgroundPanel을 contentPane으로 사용
        contentPane = new BackgroundPanel("UserStart.jpg");
        // contentPane.setBackground(Color.WHITE); // [제거] 배경 패널이 이미지를 그리므로 필요 없음
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 20));

        // 상단 제목 패널
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false); // [수정] 배경이 보이도록 투명하게
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("라이어 게임 입장");
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        lblTitle.setForeground(Color.WHITE); // [수정] 글자색 흰색으로
        titlePanel.add(lblTitle);
        contentPane.add(titlePanel, BorderLayout.NORTH);

        // 입력 필드 패널 (중앙)
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false); // [수정] 배경이 보이도록 투명하게
        inputPanel.setLayout(new GridLayout(6, 1, 0, 15));
        inputPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // 서버 IP
        JLabel lblIp = new JLabel("서버 IP");
        lblIp.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblIp.setForeground(Color.WHITE); // [수정] 글자색 흰색으로
        inputPanel.add(lblIp);
        txtIpAddress = new PlaceholderTextField("서버 IP를 입력하세요");
        txtIpAddress.setText("127.0.0.1");
        txtIpAddress.setForeground(Color.BLACK); // 기본값 텍스트 색상 (흰색으로 바꿔도 됩니다)
        ((PlaceholderTextField)txtIpAddress).isEmpty = false;
        inputPanel.add(txtIpAddress);

        // 닉네임
        JLabel lblNickname = new JLabel("닉네임");
        lblNickname.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblNickname.setForeground(Color.WHITE); // [수정] 글자색 흰색으로
        inputPanel.add(lblNickname);
        txtNickname = new PlaceholderTextField("닉네임을 입력하세요");
        inputPanel.add(txtNickname);

        // 포트
        JLabel lblPort = new JLabel("포트");
        lblPort.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblPort.setForeground(Color.WHITE); // [수정] 글자색 흰색으로
        inputPanel.add(lblPort);
        txtPort = new PlaceholderTextField("포트를 입력하세요");
        txtPort.setText("30000");
        txtPort.setForeground(Color.BLACK); // 기본값 텍스트 색상 (흰색으로 바꿔도 됩니다)
        ((PlaceholderTextField)txtPort).isEmpty = false;
        inputPanel.add(txtPort);

        contentPane.add(inputPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false); // [수정] 배경이 보이도록 투명하게
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 입장하기 버튼
        btnEnterGame = new JButton("입장하기");
        btnEnterGame.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnEnterGame.setBackground(new Color(53, 64, 82));
        btnEnterGame.setForeground(Color.RED); // [유지] 사용자님이 수정한 빨간색
        btnEnterGame.setFocusPainted(false);
        btnEnterGame.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btnEnterGame.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnEnterGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(btnEnterGame);
        buttonPanel.add(Box.createVerticalStrut(15));

        // 서버 관리자로 접속하기 버튼
        btnConnectAsAdmin = new JButton("서버 관리자로 접속하기");
        btnConnectAsAdmin.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        btnConnectAsAdmin.setForeground(Color.WHITE); // [수정] 글자색 흰색으로
        // btnConnectAsAdmin.setBackground(Color.WHITE); // [제거]
        btnConnectAsAdmin.setFocusPainted(false);
        btnConnectAsAdmin.setBorderPainted(false);
        btnConnectAsAdmin.setContentAreaFilled(false);
        btnConnectAsAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(btnConnectAsAdmin);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // 이벤트 리스너 연결
        Myaction action = new Myaction();
        btnEnterGame.addActionListener(action);
        btnConnectAsAdmin.addActionListener(action);

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
                    JavaChatClientView view = new JavaChatClientView(nickname, ip_addr, port_no);
                    view.setVisible(true);

                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "클라이언트 연결 오류: " + ex.getMessage(), "연결 오류", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else if (e.getSource() == btnConnectAsAdmin) {
                JOptionPane.showMessageDialog(null, "관리자 접속 기능은 아직 구현되지 않았습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}