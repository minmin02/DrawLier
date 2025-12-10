package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * 배경 이미지를 표시하는 커스텀 JPanel 클래스.
 * - 생성자에서 이미지 경로를 받아 배경으로 설정.
 * - 이미지 로드 실패 시 기본 그라데이션 배경을 표시.
 */
public class BackgroundPanel extends JPanel {
    //필드
    private Image backgroundImage; //배경으로 사용될 이미지 객체

    //생성자
    public BackgroundPanel(String imagePath) {
        try {
            //리소스 폴더에서 이미지 파일을 로드
            backgroundImage = new ImageIcon(getClass().getResource("/" + imagePath)).getImage();
        } catch (Exception e) {
            System.err.println("배경 이미지 로드 실패: " + imagePath);
            //이미지 로드에 실패하면 backgroundImage를 null로 설정
            backgroundImage = null;
        }
    }

    /**
     * 패널의 배경을 그리는 메소드.
     * Swing에 의해 자동으로 호출됨.
     * @param g Graphics 객체
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //패널의 기본 그리기 작업 수행

        //backgroundImage가 성공적으로 로드되었는지 확인
        if (backgroundImage != null) {
            //이미지를 패널의 전체 크기에 맞게 채워서 그림
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            //이미지가 없을 경우, 대체용 그라데이션 배경을 그림
            Graphics2D g2d = (Graphics2D) g;
            //파란색 계열의 그라데이션 설정 (위에서 아래로)
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(66, 133, 244),
                    0, getHeight(), new Color(13, 71, 161)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
