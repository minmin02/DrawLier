    package org.example;

    import javax.swing.*;
    import java.awt.*;
    import java.awt.image.BufferedImage;
    import javax.imageio.ImageIO;
    import java.io.IOException;

    /**
     * 배경 이미지를 표시하는 커스텀 패널
     */
    public class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public BackgroundPanel(String imagePath) {
            try {
                // 리소스에서 이미지 로드
                backgroundImage = new ImageIcon(getClass().getResource("/" + imagePath)).getImage();
            } catch (Exception e) {
                System.err.println("배경 이미지 로드 실패: " + imagePath);
                // 이미지 로드 실패 시 그라데이션 배경 사용
                backgroundImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (backgroundImage != null) {
                // 이미지를 패널 크기에 맞게 그리기
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                // 기본 그라데이션 배경
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(66, 133, 244),
                        0, getHeight(), new Color(13, 71, 161)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }