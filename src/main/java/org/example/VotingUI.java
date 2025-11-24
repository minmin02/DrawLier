package org.example;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 투표 UI - 게임 종료 후 라이어를 찾는 투표 화면
 */
public class VotingUI extends JFrame {
    private String userName;
    private List<String> players;
    private DataOutputStream dos;
    private JLabel lblTitle;
    private JPanel votingPanel;
    private JButton[] voteButtons;
    // 해시맵 -> 리스트
    private String selectedPlayer;

    public VotingUI(String userName, List<String> players, DataOutputStream dos) {
        this.userName = userName;
        this.players = players;
        this.dos = dos;

        initializeUI();
    }

    private void initializeUI() {
        setTitle("DrawLier - 투표 시간!");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 600, 500);

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBackground(new Color(245, 245, 250));
        setContentPane(contentPane);

        // 상단 제목
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        lblTitle = new JLabel("누가 라이어일까요?");
        lblTitle.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        lblTitle.setForeground(new Color(50, 50, 150));
        topPanel.add(lblTitle);
        contentPane.add(topPanel, BorderLayout.NORTH);

        // 중앙 투표 패널
        votingPanel = new JPanel();
        votingPanel.setLayout(new GridLayout(2, 2, 20, 20));
        votingPanel.setOpaque(false);
        votingPanel.setBorder(new EmptyBorder(30, 50, 30, 50));

        voteButtons = new JButton[players.size()];

        for (int i = 0; i < players.size(); i++) {
            String player = players.get(i);
            JButton btnVote = new JButton();

            // 버튼 스타일링
            btnVote.setLayout(new BorderLayout());
            btnVote.setBackground(Color.WHITE);
            btnVote.setFocusPainted(false);
            btnVote.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                    new EmptyBorder(10, 10, 10, 10)
            ));

            // 플레이어 아이콘
            JLabel iconLabel = new JLabel("👤", SwingConstants.CENTER);
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 50));
            btnVote.add(iconLabel, BorderLayout.CENTER);

            // 플레이어 이름
            JLabel nameLabel = new JLabel(player, SwingConstants.CENTER);
            nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            nameLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

            // 자신은 투표 불가
            if (player.equals(userName)) {
                nameLabel.setText(player + " (나)");
                nameLabel.setForeground(Color.GRAY);
                btnVote.setEnabled(false);
                btnVote.setBackground(new Color(240, 240, 240));
            } else {
                nameLabel.setForeground(new Color(50, 50, 50));
            }

            btnVote.add(nameLabel, BorderLayout.SOUTH);

            // 버튼 호버 효과
            final JButton finalBtn = btnVote;
            final String finalPlayer = player;

            btnVote.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (finalBtn.isEnabled()) {
                        finalBtn.setBackground(new Color(230, 240, 255));
                        finalBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(100, 150, 255), 3),
                                new EmptyBorder(10, 10, 10, 10)
                        ));
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (finalBtn.isEnabled() && !finalPlayer.equals(selectedPlayer)) {
                        finalBtn.setBackground(Color.WHITE);
                        finalBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                                new EmptyBorder(10, 10, 10, 10)
                        ));
                    }
                }
            });

            btnVote.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectPlayer(finalPlayer, finalBtn);
                }
            });

            voteButtons[i] = btnVote;
            votingPanel.add(btnVote);
        }

        contentPane.add(votingPanel, BorderLayout.CENTER);

        // 하단 확인 버튼
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton btnConfirm = new JButton("투표하기");
        btnConfirm.setPreferredSize(new Dimension(200, 50));
        btnConfirm.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        btnConfirm.setBackground(new Color(100, 150, 255));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setBorder(BorderFactory.createEmptyBorder());

        btnConfirm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitVote();
            }
        });

        bottomPanel.add(btnConfirm);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void selectPlayer(String player, JButton button) {
        // 이전 선택 초기화
        for (int i = 0; i < voteButtons.length; i++) {
            if (voteButtons[i].isEnabled()) {
                voteButtons[i].setBackground(Color.WHITE);
                voteButtons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }
        }

        // 새로운 선택 표시
        selectedPlayer = player;
        button.setBackground(new Color(200, 230, 255));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 100, 255), 4),
                new EmptyBorder(10, 10, 10, 10)
        ));
    }

    private void submitVote() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this,
                    "투표할 플레이어를 선택해주세요!",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 서버로 투표 정보 전송
            dos.writeUTF("/vote " + selectedPlayer);

            //메시지 처리
            JOptionPane.showMessageDialog(this,
                    selectedPlayer + "님에게 투표하였습니다!",
                    "투표 완료",
                    JOptionPane.INFORMATION_MESSAGE);


            // 승리 패널 팝업
            // 투표 완료 후 창 닫기
            //panel 이동
            // 죽여
            dispose();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "투표 전송 중 오류가 발생했습니다.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}