package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

public class UIUtils {

    /**
     * JButton에 이미지 기반의 호버, 클릭 효과를 적용.
     * @param button 효과를 적용할 JButton 객체
     */
    public static void applyButtonEffects(JButton button) {
        if (button.getIcon() == null) {
            // 아이콘이 없는 버튼(텍스트 버튼)에는 효과를 적용하지 않음
            return;
        }

        ImageIcon originalIcon = (ImageIcon) button.getIcon();
        Image originalImage = originalIcon.getImage();

        // 원본 이미지를 BufferedImage로 변환하여 이미지 처리를 준비
        BufferedImage bufferedImage = new BufferedImage(
                originalImage.getWidth(null),
                originalImage.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(originalImage, 0, 0, null);
        g2.dispose();

        // 1. 호버 효과 : 이미지를 약간 밝게
        // RescaleOp 필터를 사용하여 이미지의 각 픽셀 밝기를 1.2배 증가.
        RescaleOp hoverFilter = new RescaleOp(1.2f, 0, null);
        BufferedImage hoverImage = hoverFilter.filter(bufferedImage, null);
        button.setRolloverIcon(new ImageIcon(hoverImage));

        // 2.클릭 효과 : 이미지를 약간 어둡게
        // RescaleOp 필터를 사용하여 이미지의 각 픽셀 밝기를 0.8배 감소.
        RescaleOp pressFilter = new RescaleOp(0.8f, 0, null);
        BufferedImage pressImage = pressFilter.filter(bufferedImage, null);
        button.setPressedIcon(new ImageIcon(pressImage));

        // 3.버튼의 기본 스타일을 제거하여 이미지만 보이도록
        button.setBorderPainted(false);       // 테두리 제거
        button.setContentAreaFilled(false);   // 내용 영역 채우기 비활성화
        button.setFocusPainted(false);        // 포커스 테두리 제거
        button.setOpaque(false);              // 배경 투명화
        button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 마우스 커서를 손가락 모양으로 변경
    }
}
