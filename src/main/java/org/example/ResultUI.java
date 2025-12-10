package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.Socket;
import java.net.URL;

public class ResultUI extends JFrame {
    //사용자 및 게임 정보
    private String userName;
    private boolean citizenWin; //시민 승리 여부
    private String resultMessage; //결과 메시지
    private String roomId;
    private String serverIp;
    private String serverPort;

    //네트워크 관련 필드
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    //생성자: 게임 결과 정보를 받아 UI를 초기화
    public ResultUI(String userName, boolean citizenWin, String resultMessage, String roomId, String serverIp, String serverPort, Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.userName = userName;
        this.citizenWin = citizenWin;
        this.resultMessage = resultMessage;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        initializeUI();
    }

    //결과 UI를 초기화하는 메소드
    private void initializeUI() {
        setTitle("DrawLier - 게임 결과");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); //창을 닫을 때 임의로 종료되지 않도록 설정
        setBounds(100, 100, 650, 550);

        //배경 이미지를 그리는 패널 설정
        JPanel contentPane = new JPanel() {
            private BufferedImage bgImage;
            {
                try {
                    //승패 결과에 따라 다른 배경 이미지 로드
                    String fileName = citizenWin ? "CitizenWin.png" : "LiarWin.png";
                    URL imageUrl = getClass().getResource("/ResultTabUI/" + fileName);
                    if (imageUrl != null) {
                        bgImage = ImageIO.read(imageUrl);
                    } else {
                        System.err.println("이미지 리소스를 찾을 수 없습니다: /ResultTabUI/" + fileName);
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
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        //중앙 메시지 패널 (투명)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(200, 175, 60, 100)); //배경 이미지에 맞게 여백 설정

        //결과 메시지를 담을 패널
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false); //배경 투명

        //메시지 제목
        JLabel lblMessageTitle = new JLabel("게임 결과 상세");
        lblMessageTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        lblMessageTitle.setForeground(Color.WHITE);
        lblMessageTitle.setBorder(new EmptyBorder(0, 0, 5, 0));

        //결과 메시지 텍스트 영역
        JTextArea txtMessage = new JTextArea(resultMessage);
        txtMessage.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        txtMessage.setForeground(Color.WHITE);
        txtMessage.setEditable(false);
        txtMessage.setOpaque(false);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(lblMessageTitle, BorderLayout.NORTH);
        textPanel.add(txtMessage, BorderLayout.CENTER);
        messagePanel.add(textPanel, BorderLayout.CENTER);

        //메시지가 길어질 경우를 대비한 스크롤 패널
        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        //하단 버튼 패널
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 30, 0));

        //'로비로 이동' 버튼 생성
        JButton btnConfirm = new JButton();
        btnConfirm.setPreferredSize(new Dimension(100, 30));
        try {
            URL btnUrl = getClass().getResource("/PlayUI/GoToLobby.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(100, 30, Image.SCALE_SMOOTH);
                btnConfirm.setIcon(new ImageIcon(img));
                UIUtils.applyButtonEffects(btnConfirm); //공용 버튼 효과 적용
            } else {
                btnConfirm.setText("로비로 이동");
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnConfirm.setText("로비로 이동");
        }

        //'로비로 이동' 버튼 클릭 이벤트
        btnConfirm.addActionListener(e -> {
            try {
                //서버에 방을 나간다는 프로토콜 전송
                if (dos != null) {
                    dos.writeUTF("/leaveRoom");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                //기존 연결을 유지한 채 로비 UI로 전환
                new RoomListUI(userName, serverIp, serverPort, socket, dis, dos).setVisible(true);
                dispose(); //현재 결과 창 닫기
            });
        });

        bottomPanel.add(btnConfirm);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        //창 종료 시 처리
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    //서버에 방 나가기 신호를 보내고 모든 연결 종료
                    if (dos != null) {
                        dos.writeUTF("/leaveRoom");
                        dos.close();
                    }
                    if(dis != null) dis.close();
                    if(socket != null) socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(0); //프로그램 완전 종료
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }
}
