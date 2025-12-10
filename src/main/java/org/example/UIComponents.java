package org.example;

import java.awt.*;
import javax.swing.*;

//UI 컴포넌트들을 모아놓은 클래스
public class UIComponents {

    //둥근 모서리를 가진 버튼
    public static class RoundedButton extends JButton {
        private static final int RADIUS = 25; //버튼의 둥근 모서리 반지름

        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false); //내용 영역 채우기 비활성화
            setFocusPainted(false); //포커스 테두리 제거
            setBorderPainted(false); //테두리 제거
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //안티 앨리어싱 설정

            //버튼 상태에 따라 색상 변경
            if (!isEnabled()) {
                g2.setColor(getBackground()); //비활성화 상태
            } else if (getModel().isPressed()) {
                g2.setColor(getBackground().darker()); //눌렸을 때
            } else if (getModel().isRollover()) {
                g2.setColor(getBackground().brighter()); //마우스를 올렸을 때
            } else {
                g2.setColor(getBackground()); //기본 상태
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS); //둥근 사각형 채우기
            g2.dispose();

            super.paintComponent(g); //텍스트 등 나머지 부분 그리기
        }
    }

    //둥근 모서리를 가진 텍스트 필드
    public static class RoundedTextField extends JTextField {
        private int radius; //텍스트 필드의 둥근 모서리 반지름

        public RoundedTextField(int radius) {
            super();
            this.radius = radius;
            setOpaque(false); //배경 투명화
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //안티 앨리어싱 설정
            g2.setColor(getBackground()); //배경색 설정
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius); //둥근 사각형 채우기
            g2.setColor(new Color(220, 220, 220)); //테두리 색상 설정
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius); //둥근 테두리 그리기
            g2.dispose();
            super.paintComponent(g); //텍스트 등 나머지 부분 그리기
        }
    }

    //말풍선 모양의 패널
    public static class RoundedBubblePanel extends JPanel {
        private Color backgroundColor; //배경색
        private boolean isRight; //오른쪽에 표시될지 여부
        private static final int RADIUS = 20; //둥근 모서리 반지름
        private static final int TAIL_SIZE = 18; //말풍선 꼬리 크기

        public RoundedBubblePanel(Color backgroundColor, boolean isRight) {
            this.backgroundColor = backgroundColor;
            this.isRight = isRight;
            setOpaque(false); //배경 투명화
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //안티 앨리어싱 설정

            int width = getWidth();
            int height = getHeight();

            g2.setColor(backgroundColor); //배경색 설정

            if (isRight) { //오른쪽 말풍선
                g2.fillRoundRect(0, 0, width - TAIL_SIZE, height, RADIUS, RADIUS); //몸통 그리기
                int[] xPoints = {width - TAIL_SIZE, width - 2, width - TAIL_SIZE}; //꼬리 x좌표
                int[] yPoints = {height - 25, height - 3, height - 8}; //꼬리 y좌표
                g2.fillPolygon(xPoints, yPoints, 3); //꼬리 그리기
            } else { //왼쪽 말풍선
                g2.fillRoundRect(TAIL_SIZE, 0, width - TAIL_SIZE, height, RADIUS, RADIUS); //몸통 그리기
                int[] xPoints = {TAIL_SIZE, 2, TAIL_SIZE}; //꼬리 x좌표
                int[] yPoints = {height - 25, height - 3, height - 8}; //꼬리 y좌표
                g2.fillPolygon(xPoints, yPoints, 3); //꼬리 그리기
            }

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width += TAIL_SIZE; //꼬리 크기만큼 너비 추가
            return size;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension size = super.getMaximumSize();
            size.width = Math.min(size.width, 140); //최대 너비 제한
            return size;
        }
    }

    //둥근 모서리와 그림자 효과가 있는 테두리
    public static class RoundedBorder implements javax.swing.border.Border {
        private int radius; //둥근 모서리 반지름
        private Color backgroundColor; //배경색

        public RoundedBorder(int radius, Color backgroundColor) {
            this.radius = radius;
            this.backgroundColor = backgroundColor;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius/2 + 2, this.radius/2 + 2, this.radius/2 + 4, this.radius/2 + 4);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //안티 앨리어싱 설정
            g2.setColor(new Color(0, 0, 0, 30)); //그림자 색상
            g2.fillRoundRect(x + 2, y + 2, width - 3, height - 3, radius, radius); //그림자 그리기
            g2.setColor(backgroundColor); //배경색 설정
            g2.fillRoundRect(x, y, width - 4, height - 4, radius, radius); //배경 그리기
            g2.dispose();
        }
    }

    //말풍선 모양의 테두리
    public static class SpeechBubbleBorder implements javax.swing.border.Border {
        private int radius; //둥근 모서리 반지름
        private Color backgroundColor; //배경색
        private boolean isRight; //오른쪽에 표시될지 여부
        private static final int TAIL_SIZE = 12; //말풍선 꼬리 크기

        public SpeechBubbleBorder(int radius, Color backgroundColor, boolean isRight) {
            this.radius = radius;
            this.backgroundColor = backgroundColor;
            this.isRight = isRight;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            if (isRight) {
                return new Insets(radius/2, radius/2, radius/2, radius/2 + TAIL_SIZE);
            } else {
                return new Insets(radius/2, radius/2 + TAIL_SIZE, radius/2, radius/2);
            }
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //안티 앨리어싱 설정

            g2.setColor(backgroundColor); //배경색 설정

            if (isRight) { //오른쪽 말풍선
                g2.fillRoundRect(x, y, width - TAIL_SIZE, height, radius, radius); //몸통 그리기
                int[] xPoints = {width - TAIL_SIZE, width - 2, width - TAIL_SIZE}; //꼬리 x좌표
                int[] yPoints = {height - 20, height - 5, height - 10}; //꼬리 y좌표
                g2.fillPolygon(xPoints, yPoints, 3); //꼬리 그리기
            } else { //왼쪽 말풍선
                g2.fillRoundRect(x + TAIL_SIZE, y, width - TAIL_SIZE, height, radius, radius); //몸통 그리기
                int[] xPoints = {TAIL_SIZE, 2, TAIL_SIZE}; //꼬리 x좌표
                int[] yPoints = {height - 20, height - 5, height - 10}; //꼬리 y좌표
                g2.fillPolygon(xPoints, yPoints, 3); //꼬리 그리기
            }

            g2.dispose();
        }
    }

    //둥근 모서리를 가진 입력창 테두리
    public static class RoundedInputBorder implements javax.swing.border.Border {
        private int radius; //둥근 모서리 반지름
        private Color borderColor; //테두리 색상

        public RoundedInputBorder(int radius, Color borderColor) {
            this.radius = radius;
            this.borderColor = borderColor;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //안티 앨리어싱 설정
            g2.setColor(new Color(250, 250, 250)); //배경색 설정
            g2.fillRoundRect(x, y, width - 1, height - 1, radius, radius); //배경 그리기
            g2.setColor(borderColor); //테두리 색상 설정
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius); //테두리 그리기
            g2.dispose();
        }
    }
}
