package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 투표 UI - 수정됨
 * 라이어 적발 시 스트림 충돌 방지를 위해 리스너 스레드 종료 처리 추가
 */
public class VotingUI extends JFrame {
    private String userName;
    private List<String> players;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String roomId;
    private String serverIp;
    private String serverPort;

    private JLabel lblTitle;
    private JPanel votingPanel;
    private JButton[] voteButtons;
    private String selectedPlayer;
    private boolean hasVoted = false;

    public VotingUI(String userName, List<String> players, DataOutputStream dos, DataInputStream dis, String roomId, String serverIp, String serverPort) {
        this.userName = userName;
        this.players = players;
        this.dos = dos;
        this.dis = dis;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        initializeUI();
        new ListenVoteResult().start();
    }

    private void initializeUI() {
        setTitle("DrawLier - 투표 시간!");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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

            final JButton finalBtn = btnVote;
            final String finalPlayer = player;

            btnVote.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (finalBtn.isEnabled() && !hasVoted) {
                        finalBtn.setBackground(new Color(230, 240, 255));
                        finalBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(100, 150, 255), 3),
                                new EmptyBorder(10, 10, 10, 10)
                        ));
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (finalBtn.isEnabled() && !hasVoted && !finalPlayer.equals(selectedPlayer)) {
                        finalBtn.setBackground(Color.WHITE);
                        finalBtn.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                                new EmptyBorder(10, 10, 10, 10)
                        ));
                    }
                }
            });

            btnVote.addActionListener(e -> {
                if (!hasVoted) {
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

        btnConfirm.addActionListener(e -> submitVote());

        bottomPanel.add(btnConfirm);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void selectPlayer(String player, JButton button) {
        if (hasVoted) return;

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
        if (hasVoted) {
            JOptionPane.showMessageDialog(this,
                    "이미 투표하셨습니다!",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this,
                    "투표할 플레이어를 선택해주세요!",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            dos.writeUTF("/vote " + selectedPlayer);
            hasVoted = true;

            // 버튼 비활성화
            for (JButton btn : voteButtons) {
                btn.setEnabled(false);
            }

            lblTitle.setText("투표 완료! 결과를 기다리는 중...");
            lblTitle.setForeground(new Color(100, 100, 100));

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "투표 전송 중 오류가 발생했습니다.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    class ListenVoteResult extends Thread {
        public void run() {
            try {
                while (true) {
                    String msg = dis.readUTF();
                    System.out.println("[VotingUI] 수신한 메시지: " + msg);

                    if (msg.startsWith("/voteResult ")) {
                        // "/voteResult 최다득표자|라이어여부"
                        String[] parts = msg.substring(12).split("\\|");
                        String mostVoted = parts[0];
                        boolean isLiar = parts[1].equals("true");

                        System.out.println("[VotingUI] 투표 결과 - 최다득표: " + mostVoted + ", 라이어: " + isLiar);

                        SwingUtilities.invokeLater(() -> {
                            if (isLiar) {
                                // 라이어가 최다 득표 -> 라이어에게 답 입력 기회
                                System.out.println("[VotingUI] 라이어 적발! userName=" + userName + ", mostVoted=" + mostVoted);
                                if (userName.equals(mostVoted)) {
                                    System.out.println("[VotingUI] 본인이 라이어 - InsertAnswerUI 열기");
                                    // 라이어 본인만 InsertAnswerUI로 전환
                                    new InsertAnswerUI(userName, dos, dis, roomId, serverIp, serverPort);
                                    dispose();
                                } else {
                                    System.out.println("[VotingUI] 다른 플레이어 - 대기 중 (dispose 안 함)");
                                    lblTitle.setText(mostVoted + "님이 라이어로 적발! 정답 맞추기 진행 중...");
                                    lblTitle.setForeground(new Color(220, 53, 69));

                                    // 투표 버튼들 비활성화
                                    for (JButton btn : voteButtons) {
                                        btn.setEnabled(false);
                                    }

                                    // 투표 패널을 대기 메시지로 교체
                                    votingPanel.removeAll();
                                    JLabel waitLabel = new JLabel("<html><center>라이어 " + mostVoted + "님이<br>정답을 맞추는 중입니다...<br><br>잠시만 기다려주세요</center></html>");
                                    waitLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
                                    waitLabel.setForeground(new Color(100, 100, 100));
                                    waitLabel.setHorizontalAlignment(SwingConstants.CENTER);
                                    votingPanel.setLayout(new BorderLayout());
                                    votingPanel.add(waitLabel, BorderLayout.CENTER);
                                    votingPanel.revalidate();
                                    votingPanel.repaint();
                                }
                            } else {
                                // 라이어가 아닌 사람이 최다 득표 -> 라이어 승리 (게임 즉시 종료)
                                System.out.println("[VotingUI] 시민이 억울하게 투표됨 - ResultUI 열기");
                                new ResultUI(userName, false, mostVoted + "님이 억울하게 투표되었습니다! 실제 라이어는 다른 플레이어였습니다.\n정답을 확인하세요!", dos, roomId, serverIp, serverPort);
                                dispose();
                            }
                        });

                        // ★ 중요 수정 ★
                        // 라이어 본인은 InsertAnswerUI로 넘어가서 새 리스너를 시작하므로,
                        // 여기서는 스트림을 놔주기 위해 루프를 탈출해야 함.
                        if (isLiar && userName.equals(mostVoted)) {
                            System.out.println("[VotingUI] 라이어 화면 전환으로 인한 리스너 스레드 종료");
                            break;
                        }

                        // 라이어가 아닌 사람이 뽑혔다면 게임이 끝났으므로 루프 종료
                        if (!isLiar) {
                            break;
                        }

                        // 라이어가 아닌 플레이어들은 루프를 계속 돌며 /finalResult를 기다림
                    }
                    else if (msg.startsWith("/finalResult ")) {
                        // 라이어의 정답 맞추기 결과 (시민들 화면에서 수신)
                        System.out.println("[VotingUI] 최종 결과 수신");
                        String[] parts = msg.substring(13).split("\\|", 3);
                        boolean citizenWin = parts[0].equals("CITIZEN");
                        String message = parts[2];

                        SwingUtilities.invokeLater(() -> {
                            System.out.println("[VotingUI] ResultUI 열기 - 시민승리: " + citizenWin);
                            new ResultUI(userName, citizenWin, message, dos, roomId, serverIp, serverPort);
                            dispose();
                        });
                        break; // 게임 종료
                    }
                }
            } catch (IOException e) {
                System.err.println("투표 결과 수신 오류");
                e.printStackTrace();
            }
        }
    }
}