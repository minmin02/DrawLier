package org.example;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// 라이어 게임 메인 화면 - 그림판과 채팅 기능이 있는 클라이언트 뷰
public class JavaChatClientView extends JFrame {
    // 메인 컨텐트 패널
    private JPanel contentPane;
    // 채팅 메시지 입력 필드
    private JTextField txtInput;
    // 사용자 이름(닉네임)
    private String UserName;
    // 채팅 전송 버튼
    private JButton btnSend;
    // 채팅 메시지를 표시하는 텍스트 영역
    private JTextArea textArea;
    // 버퍼 길이 상수
    private static final int BUF_LEN = 128;
    // 서버와 연결된 소켓
    private Socket socket;
    // 입력 스트림 (서버로부터 데이터 받기)
    private InputStream is;
    // 출력 스트림 (서버로 데이터 보내기)
    private OutputStream os;
    // 데이터 입력 스트림 (UTF 문자열 읽기용)
    private DataInputStream dis;
    // 데이터 출력 스트림 (UTF 문자열 쓰기용)
    private DataOutputStream dos;
    // 사용자 이름을 표시하는 라벨
    private JLabel lblUserName;

    // ===== 그림판 관련 변수 =====
    // 실제 그림을 그릴 수 있는 패널
    private DrawingPanel drawingPanel;
    // 전체 지우기 버튼
    private JButton btnClear;
    // 지우개 모드 토글 버튼
    private JButton btnEraser;
    // 색상 선택 콤보박스
    private JComboBox<String> colorComboBox;
    // 선 굵기 선택 콤보박스
    private JComboBox<Integer> thicknessComboBox;

    /**
     * 생성자 - UI 초기화 및 서버 연결
     * @param username 사용자 닉네임
     * @param ip_addr 서버 IP 주소
     * @param port_no 서버 포트 번호
     */
    public JavaChatClientView(String username, String ip_addr, String port_no) {
        // 프레임 타이틀에 사용자 이름 표시
        setTitle("DrawLier - " + username);
        // 창 닫기 버튼 클릭 시 프로그램 종료
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 프레임 위치(100, 100)와 크기(1000x600) 설정
        setBounds(100, 100, 1000, 600);
        // 메인 컨텐트 패널 생성
        contentPane = new JPanel();
        // 패널 여백 설정 (5px)
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        // 패널 배경색을 흰색으로 설정
        contentPane.setBackground(Color.WHITE);
        // 이 패널을 프레임의 컨텐트 패널로 설정
        setContentPane(contentPane);
        // BorderLayout 설정 (컴포넌트 간 간격 10px)
        contentPane.setLayout(new BorderLayout(10, 10));

        // ===== 메인 컨테이너 - 좌우 분할 =====
        // 1행 2열 그리드 레이아웃 (좌우 간격 10px)
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        // 배경색을 흰색으로 설정
        mainPanel.setBackground(Color.WHITE);

        // ===== 왼쪽: 그림판 영역 =====
        // BorderLayout으로 그림판 영역 패널 생성 (간격 5px)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        // 배경색을 흰색으로 설정
        leftPanel.setBackground(Color.WHITE);
        // 테두리와 타이틀 설정 (어두운 청회색 테두리, "그림 그리기" 타이틀)
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(53, 64, 82), 2),
                "그림 그리기",
                // 새로운 배치관리자
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 14),
                new Color(53, 64, 82)
        ));

        // ----- 그림판 도구 패널 -----
        // FlowLayout으로 도구 패널 생성 (왼쪽 정렬, 간격 10px, 5px)
        JPanel drawToolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        // 배경색을 흰색으로 설정
        drawToolPanel.setBackground(Color.WHITE);

        // --- 색상 선택 ---
        // "색상:" 라벨 생성
        JLabel colorLabel = new JLabel("색상:");

        // 폰트 설정 (맑은 고딕, 굵게, 12px)
        colorLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        // 라벨을 도구 패널에 추가
        drawToolPanel.add(colorLabel);

        // 색상 옵션 배열 생성 (8가지 색상)
        String[] colors = {"검정색", "빨간색", "파란색", "초록색", "노란색", "주황색", "보라색", "분홍색"};
        // 색상 콤보박스 생성
        colorComboBox = new JComboBox<>(colors);
        // 폰트 설정
        colorComboBox.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        // 색상 변경 시 updateDrawingColor() 메소드 호출
        colorComboBox.addActionListener(e -> updateDrawingColor());
        // 콤보박스를 도구 패널에 추가
        drawToolPanel.add(colorComboBox);

        // --- 굵기 선택 ---
        // "굵기:" 라벨 생성
        JLabel thicknessLabel = new JLabel("굵기:");
        // 폰트 설정
        thicknessLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        // 라벨을 도구 패널에 추가
        drawToolPanel.add(thicknessLabel);

        // 굵기 옵션 배열 생성 (2px ~ 20px)
        Integer[] thicknesses = {2, 4, 6, 8, 10, 12, 15, 20};
        // 굵기 콤보박스 생성
        thicknessComboBox = new JComboBox<>(thicknesses);
        // 폰트 설정
        thicknessComboBox.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        // 기본 굵기를 6px로 설정 (인덱스 2)
        thicknessComboBox.setSelectedIndex(2);
        // 굵기 변경 시 updateDrawingThickness() 메소드 호출
        thicknessComboBox.addActionListener(e -> updateDrawingThickness());
        // 콤보박스를 도구 패널에 추가
        drawToolPanel.add(thicknessComboBox);

        // --- 지우개 버튼 ---
        // "지우개" 버튼 생성
        btnEraser = new JButton("지우개");
        // 폰트 설정
        btnEraser.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        // 버튼 배경색 설정 (주황색 계열)
        btnEraser.setBackground(new Color(255, 200, 100));
        // 포커스 테두리 제거
        btnEraser.setFocusPainted(false);
        // 클릭 시 toggleEraser() 메소드 호출
        btnEraser.addActionListener(e -> toggleEraser());
        // 버튼을 도구 패널에 추가
        drawToolPanel.add(btnEraser);

        // --- 전체 지우기 버튼 ---
        // "전체 지우기" 버튼 생성
        btnClear = new JButton("전체 지우기");
        // 폰트 설정
        btnClear.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        // 버튼 배경색 설정 (빨간색 계열)
        btnClear.setBackground(new Color(255, 100, 100));
        // 버튼 글자색을 흰색으로 설정
        btnClear.setForeground(Color.WHITE);
        // 포커스 테두리 제거
        btnClear.setFocusPainted(false);
        // 클릭 시 clearDrawing() 메소드 호출
        btnClear.addActionListener(e -> clearDrawing());
        // 버튼을 도구 패널에 추가
        drawToolPanel.add(btnClear);

        // 도구 패널을 왼쪽 패널 상단에 배치
        leftPanel.add(drawToolPanel, BorderLayout.NORTH);

        // --- 그림판 ---
        // 실제 그림을 그릴 패널 생성
        drawingPanel = new DrawingPanel();
        // 배경색을 흰색으로 설정
        drawingPanel.setBackground(Color.WHITE);
        // 회색 테두리 설정 (1px)
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        // 그림판을 왼쪽 패널 중앙에 배치
        leftPanel.add(drawingPanel, BorderLayout.CENTER);

        // ===== 오른쪽: 채팅 영역 =====
        // BorderLayout으로 채팅 영역 패널 생성 (간격 5px)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        // 배경색을 흰색으로 설정
        rightPanel.setBackground(Color.WHITE);
        // 테두리와 타이틀 설정 (어두운 청회색 테두리, "채팅" 타이틀)
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(53, 64, 82), 2),
                "채팅",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 14),
                new Color(53, 64, 82)
        ));

        // ----- 채팅 표시 영역 -----
        // 스크롤 가능한 패널 생성
        JScrollPane scrollPane = new JScrollPane();
        // 스크롤 패널 테두리 설정 (연한 회색, 1px)
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        // 텍스트 영역 생성
        textArea = new JTextArea();
        // 텍스트 영역을 편집 불가능하게 설정 (읽기 전용)
        textArea.setEditable(false);
        // 폰트 설정 (맑은 고딕, 보통체, 13px)
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        // 자동 줄 바꿈 활성화
        textArea.setLineWrap(true);
        // 단어 단위로 줄 바꿈
        textArea.setWrapStyleWord(true);
        // 텍스트 영역을 스크롤 패널에 추가
        scrollPane.setViewportView(textArea);
        // 스크롤 패널을 오른쪽 패널 중앙에 배치
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        // ----- 채팅 입력 영역 -----
        // BorderLayout으로 입력 영역 패널 생성 (가로 간격 5px)
        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        // 배경색을 흰색으로 설정
        chatInputPanel.setBackground(Color.WHITE);
        // 패널 여백 설정 (상단 5px)
        chatInputPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        // --- 사용자 이름 라벨 ---
        // "닉네임>" 형식의 라벨 생성
        lblUserName = new JLabel(username + ">");
        // 폰트 설정 (맑은 고딕, 굵게, 13px)
        lblUserName.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        // 라벨 텍스트를 가운데 정렬
        lblUserName.setHorizontalAlignment(SwingConstants.CENTER);
        // 라벨 크기 설정 (80x40)
        lblUserName.setPreferredSize(new Dimension(80, 40));
        // 글자색 설정 (어두운 청회색)
        lblUserName.setForeground(new Color(53, 64, 82));
        // 라벨을 입력 패널 왼쪽에 배치
        chatInputPanel.add(lblUserName, BorderLayout.WEST);

        // --- 메시지 입력 필드 ---
        // 텍스트 필드 생성
        txtInput = new JTextField();
        // 폰트 설정
        txtInput.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        // 테두리 설정 (연한 회색 라인 + 내부 여백)
        txtInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        // 텍스트 필드를 입력 패널 중앙에 배치
        chatInputPanel.add(txtInput, BorderLayout.CENTER);

        // --- 전송 버튼 ---
        // "전송" 버튼 생성
        btnSend = new JButton("전송");
        // 폰트 설정
        btnSend.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        // 버튼 배경색 설정 (어두운 청회색)
        btnSend.setBackground(new Color(53, 64, 82));
        // 버튼 글자색을 흰색으로 설정
        btnSend.setForeground(Color.WHITE);
        // 포커스 테두리 제거
        btnSend.setFocusPainted(false);
        // 버튼 크기 설정 (70x40)
        btnSend.setPreferredSize(new Dimension(70, 40));
        // 버튼을 입력 패널 오른쪽에 배치
        chatInputPanel.add(btnSend, BorderLayout.EAST);

        // 입력 패널을 오른쪽 패널 하단에 배치
        rightPanel.add(chatInputPanel, BorderLayout.SOUTH);

        // ===== 좌우 패널을 메인 패널에 추가 =====
        // 왼쪽 패널(그림판) 추가
        mainPanel.add(leftPanel);
        // 오른쪽 패널(채팅) 추가
        mainPanel.add(rightPanel);

        // 메인 패널을 컨텐트 패널 중앙에 배치
        contentPane.add(mainPanel, BorderLayout.CENTER);

        // 프레임을 화면에 표시
        setVisible(true);

        // ===== 네트워크 연결 =====
        // 연결 정보를 채팅창에 출력
        AppendText("User " + username + " connecting " + ip_addr + " " + port_no + "\n");
        // 사용자 이름 저장
        UserName = username;

        try {
            // 서버 소켓 생성 (IP와 포트로 연결)
            socket = new Socket(ip_addr, Integer.parseInt(port_no));
            // 입력 스트림 얻기
            is = socket.getInputStream();
            // 데이터 입력 스트림 생성
            dis = new DataInputStream(is);
            // 출력 스트림 얻기
            os = socket.getOutputStream();
            // 데이터 출력 스트림 생성
            dos = new DataOutputStream(os);

            // 서버에 로그인 메시지 전송 ("/login 닉네임" 형식)
            SendMessage("/login " + UserName);
            // 서버 메시지를 수신하는 스레드 생성
            ListenNetwork net = new ListenNetwork();
            // 수신 스레드 시작
            net.start();
            // 버튼 액션 리스너 생성
            Myaction action = new Myaction();
            // 전송 버튼에 리스너 등록
            btnSend.addActionListener(action);
            // 텍스트 필드에 리스너 등록 (엔터 키로도 전송 가능)
            txtInput.addActionListener(action);
            // 텍스트 필드에 포커스 설정
            txtInput.requestFocus();
        } catch (NumberFormatException | IOException e) {
            // 예외 스택 트레이스 출력
            e.printStackTrace();
            // 연결 오류 메시지 출력
            AppendText("connect error\n");
        }
    }


    /**
     * 그림판 색상 업데이트 메소드
     */
    private void updateDrawingColor() {
        // 선택된 색상 이름 가져오기
        String selectedColor = (String) colorComboBox.getSelectedItem();
        // Color 객체 선언
        Color color;
        // 선택된 색상에 따라 Color 객체 설정
        switch (selectedColor) {
            case "빨간색": color = Color.RED; break;
            case "파란색": color = Color.BLUE; break;
            case "초록색": color = Color.GREEN; break;
            case "노란색": color = Color.YELLOW; break;
            case "주황색": color = Color.ORANGE; break;
            case "보라색": color = new Color(128, 0, 128); break;
            case "분홍색": color = Color.PINK; break;
            default: color = Color.BLACK; // 기본값은 검정색
        }
        // 그림판에 선택된 색상 설정
        drawingPanel.setDrawingColor(color);
        // 지우개 모드 해제
        drawingPanel.setEraserMode(false);
        // 지우개 버튼 색상을 기본 색상(주황색)으로 변경
        btnEraser.setBackground(new Color(255, 200, 100));
    }

    /**
     * 그림판 선 굵기 업데이트 메소드
     */
    private void updateDrawingThickness() {
        // 선택된 굵기 값 가져오기
        Integer thickness = (Integer) thicknessComboBox.getSelectedItem();
        // 그림판에 선택된 굵기 설정
        drawingPanel.setStrokeThickness(thickness);
    }

    /**
     * 지우개 모드 토글 메소드
     */
    private void toggleEraser() {
        // 현재 지우개 모드의 반대 상태 가져오기
        boolean isEraser = !drawingPanel.isEraserMode();
        // 그림판에 지우개 모드 설정
        drawingPanel.setEraserMode(isEraser);
        // 지우개 모드가 활성화되면
        if (isEraser) {
            // 버튼 색상을 초록색으로 변경 (활성 상태 표시)
            btnEraser.setBackground(new Color(100, 255, 100));
        } else {
            // 버튼 색상을 주황색으로 변경 (비활성 상태 표시)
            btnEraser.setBackground(new Color(255, 200, 100));
        }
    }

    /**
     * 그림판 전체 지우기 메소드
     */
    private void clearDrawing() {
        // 확인 대화상자 표시
        int result = JOptionPane.showConfirmDialog(
                this,
                "그림을 모두 지우시겠습니까?",
                "확인",
                JOptionPane.YES_NO_OPTION
        );
        // 사용자가 '예'를 선택하면
        if (result == JOptionPane.YES_OPTION) {
            // 그림판 초기화
            drawingPanel.clear();
            // 서버에 전체 지우기 명령 전송
            SendDrawCommand("/clear");
        }
    }

    /**
     * 그림 명령을 서버로 전송하는 메소드
     * @param command 그림 명령 문자열
     */
    private void SendDrawCommand(String command) {
        try {
            // UTF 문자열 형식으로 서버에 그림 명령 전송
            dos.writeUTF(command);
        } catch (IOException e) {
            AppendText("그림 데이터 전송 실패\n");
        }
    }

    /**
     * 서버로부터 받은 그림 명령을 처리하는 메소드
     * @param command 그림 명령 문자열
     */
    private void ProcessDrawCommand(String command) {
        // 명령어를 공백으로 분리
        String[] parts = command.split(" ");

        // 전체 지우기 명령
        if (parts[0].equals("/clear")) {
            drawingPanel.clear();
        }
        // 그리기 명령: /draw x1 y1 x2 y2 r g b thickness isEraser
        else if (parts[0].equals("/draw") && parts.length >= 10) {
            try {
                int x1 = Integer.parseInt(parts[1]);
                int y1 = Integer.parseInt(parts[2]);
                int x2 = Integer.parseInt(parts[3]);
                int y2 = Integer.parseInt(parts[4]);
                int r = Integer.parseInt(parts[5]);
                int g = Integer.parseInt(parts[6]);
                int b = Integer.parseInt(parts[7]);
                int thickness = Integer.parseInt(parts[8]);
                boolean isEraser = Boolean.parseBoolean(parts[9]);

                // 다른 사용자의 그림을 로컬 그림판에 그리기
                drawingPanel.drawRemoteLine(x1, y1, x2, y2, new Color(r, g, b), thickness, isEraser);
            } catch (NumberFormatException e) {
                AppendText("잘못된 그림 데이터\n");
            }
        }
    }

    /**
     * 그림판 패널 내부 클래스 - 실제 그림 그리기 기능 구현
     */
    class DrawingPanel extends JPanel {
        // 그림을 저장할 이미지 객체
        private Image image;
        // 그래픽스 객체 (그림 그리기용)
        private Graphics2D g2;
        // 현재 마우스 좌표
        private int currentX, currentY;
        // 이전 마우스 좌표
        private int oldX, oldY;
        // 현재 그리기 색상 (기본값: 검정색)
        private Color drawingColor = Color.BLACK;
        // 선 굵기 (기본값: 6px)
        private int strokeThickness = 6;
        // 지우개 모드 여부 (기본값: false)
        private boolean eraserMode = false;

        /**
         * DrawingPanel 생성자 - 마우스 이벤트 리스너 설정
         */
        public DrawingPanel() {
            // 더블 버퍼링 비활성화 (직접 그리기 위해)
            setDoubleBuffered(false);
            // 마우스 리스너 추가
            addMouseListener(new MouseAdapter() {
                // 마우스 버튼을 눌렀을 때
                public void mousePressed(MouseEvent e) {
                    // 현재 마우스 위치를 이전 위치로 저장
                    oldX = e.getX();
                    oldY = e.getY();
                }
            });

            // 마우스 모션 리스너 추가
            addMouseMotionListener(new MouseMotionAdapter() {
                // 마우스를 드래그할 때
                public void mouseDragged(MouseEvent e) {
                    // 현재 마우스 위치 저장
                    currentX = e.getX();
                    currentY = e.getY();

                    // 그래픽스 객체가 초기화되어 있으면
                    if (g2 != null) {
                        // 로컬에서 선 그리기
                        drawLocalLine(oldX, oldY, currentX, currentY);

                        // 서버로 그림 데이터 전송
                        // 형식: /draw x1 y1 x2 y2 r g b thickness isEraser
                        Color color = eraserMode ? Color.WHITE : drawingColor;
                        String drawCommand = String.format("/draw %d %d %d %d %d %d %d %d %b",
                                oldX, oldY, currentX, currentY,
                                color.getRed(), color.getGreen(), color.getBlue(),
                                eraserMode ? strokeThickness * 2 : strokeThickness,
                                eraserMode);
                        SendDrawCommand(drawCommand);

                        // 화면 다시 그리기
                        repaint();
                        // 현재 위치를 이전 위치로 업데이트
                        oldX = currentX;
                        oldY = currentY;
                    }
                }
            });
        }

        /**
         * 로컬에서 선 그리기 (자신의 그림)
         */
        private void drawLocalLine(int x1, int y1, int x2, int y2) {
            // 지우개 모드인 경우
            if (eraserMode) {
                // 흰색으로 설정 (지우기)
                g2.setPaint(Color.WHITE);
                // 지우개 굵기를 선 굵기의 2배로 설정
                g2.setStroke(new BasicStroke(strokeThickness * 2));
            } else {
                // 선택된 그리기 색상으로 설정
                g2.setPaint(drawingColor);
                // 선 굵기와 스타일 설정 (둥근 끝, 둥근 연결)
                g2.setStroke(new BasicStroke(strokeThickness,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            // 이전 위치에서 현재 위치까지 선 그리기
            g2.drawLine(x1, y1, x2, y2);
        }

        /**
         * 원격에서 받은 선 그리기 (다른 사용자의 그림)
         */
        public void drawRemoteLine(int x1, int y1, int x2, int y2, Color color, int thickness, boolean isEraser) {
            if (g2 != null) {
                // 지우개 모드인 경우
                if (isEraser) {
                    g2.setPaint(Color.WHITE);
                    g2.setStroke(new BasicStroke(thickness));
                } else {
                    g2.setPaint(color);
                    g2.setStroke(new BasicStroke(thickness,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }
                // 선 그리기
                g2.drawLine(x1, y1, x2, y2);
                // 화면 다시 그리기
                repaint();
            }
        }

        /**
         * 패널 그리기 메소드 - 화면에 이미지를 그림
         */
        protected void paintComponent(Graphics g) {
            // 부모 클래스의 paintComponent 호출
            super.paintComponent(g);
            // 이미지가 아직 생성되지 않았으면
            if (image == null) {
                // 패널 크기만큼 이미지 생성
                image = createImage(getWidth(), getHeight());
                // 이미지의 그래픽스 객체 얻기
                g2 = (Graphics2D) image.getGraphics();
                // 안티앨리어싱 활성화 (부드러운 선 그리기)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // 이미지 초기화 (흰색으로 채우기)
                clear();
            }
            // 이미지를 화면에 그리기
            g.drawImage(image, 0, 0, null);
        }

        /**
         * 그림판 초기화 메소드 - 흰색으로 채우기
         */
        public void clear() {
            // 그래픽스 객체가 있으면
            if (g2 != null) {
                // 흰색으로 설정
                g2.setPaint(Color.WHITE);
                // 전체 영역을 흰색으로 채우기
                g2.fillRect(0, 0, getWidth(), getHeight());
                // 그리기 색상으로 다시 설정
                g2.setPaint(drawingColor);
                // 화면 다시 그리기
                repaint();
            }
        }

        /**
         * 그리기 색상 설정 메소드
         */
        public void setDrawingColor(Color color) {
            this.drawingColor = color;
        }

        /**
         * 선 굵기 설정 메소드
         */
        public void setStrokeThickness(int thickness) {
            this.strokeThickness = thickness;
        }

        /**
         * 지우개 모드 설정 메소드
         */
        public void setEraserMode(boolean mode) {
            this.eraserMode = mode;
        }

        /**
         * 지우개 모드 여부 반환 메소드
         */
        public boolean isEraserMode() {
            return eraserMode;
        }
    }

    /**
     * 서버 메시지 수신 스레드 - 서버로부터 메시지를 계속 받아옴
     */
    class ListenNetwork extends Thread {
        public void run() {
            // 무한 루프로 계속 메시지 수신
            while (true) {
                try {
                    // 서버로부터 UTF 문자열 읽기
                    String msg = dis.readUTF();

                    // 그림 명령인 경우 (/draw 또는 /clear로 시작)
                    if (msg.startsWith("/draw") || msg.startsWith("/clear")) {
                        // 그림 명령 처리
                        ProcessDrawCommand(msg);
                    } else {
                        // 일반 채팅 메시지를 채팅창에 출력
                        AppendText(msg);
                    }
                } catch (IOException e) {
                    // 연결 종료 메시지 출력
                    AppendText("연결이 종료되었습니다.\n");
                    try {
                        // 출력 스트림 닫기
                        dos.close();
                        // 입력 스트림 닫기
                        dis.close();
                        // 소켓 닫기
                        socket.close();
                        // 루프 종료
                        break;
                    } catch (Exception ee) {
                        // 예외 발생 시 루프 종료
                        break;
                    }
                }
            }
        }
    }

    /**
     * 메시지 전송 액션 리스너 - 전송 버튼이나 엔터 키 입력 처리
     */
    class Myaction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 전송 버튼이나 텍스트 필드(엔터 키)에서 이벤트 발생 시
            if (e.getSource() == btnSend || e.getSource() == txtInput) {
                // 입력된 메시지 가져오기 (앞뒤 공백 제거)
                String msg = txtInput.getText().trim();
                // 메시지가 비어있지 않으면
                if (!msg.isEmpty()) {
                    // "[닉네임] 메시지" 형식으로 포맷팅
                    String formattedMsg = String.format("[%s] %s", UserName, msg);
                    // 서버로 메시지 전송
                    SendMessage(formattedMsg);
                    // 입력 필드 초기화
                    txtInput.setText("");
                    // 입력 필드에 포커스 다시 설정
                    txtInput.requestFocus();

                    // 메시지에 "/exit"가 포함되어 있으면
                    if (msg.contains("/exit")) {
                        // 프로그램 종료
                        System.exit(0);
                    }
                }
            }
        }
    }

    /**
     * 채팅창에 텍스트 출력 메소드
     * @param msg 출력할 메시지
     */
    public void AppendText(String msg) {
        // 텍스트 영역에 메시지 추가
        textArea.append(msg);
        // 커서를 텍스트 끝으로 이동 (자동 스크롤)
        textArea.setCaretPosition(textArea.getText().length());
    }

    /**
     * 서버로 메시지 전송 메소드
     * @param msg 전송할 메시지
     */
    public void SendMessage(String msg) {
        try {
            // UTF 문자열 형식으로 서버에 메시지 전송
            dos.writeUTF(msg);
        } catch (IOException e) {
            // 전송 실패 메시지 출력
            AppendText("메시지 전송 실패\n");
            try {
                // 출력 스트림 닫기
                dos.close();
                // 입력 스트림 닫기
                dis.close();
                // 소켓 닫기
                socket.close();
            } catch (IOException e1) {
                // 예외 스택 트레이스 출력
                e1.printStackTrace();
                // 프로그램 종료
                System.exit(0);
            }
        }
    }
}