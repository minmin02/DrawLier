package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class VotingUI extends JFrame {
    private String userName;
    private List<String> players;
    private DataOutputStream dos;

    // 연결 정보 (ResultUI로 전달하기 위함)
    private String serverIp;
    private String serverPort;

    private int myIndex = -1;

    public VotingUI(String userName, List<String> players, DataOutputStream dos,
                    String serverIp, String serverPort) {
        this.userName = userName;
        this.players = players;
        this.dos = dos;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        this.myIndex = players.indexOf(userName);

        setTitle("라이어 투표");
        setSize(450, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 강제 종료 방지
        setLayout(new BorderLayout(10, 10));

        // 상단 안내 패널
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(255, 245, 230));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel("🎭 라이어를 투표해주세요!");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        titleLabel.setForeground(new Color(220, 53, 69));
        topPanel.add(titleLabel);

        add(topPanel, BorderLayout.NORTH);

        // 중앙 버튼 패널
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(players.size(), 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        buttonPanel.setBackground(Color.WHITE);

        // 투표 버튼 생성
        for (int i = 0; i < players.size(); i++) {
            int idx = i;
            String playerName = players.get(i);

            JButton btn = new JButton(playerName);
            btn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            btn.setPreferredSize(new Dimension(300, 60));

            // 자기 자신 투표 금지
            if (idx == myIndex) {
                btn.setEnabled(false);
                btn.setText(playerName + " (본인)");
                btn.setBackground(new Color(200, 200, 200));
                btn.setForeground(Color.GRAY);
            } else {
                btn.setBackground(new Color(66, 133, 244));
                btn.setForeground(Color.WHITE);
                btn.setFocusPainted(false);

                btn.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        btn.setBackground(new Color(50, 100, 200));
                    }
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        btn.setBackground(new Color(66, 133, 244));
                    }
                });

                btn.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(
                            this,
                            playerName + "님을 라이어로 투표하시겠습니까?",
                            "투표 확인",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (confirm == JOptionPane.YES_OPTION) {
                        vote(idx);
                    }
                });
            }

            buttonPanel.add(btn);
        }

        JScrollPane scrollPane = new JScrollPane(buttonPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 안내 패널
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(245, 245, 245));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));

        JLabel infoLabel = new JLabel("💡 가장 의심스러운 플레이어를 선택하세요");
        infoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        infoLabel.setForeground(Color.DARK_GRAY);
        bottomPanel.add(infoLabel);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    /* 투표 처리*/
    private void vote(int targetIdx) {
        String votedPlayer = players.get(targetIdx);

        // 서버로 투표 전송
        try {
            dos.writeUTF("/vote " + votedPlayer);
            dos.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "투표 전송 실패: " + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }

        // ResultUI로 이동 (연결 정보 전달)
        SwingUtilities.invokeLater(() -> {
            new ResultUI(userName, players, votedPlayer, serverIp, serverPort);
            dispose();
        });
    }
}