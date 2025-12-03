package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class VotingUI extends JFrame {
    private String userName;
    private List<String> players;
    private String roomId;
    private String serverIp;
    private String serverPort;

    // ★★★ [추가] 소켓 및 스트림 필드
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private JPanel votingPanel;
    private JButton[] voteButtons;
    private String selectedPlayer;
    private boolean hasVoted = false;

    // ★★★ [수정] 생성자에서 소켓과 스트림을 받도록 변경
    public VotingUI(String userName, List<String> players, String roomId, String serverIp, String serverPort, Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.userName = userName;
        this.players = players;
        this.roomId = roomId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;

        initializeUI();
        new ListenVoteResult().start();
    }

    private void initializeUI() {
        setTitle("DrawLier - 투표 시간!");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 600, 500);

        JPanel contentPane = new JPanel() {
            private Image bgImage;
            {
                URL url = getClass().getResource("/PlayUI/Voting.png");
                if (url != null) {
                    bgImage = new ImageIcon(url).getImage();
                } else {
                    System.err.println("배경 이미지를 찾을 수 없습니다: /PlayUI/Voting.png");
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(245, 245, 250));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPane.setLayout(new BorderLayout(10, 10));
        setContentPane(contentPane);

        votingPanel = new JPanel();
        votingPanel.setLayout(new GridLayout(2, 2, 20, 20));
        votingPanel.setOpaque(false);
        votingPanel.setBorder(new EmptyBorder(70, 50, 30, 50));

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

            JLabel iconLabel = new JLabel("👤", SwingConstants.CENTER);
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
            btnVote.add(iconLabel, BorderLayout.CENTER);

            JLabel nameLabel = new JLabel(player, SwingConstants.CENTER);
            nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            nameLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

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

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 20, 0));

        JButton btnConfirm = new JButton();
        btnConfirm.setPreferredSize(new Dimension(100, 30));

        try {
            URL btnUrl = getClass().getResource("/PlayUI/VoteButton.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(100, 30, Image.SCALE_SMOOTH);
                btnConfirm.setIcon(new ImageIcon(img));
                UIUtils.applyButtonEffects(btnConfirm);
            } else {
                btnConfirm.setText("투표하기");
                System.err.println("이미지를 찾을 수 없습니다: /PlayUI/VoteButton.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnConfirm.setText("투표하기");
        }

        btnConfirm.addActionListener(e -> submitVote());
        bottomPanel.add(btnConfirm);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void selectPlayer(String player, JButton button) {
        if (hasVoted) return;

        for (int i = 0; i < voteButtons.length; i++) {
            if (voteButtons[i].isEnabled()) {
                voteButtons[i].setBackground(Color.WHITE);
                voteButtons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }
        }

        selectedPlayer = player;
        button.setBackground(new Color(200, 230, 255));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 100, 255), 4),
                new EmptyBorder(10, 10, 10, 10)
        ));
    }

    private void submitVote() {
        if (hasVoted) {
            JOptionPane.showMessageDialog(this, "이미 투표하셨습니다!", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "투표할 플레이어를 선택해주세요!", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            dos.writeUTF("/vote " + selectedPlayer);
            hasVoted = true;

            for (JButton btn : voteButtons) {
                btn.setEnabled(false);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "투표 전송 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
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
                        String[] parts = msg.substring(12).split("\\|");
                        String mostVoted = parts[0];
                        boolean isLiar = parts[1].equals("true");

                        SwingUtilities.invokeLater(() -> {
                            if (isLiar) {
                                if (userName.equals(mostVoted)) {
                                    // ★★★ [수정] InsertAnswerUI 생성자에 소켓과 스트림 전달
                                    new InsertAnswerUI(userName, roomId, serverIp, serverPort, socket, dis, dos);
                                    dispose();
                                } else {
                                    JPanel fullWaitPanel = new JPanel() {
                                        private Image waitImage;
                                        {
                                            URL url = getClass().getResource("/PlayUI/WaitingUI.png");
                                            if (url != null) {
                                                waitImage = new ImageIcon(url).getImage();
                                            }
                                        }

                                        @Override
                                        protected void paintComponent(Graphics g) {
                                            super.paintComponent(g);
                                            if (waitImage != null) {
                                                g.drawImage(waitImage, 0, 0, getWidth(), getHeight(), this);
                                            } else {
                                                g.setColor(Color.BLACK);
                                                g.fillRect(0, 0, getWidth(), getHeight());
                                            }
                                        }
                                    };
                                    fullWaitPanel.setLayout(new BorderLayout());
                                    setContentPane(fullWaitPanel);
                                    revalidate();
                                    repaint();
                                }
                            } else {
                                // ★★★ [수정] ResultUI 생성자에 소켓과 스트림 전달
                                new ResultUI(userName, false, mostVoted + "님이 억울하게 투표되었습니다!\n실제 라이어는 다른 플레이어였습니다.\n", roomId, serverIp, serverPort, socket, dis, dos);
                                dispose();
                            }
                        });

                        if (isLiar && userName.equals(mostVoted)) break;
                        if (!isLiar) break;
                    }
                    else if (msg.startsWith("/finalResult ")) {
                        String[] parts = msg.substring(13).split("\\|", 3);
                        boolean citizenWin = parts[0].equals("CITIZEN");
                        String message = parts[2];

                        SwingUtilities.invokeLater(() -> {
                            // ★★★ [수정] ResultUI 생성자에 소켓과 스트림 전달
                            new ResultUI(userName, citizenWin, message, roomId, serverIp, serverPort, socket, dis, dos);
                            dispose();
                        });
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
