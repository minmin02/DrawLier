package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

// 라이어 게임 클라이언트 로그인 UI 클래스
public class LiarGameClientLoginUI extends JFrame {

    // 배경 이미지가 있는 커스텀 패널
    private BackgroundPanel contentPane;
    // 서버 IP 입력 필드
    private JTextField txtIpAddress;
    // 사용자 닉네임 입력 필드
    private JTextField txtNickname;
    // 서버 포트 번호 입력 필드
    private JTextField txtPort;
    // 게임 입장 버튼
    private JButton btnEnterGame;
    // 관리자로 접속하는 버튼
    private JButton btnConnectAsAdmin;

    // 플레이스홀더(힌트 텍스트) 기능이 있는 커스텀 텍스트 필드 내부 클래스
    class PlaceholderTextField extends JTextField implements FocusListener {
        // 플레이스홀더로 표시할 텍스트
        private String placeholder;
        // 현재 필드가 비어있는지 여부
        private boolean isEmpty;

        // 생성자: 플레이스홀더 텍스트를 받아서 초기화
        public PlaceholderTextField(String placeholder) {
            // 플레이스홀더 텍스트 저장
            this.placeholder = placeholder;
            // 초기에는 비어있음으로 설정
            this.isEmpty = true;
            // 플레이스홀더를 기본 텍스트로 설정
            setText(placeholder);
            // 플레이스홀더는 연한 회색으로 표시
            setForeground(new Color(150, 150, 150));
            // 포커스 이벤트 리스너 등록
            addFocusListener(this);
            // 컬럼 수 설정
            setColumns(15);
            // 텍스트 필드 크기 설정
            setPreferredSize(new Dimension(getPreferredSize().width, 42));
            // 폰트 설정 (맑은 고딕, 보통체, 14px)
            setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            // 텍스트 필드 배경색 설정 (반투명 흰색)
            setBackground(new Color(255, 255, 255, 200));
            // 텍스트 필드를 불투명하게 설정
            setOpaque(true);
            // 커서(캐럿) 색상을 검정색으로 설정
            setCaretColor(Color.BLACK);
            // 둥근 테두리 설정 (회색, 1px)
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
            ));
        }

        // 텍스트 필드에 포커스가 들어왔을 때 호출
        @Override
        public void focusGained(FocusEvent e) {
            // 필드가 비어있는 상태라면
            if (isEmpty) {
                // 플레이스홀더 텍스트 제거
                setText("");
                // 실제 입력 텍스트는 검정색으로 표시
                setForeground(Color.BLACK);
                // 더 이상 비어있지 않음으로 표시
                isEmpty = false;
            }
            // 포커스 시 테두리 색상 변경 (파란색)
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(66, 133, 244), 2, true),
                    BorderFactory.createEmptyBorder(7, 14, 7, 14)
            ));
        }

        // 텍스트 필드에서 포커스가 벗어났을 때 호출
        @Override
        public void focusLost(FocusEvent e) {
            // 사용자가 아무것도 입력하지 않았다면
            if (getText().trim().isEmpty()) {
                // 플레이스홀더 텍스트를 다시 표시
                setText(placeholder);
                // 연한 회색으로 변경
                setForeground(new Color(150, 150, 150));
                // 비어있음으로 표시
                isEmpty = true;
            }
            // 포커스 아웃 시 테두리 색상 원래대로
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
            ));
        }

        // getText() 메소드 오버라이드: 실제 입력값만 반환
        @Override
        public String getText() {
            // 필드가 비어있다면 빈 문자열 반환 (플레이스홀더를 반환하지 않음)
            if (isEmpty) {
                return "";
            }
            // 실제 입력된 텍스트 반환
            return super.getText();
        }
    }

    /**
     * 프로그램 실행 메인 메소드
     */
    public static void main(String[] args) {
        // Swing의 이벤트 디스패치 스레드에서 GUI 생성
        EventQueue.invokeLater(() -> {
            try {
                // LiarGameClientLoginUI 프레임 객체 생성
                LiarGameClientLoginUI frame = new LiarGameClientLoginUI();
                // 프레임을 화면에 표시
                frame.setVisible(true);
            } catch (Exception e) {
                // 예외 발생 시 스택 트레이스 출력
                e.printStackTrace();
            }
        });
    }

    /**
     * 프레임 생성자 - UI 컴포넌트를 초기화하고 배치
     */
    public LiarGameClientLoginUI() {
        // 창 닫기 버튼 클릭 시 프로그램 종료
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 프레임 타이틀 설정
        setTitle("DrawLier - Liar Game Client");
        // 프레임 위치(100, 100)와 크기(400x550) 설정
        setBounds(100, 100, 400, 550);

        // 배경 이미지(UserStart.jpg)가 있는 커스텀 패널 생성
        contentPane = new BackgroundPanel("UserStart.jpg");
        // 패널 여백 설정 (상하좌우 30px)
        contentPane.setBorder(new EmptyBorder(30, 30, 30, 30));
        // 이 패널을 프레임의 컨텐트 패널로 설정
        setContentPane(contentPane);
        // BorderLayout 설정 (컴포넌트 간 간격 20px)
        contentPane.setLayout(new BorderLayout(0, 20));

        // ===== 상단 제목 패널 =====
        // 제목을 담을 패널 생성
        JPanel titlePanel = new JPanel();
        // 패널을 투명하게 설정 (배경 이미지가 보이도록)
        titlePanel.setOpaque(false);
        // BoxLayout으로 설정 (수직 배치)
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        // 메인 타이틀 이미지 라벨 생성
        JLabel lblMainTitle = new JLabel();
        try {
            // 이미지 파일 로드 (프로젝트 루트 또는 resources 폴더에서)
            ImageIcon titleIcon = new ImageIcon(getClass().getResource("/DrawLierLogo.png"));
            // 이미지 크기 조정 (원하는 크기로 설정 가능)
            Image titleImage = titleIcon.getImage().getScaledInstance(250, 80, Image.SCALE_SMOOTH);
            lblMainTitle.setIcon(new ImageIcon(titleImage));
        } catch (Exception e) {
            // 이미지 로드 실패 시 텍스트로 대체
            lblMainTitle.setText("DrawLier");
            lblMainTitle.setFont(new Font("맑은 고딕", Font.BOLD, 38));
            lblMainTitle.setForeground(Color.WHITE);
        }
        // 가운데 정렬
        lblMainTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        // 제목 라벨을 패널에 추가
        titlePanel.add(lblMainTitle);

        // 간격 추가 (5px)
        titlePanel.add(Box.createVerticalStrut(5));

        // 서브 타이틀 "Enter the Game" 라벨 생성
        JLabel lblSubTitle = new JLabel("Enter the Game");
        // 폰트 설정 (맑은 고딕, 보통체, 16px)
        lblSubTitle.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        // 글자색을 밝은 회색으로 설정
        lblSubTitle.setForeground(new Color(230, 230, 230));
        // 가운데 정렬
        lblSubTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        // 서브 타이틀 라벨을 패널에 추가
        titlePanel.add(lblSubTitle);

        // 제목 패널을 상단(NORTH)에 배치
        contentPane.add(titlePanel, BorderLayout.NORTH);

        // ===== 입력 필드 패널 (중앙) =====
        // 입력 필드들을 담을 패널 생성
        JPanel inputPanel = new JPanel();
        // 패널을 투명하게 설정
        inputPanel.setOpaque(false);
        // GridLayout 설정 (6행 1열, 세로 간격 12px)
        inputPanel.setLayout(new GridLayout(6, 1, 0, 12));
        // 패널 여백 설정 (상하 20px, 좌우 10px)
        inputPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

        // ----- 서버 IP 입력 -----
        // "Server IP" 라벨 생성
        JLabel lblIp = new JLabel("Server IP");
        // 폰트 설정 (맑은 고딕, 굵게, 13px)
        lblIp.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        // 글자색을 흰색으로 설정
        lblIp.setForeground(Color.WHITE);
        // 라벨을 패널에 추가
        inputPanel.add(lblIp);
        // 플레이스홀더가 있는 텍스트 필드 생성
        txtIpAddress = new PlaceholderTextField("Enter server IP address");
        // 기본값으로 로컬호스트(127.0.0.1) 설정
        txtIpAddress.setText("127.0.0.1");
        // 기본값 텍스트는 검정색으로 표시
        txtIpAddress.setForeground(Color.BLACK);
        // isEmpty를 false로 설정 (기본값이 있으므로)
        ((PlaceholderTextField)txtIpAddress).isEmpty = false;
        // 텍스트 필드를 패널에 추가
        inputPanel.add(txtIpAddress);

        // ----- 닉네임 입력 -----
        // "Nickname" 라벨 생성
        JLabel lblNickname = new JLabel("Nickname");
        // 폰트 설정
        lblNickname.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        // 글자색을 흰색으로 설정
        lblNickname.setForeground(Color.WHITE);
        // 라벨을 패널에 추가
        inputPanel.add(lblNickname);
        // 플레이스홀더가 있는 텍스트 필드 생성
        txtNickname = new PlaceholderTextField("Enter your nickname");
        // 텍스트 필드를 패널에 추가
        inputPanel.add(txtNickname);

        // ----- 포트 입력 -----
        // "Port" 라벨 생성
        JLabel lblPort = new JLabel("Port");
        // 폰트 설정
        lblPort.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        // 글자색을 흰색으로 설정
        lblPort.setForeground(Color.WHITE);
        // 라벨을 패널에 추가
        inputPanel.add(lblPort);
        // 플레이스홀더가 있는 텍스트 필드 생성
        txtPort = new PlaceholderTextField("Enter port number");
        // 기본값으로 30000 설정
        txtPort.setText("30000");
        // 기본값 텍스트는 검정색으로 표시
        txtPort.setForeground(Color.BLACK);
        // isEmpty를 false로 설정
        ((PlaceholderTextField)txtPort).isEmpty = false;
        // 텍스트 필드를 패널에 추가
        inputPanel.add(txtPort);

        // 입력 패널을 중앙(CENTER)에 배치
        contentPane.add(inputPanel, BorderLayout.CENTER);

        // ===== 하단 버튼 패널 =====
        // 버튼들을 담을 패널 생성
        JPanel buttonPanel = new JPanel();
        // 패널을 투명하게 설정
        buttonPanel.setOpaque(false);
        // BoxLayout 설정 (수직 배치)
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        // 패널 여백 설정 (상단 10px, 좌우 10px)
        buttonPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

        // ----- 입장하기 버튼 -----
        // "입장하기" 버튼 생성
        btnEnterGame = new JButton("입장하기");
        // 폰트 설정 (맑은 고딕, 굵게, 16px)
        btnEnterGame.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        // 버튼 배경색 설정 (선명한 빨간색)
        btnEnterGame.setBackground(new Color(220, 53, 69));
        // 버튼 글자색을 흰색으로 설정
        btnEnterGame.setForeground(Color.WHITE);
        // 포커스 테두리 제거
        btnEnterGame.setFocusPainted(false);
        // 둥근 테두리 설정
        btnEnterGame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 43, 59), 2, true),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)
        ));
        // 버튼 최대 크기 설정 (가로 무제한, 세로 48px)
        btnEnterGame.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        // 버튼을 가운데 정렬
        btnEnterGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        // 마우스 오버 효과 추가
        btnEnterGame.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnEnterGame.setBackground(new Color(200, 43, 59));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnEnterGame.setBackground(new Color(220, 53, 69));
            }
        });
        // 버튼을 패널에 추가
        buttonPanel.add(btnEnterGame);
        // 버튼 사이에 15px 간격 추가
        buttonPanel.add(Box.createVerticalStrut(15));

        // ----- 서버 관리자로 접속하기 버튼 -----
        // "서버 관리자로 접속하기" 버튼 생성
        btnConnectAsAdmin = new JButton("Connect as Server Admin");
        // 폰트 설정 (맑은 고딕, 보통체, 12px)
        btnConnectAsAdmin.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        // 버튼 글자색을 밝은 흰색으로 설정
        btnConnectAsAdmin.setForeground(new Color(230, 230, 230));
        // 포커스 테두리 제거
        btnConnectAsAdmin.setFocusPainted(false);
        // 버튼 테두리 제거
        btnConnectAsAdmin.setBorderPainted(false);
        // 버튼 배경 제거 (투명하게)
        btnConnectAsAdmin.setContentAreaFilled(false);
        // 버튼을 가운데 정렬
        btnConnectAsAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
        // 마우스 오버 효과 추가
        btnConnectAsAdmin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnConnectAsAdmin.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnConnectAsAdmin.setForeground(new Color(230, 230, 230));
            }
        });
        // 버튼을 패널에 추가
        buttonPanel.add(btnConnectAsAdmin);

        // 버튼 패널을 하단(SOUTH)에 배치
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // ===== 이벤트 리스너 연결 =====
        // 액션 리스너 객체 생성
        Myaction action = new Myaction();
        // 입장하기 버튼에 리스너 등록
        btnEnterGame.addActionListener(action);
        // 관리자 접속 버튼에 리스너 등록
        btnConnectAsAdmin.addActionListener(action);

        // 프레임을 화면 중앙에 배치
        setLocationRelativeTo(null);
    }

    // 버튼 클릭 이벤트를 처리하는 내부 클래스
    class Myaction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 입장하기 버튼이 클릭되었을 때
            if (e.getSource() == btnEnterGame) {
                // IP 주소 입력값 가져오기 (앞뒤 공백 제거)
                String ip_addr = txtIpAddress.getText().trim();
                // 닉네임 입력값 가져오기
                String nickname = txtNickname.getText().trim();
                // 포트 번호 입력값 가져오기
                String port_no = txtPort.getText().trim();

                // 입력값 검증: 하나라도 비어있으면
                if (nickname.isEmpty() || ip_addr.isEmpty() || port_no.isEmpty()) {
                    // 경고 메시지 표시
                    JOptionPane.showMessageDialog(null, "모든 정보를 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    // 메소드 종료
                    return;
                }

                try {
                    // 게임 클라이언트 뷰 생성 (닉네임, IP, 포트 전달)
                    JavaChatClientView view = new JavaChatClientView(nickname, ip_addr, port_no);
                    // 게임 화면을 표시
                    view.setVisible(true);

                    // 로그인 화면 닫기
                    dispose();
                } catch (Exception ex) {
                    // 연결 실패 시 에러 메시지 표시
                    JOptionPane.showMessageDialog(null, "클라이언트 연결 오류: " + ex.getMessage(), "연결 오류", JOptionPane.ERROR_MESSAGE);
                    // 예외 스택 트레이스 출력
                    ex.printStackTrace();
                }
            }
            // 관리자 접속 버튼이 클릭되었을 때
            else if (e.getSource() == btnConnectAsAdmin) {
                // 아직 구현되지 않았다는 안내 메시지 표시
                JOptionPane.showMessageDialog(null, "관리자 접속 기능은 아직 구현되지 않았습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}