package org.example;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * 둥근 모서리를 가진 커스텀 Border 클래스
 * JTextField 디자인 개선을 위해 추가됨
 */
class RoundBorder extends AbstractBorder {
    private Color color;
    private int thickness;
    private int radius;
    private Insets insets;

    public RoundBorder(Color color, int thickness, int radius) {
        this.color = color;
        this.thickness = thickness;
        this.radius = radius;
        this.insets = new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));

        // 둥근 사각형 그리기
        g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
                width - thickness, height - thickness,
                radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = thickness;
        return insets;
    }
}


/**
 * 라이어 게임 클라이언트 로그인 UI 클래스
 * 로그인 후 방 목록 화면으로 이동
 */
public class LiarGameClientLoginUI extends JFrame {

    // (BackgroundPanel 클래스는 이 파일에 없으므로, 이미 정의되어 있다고 가정합니다.)
    // class BackgroundPanel extends JPanel { ... }
    private BackgroundPanel contentPane;
    private JTextField txtIpAddress;
    private JTextField txtNickname;
    private JTextField txtPort;
    private JButton btnEnterGame;

    // 플레이스홀더 텍스트 필드 (기존 코드 유지)
    class PlaceholderTextField extends JTextField implements FocusListener {
        private String placeholder;
        private boolean isEmpty;

        // 리팩토링: 둥근 모서리 반경 설정
        private final int CORNER_RADIUS = 10;

        // 리팩토링: 기본 테두리와 포커스 테두리 인셋 설정
        private final Border DEFAULT_PADDING = BorderFactory.createEmptyBorder(8, 15, 8, 15);
        private final Border FOCUSED_PADDING = BorderFactory.createEmptyBorder(7, 14, 7, 14);


        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            this.isEmpty = true;
            setText(placeholder);
            setForeground(new Color(150, 150, 150));
            addFocusListener(this);

            // 가로 크기 줄이기
            setColumns(20);
            setMaximumSize(new Dimension(400, 40));
            setPreferredSize(new Dimension(350, 40)); // 텍스트 필드 크기 유지

            setFont(new Font("맑은 고딕", Font.PLAIN, 14));

            // 리팩토링: 배경색을 조금 더 투명도를 줄여서 깔끔하게 설정
            setBackground(new Color(255, 255, 255, 220));
            setOpaque(true);
            setCaretColor(Color.BLACK);

            // --- [리팩토링 1] 둥근 모서리 및 깔끔한 테두리 적용 ---
            Border outerBorder = new RoundBorder(new Color(220, 220, 220), 1, CORNER_RADIUS);
            setBorder(BorderFactory.createCompoundBorder(outerBorder, DEFAULT_PADDING));
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (isEmpty) {
                setText("");
                setForeground(Color.BLACK);
                isEmpty = false;
            }

            // --- [리팩토링 2] 포커스 시 강조 효과 ---
            // 포커스 시 테두리: 눈에 띄는 파란색 (66, 133, 244), 2px 두께
            Border focusedBorder = new RoundBorder(new Color(66, 133, 244), 2, CORNER_RADIUS);
            setBorder(BorderFactory.createCompoundBorder(focusedBorder, FOCUSED_PADDING));
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (getText().trim().isEmpty()) {
                setText(placeholder);
                setForeground(new Color(150, 150, 150));
                isEmpty = true;
            }

            // --- [리팩토링 3] 포커스 상실 시 기본 스타일 복구 ---
            Border outerBorder = new RoundBorder(new Color(220, 220, 220), 1, CORNER_RADIUS);
            setBorder(BorderFactory.createCompoundBorder(outerBorder, DEFAULT_PADDING));
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
     * 이미지를 로드하여 JLabel에 설정하는 헬퍼 메서드
     * 이미지를 텍스트 필드와 유사한 높이(45px)로 스케일 다운하고 비율을 유지합니다.
     */
    private JLabel createImageLabel(String imagePath, String fallbackText) {
        JLabel label = new JLabel();

        // 목표 이미지 크기 (가로 134px, 높이 45px)
        final int TARGET_WIDTH = 134;
        final int TARGET_HEIGHT = 45;

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));

            // 이미지가 제대로 로드되었는지 확인
            if (icon.getIconWidth() == -1) {
                throw new Exception("이미지를 찾을 수 없습니다: " + imagePath);
            }

            // 이미지 크기를 목표 크기로 스케일 조정 (비율 유지)
            Image img = icon.getImage().getScaledInstance(TARGET_WIDTH, TARGET_HEIGHT, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(img));

            // 레이블의 크기를 이미지 크기로 고정하여 늘어짐 방지
            Dimension fixedSize = new Dimension(TARGET_WIDTH, TARGET_HEIGHT);
            label.setPreferredSize(fixedSize);
            label.setMaximumSize(fixedSize);
            label.setMinimumSize(fixedSize);

        } catch (Exception e) {
            // 이미지 로드 실패 시 텍스트로 대체
            label.setText(fallbackText);
            label.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            label.setForeground(Color.BLACK);
            System.err.println("경고: " + e.getMessage());
        }

        // 레이블을 중앙 정렬하도록 설정
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
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

        setBounds(100, 100, 1100, 900);

        contentPane = new BackgroundPanel("loginBackground.png");
        contentPane.setBorder(new EmptyBorder(30, 80, 30, 20));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 20));


        // 입력 필드 패널
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        // GridBagLayout을 사용하여 크기 제어 및 중앙 정렬
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setBorder(new EmptyBorder(200, 100, 50, 100)); // 상단 여백 200px
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; // 가로로 늘어나지 않게 설정
        gbc.insets = new Insets(0, 0, 10, 0); // 컴포넌트 간의 아래쪽 여백 (10px로 축소)
        // 텍스트 필드와 이미지가 모두 중앙에 위치하도록 weightx를 0으로 설정하고 anchor를 CENTER로 지정
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.CENTER;


        // --- Server IP ---
        JLabel lblIp = createImageLabel("/loginUI/ServerIp.png", "Server IP");
        gbc.gridx = 0; // 0열
        gbc.gridy = 0; // 0행
        inputPanel.add(lblIp, gbc);

        txtIpAddress = new PlaceholderTextField("Enter server IP address");
        txtIpAddress.setText("127.0.0.1");
        txtIpAddress.setForeground(Color.BLACK);
        ((PlaceholderTextField)txtIpAddress).isEmpty = false;
        gbc.gridy = 1; // 1행
        inputPanel.add(txtIpAddress, gbc);

        // --- Nickname ---
        JLabel lblNickname = createImageLabel("/loginUI/NickName.png", "Nickname");
        gbc.gridy = 2; // 2행
        inputPanel.add(lblNickname, gbc);

        txtNickname = new PlaceholderTextField("Enter your nickname");
        gbc.gridy = 3; // 3행
        inputPanel.add(txtNickname, gbc);

        // --- Port ---
        JLabel lblPort = createImageLabel("/loginUI/Port.png", "Port");
        gbc.gridy = 4; // 4행
        inputPanel.add(lblPort, gbc);

        txtPort = new PlaceholderTextField("Enter port number");
        txtPort.setText("30000");
        txtPort.setForeground(Color.BLACK);
        ((PlaceholderTextField)txtPort).isEmpty = false;
        gbc.gridy = 5; // 5행
        inputPanel.add(txtPort, gbc);

        contentPane.add(inputPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

        // 버튼 이미지 로딩 (기존 코드 유지)
        btnEnterGame = new JButton(); // 텍스트 제거

        try {
            // 이미지 로드 및 크기 조절
            ImageIcon icon = new ImageIcon(getClass().getResource("/loginUI/StartButton.png"));

            // 이미지가 제대로 로드되었는지 확인
            if (icon.getIconWidth() == -1) {
                throw new Exception("이미지를 찾을 수 없습니다.");
            }

            // 버튼 크기는 220x60 유지
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