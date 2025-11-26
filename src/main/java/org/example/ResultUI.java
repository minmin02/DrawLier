package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * 투표 결과 화면
 * - 투표 결과 표시
 * - 확인 버튼 클릭 시 RoomListUI로 이동
 */
public class ResultUI extends JFrame {
    private String userName;
    private List<String> players;
    private String votedPlayer;

    // 연결 정보
    private String serverIp;
    private String serverPort;

    public ResultUI(String userName, List<String> players, String votedPlayer,
                    String serverIp, String serverPort) {
        this.userName = userName;
        this.players = players;
        this.votedPlayer = votedPlayer;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        setTitle("투표 결과");
        setSize(500, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));

        // 배경색 설정
        JPanel contentPane = new JPanel(new BorderLayout(15, 15));
        contentPane.setBackground(Color.WHITE);
        contentPane.setBorder(new EmptyBorder(30, 40, 30, 40));
        setContentPane(contentPane);

        // 상단 제목
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🎯 투표 결과");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        titleLabel.setForeground(new Color(220, 53, 69));
        topPanel.add(titleLabel);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // 중앙 결과 표시
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 투표한 플레이어 표시
        JLabel voteLabel = new JLabel("당신의 투표:");
        voteLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        voteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(voteLabel);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel votedLabel = new JLabel(votedPlayer);
        votedLabel.setFont(new Font("맑은 고딕", Font.BOLD, 32));
        votedLabel.setForeground(new Color(220, 53, 69));
        votedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(votedLabel);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // 안내 메시지
        JLabel infoLabel = new JLabel("다른 플레이어의 투표를 기다리는 중...");
        infoLabel.setFont(new Font("맑은 고딕", Font.ITALIC, 14));
        infoLabel.setForeground(Color.GRAY);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(infoLabel);

        contentPane.add(centerPanel, BorderLayout.CENTER);

        // 하단 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);

        JButton btnConfirm = new JButton("방 목록으로 돌아가기");
        btnConfirm.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btnConfirm.setPreferredSize(new Dimension(250, 50));
        btnConfirm.setBackground(new Color(66, 133, 244));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);

        btnConfirm.addActionListener(e -> {
            // RoomListUI로 이동 (연결 정보 전달)
            SwingUtilities.invokeLater(() -> {
                RoomListUI roomListUI = new RoomListUI(userName, serverIp, serverPort);
                roomListUI.setVisible(true);
                dispose();
            });
        });

        bottomPanel.add(btnConfirm);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }
}