package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * 투표 UI - 수정됨
 * 1. 상단 제목 패널 제거
 * 2. 배경화면을 /PlayUI/Voting.png 이미지로 변경
 */
public class VotingUI extends JFrame {
    private String userName;
    private List<String> players;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String roomId;
    private String serverIp;
    private String serverPort;

    // lblTitle 제거됨
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

        // ★★★ [수정] 배경 이미지를 그리는 패널로 교체 ★★★
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
                    // 이미지가 없을 경우 기본 배경색
                    g.setColor(new Color(245, 245, 250));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        // 이미지에 맞춰 여백 조정 (필요 시 숫자 조절 가능)
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPane.setLayout(new BorderLayout(10, 10));
        setContentPane(contentPane);

        // ★★★ [삭제] 상단 제목 패널(topPanel) 제거함 ★★★
        /*
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        lblTitle = new JLabel("누가 라이어일까요?");
        // ... (생략) ...
        contentPane.add(topPanel, BorderLayout.NORTH);
        */

        // 중앙 투표 패널
        votingPanel = new JPanel();
        votingPanel.setLayout(new GridLayout(2, 2, 20, 20));
        votingPanel.setOpaque(false); // 배경 투명화

        // 상단 타이틀이 없어졌으므로, 이미지를 가리지 않도록 상단 여백을 좀 더 줄 수도 있습니다.
        // 현재는 기존 값 유지 (30, 50, 30, 50)
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

            // 플레이어 아이콘
            JLabel iconLabel = new JLabel("👤", SwingConstants.CENTER);
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
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
        bottomPanel.setOpaque(false); // 배경 투명화
        bottomPanel.setBorder(new EmptyBorder(10, 0, 20, 0)); // 하단 여백 조정

        JButton btnConfirm = new JButton();
        int btnWidth = 100;
        int btnHeight = 30;
        btnConfirm.setPreferredSize(new Dimension(btnWidth, btnHeight));

        try {
            URL btnUrl = getClass().getResource("/PlayUI/VoteButton.png");
            if (btnUrl != null) {
                ImageIcon icon = new ImageIcon(btnUrl);
                Image img = icon.getImage().getScaledInstance(btnWidth, btnHeight, Image.SCALE_SMOOTH);
                btnConfirm.setIcon(new ImageIcon(img));
                applyButtonEffects(btnConfirm); //버튼 효과 적용
            } else {
                btnConfirm.setText("투표하기");
                System.err.println("이미지를 찾을 수 없습니다: /PlayUI/VoteButton.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnConfirm.setText("투표하기");
        }

        // 버튼 스타일 투명화
        btnConfirm.setBorderPainted(false);
        btnConfirm.setContentAreaFilled(false);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setOpaque(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
    // 버튼에 호버(밝게) 및 클릭(어둡게) 효과를 자동으로 적용하는 메서드
    private void applyButtonEffects(JButton button) {
        if (button.getIcon() == null) return;

        ImageIcon originalIcon = (ImageIcon) button.getIcon();
        Image originalImage = originalIcon.getImage();

        //BufferedImage로 변환
        int w = originalImage.getWidth(null);
        int h = originalImage.getHeight(null);
        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(originalImage, 0, 0, null);
        g2.dispose();

        //호버 효과 (밝게: 1.2배)
        java.awt.image.RescaleOp hoverFilter = new java.awt.image.RescaleOp(1.2f, 0, null);
        java.awt.image.BufferedImage hoverImage = hoverFilter.filter(bufferedImage, null);
        button.setRolloverIcon(new ImageIcon(hoverImage));

        //클릭 효과 (어둡게: 0.8배)
        java.awt.image.RescaleOp pressFilter = new java.awt.image.RescaleOp(0.8f, 0, null);
        java.awt.image.BufferedImage pressImage = pressFilter.filter(bufferedImage, null);
        button.setPressedIcon(new ImageIcon(pressImage));

        //기본 설정 강제 (투명화 등)
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
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
                        String[] parts = msg.substring(12).split("\\|");
                        String mostVoted = parts[0];
                        boolean isLiar = parts[1].equals("true");

                        SwingUtilities.invokeLater(() -> {
                            if (isLiar) {
                                // 라이어 적발 시
                                if (userName.equals(mostVoted)) {
                                    // 본인이 라이어 -> 정답 입력 UI로 이동 (창 닫음)
                                    new InsertAnswerUI(userName, dos, dis, roomId, serverIp, serverPort);
                                    dispose();
                                } else {
                                    //완전히 새로운 패널 생성 (여백 없음)
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
                                                // 창 크기에 맞춰 이미지 꽉 차게 그리기
                                                g.drawImage(waitImage, 0, 0, getWidth(), getHeight(), this);
                                            } else {
                                                // 이미지 로드 실패 시 검은 배경
                                                g.setColor(Color.BLACK);
                                                g.fillRect(0, 0, getWidth(), getHeight());
                                            }
                                        }
                                    };
                                    fullWaitPanel.setLayout(new BorderLayout());

                                    //기존 contentPane을 새 패널로 통째로 교체
                                    setContentPane(fullWaitPanel);

                                    //화면 갱신
                                    revalidate();
                                    repaint();
                                }
                            } else {
                                // 시민이 억울하게 지목됨 -> 결과창 이동 (창 닫음)
                                new ResultUI(userName, false, mostVoted + "님이 억울하게 투표되었습니다!\n실제 라이어는 다른 플레이어였습니다.\n", dos, roomId, serverIp, serverPort);
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
                            new ResultUI(userName, citizenWin, message, dos, roomId, serverIp, serverPort);
                            dispose();
                        });
                        break; // 게임 종료
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}