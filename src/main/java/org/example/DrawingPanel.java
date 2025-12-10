package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * 그림 그리기 기능을 제공하는 패널 클래스
 * 마우스 드래그로 그림을 그리고, 서버로 그림 데이터를 전송
 */
public class DrawingPanel extends JPanel {
    private Image screenImage;
    private Graphics2D screenGraphic;
    private int prevX, prevY;
    private boolean isEnabled = true;
    private DrawingCallback callback;

    public interface DrawingCallback {
        void sendProtocol(String msg);
        Color getCurrentColor();
        int getStrokeWidth();
        Color getDrawingBgColor();
    }

    public DrawingPanel(DrawingCallback callback) {
        this.callback = callback;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 500));

        MyMouseListener mm = new MyMouseListener();
        addMouseListener(mm);
        addMouseMotionListener(mm);
    }

    /**
     * 그리기 활성화/비활성화 설정
     * 비활성화 시 커서를 기본 커서로 변경하여 그리기 불가능함을 표시
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (screenImage != null) {
            g.drawImage(screenImage, 0, 0, null);
        }
    }

    /**
     * 이미지 버퍼를 확인하고 초기화
     * 버퍼가 없으면 새로 생성하고 흰색 배경으로 채움
     */
    public void checkImageBuffer() {
        if (screenImage == null) {
            screenImage = createImage(getWidth(), getHeight());
            screenGraphic = (Graphics2D) screenImage.getGraphics();
            screenGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            screenGraphic.setColor(Color.WHITE);
            screenGraphic.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * 서버로부터 받은 그림 명령어를 파싱하여 화면에 그리기
     * 명령어 형식: "/draw x1 y1 x2 y2 r g b width"
     * @param command 그림 명령어 문자열
     */
    public void processDrawCommand(String command) {
        checkImageBuffer();
        try {
            String[] parts = command.split(" ");
            int x1 = Integer.parseInt(parts[1]);
            int y1 = Integer.parseInt(parts[2]);
            int x2 = Integer.parseInt(parts[3]);
            int y2 = Integer.parseInt(parts[4]);
            int r = Integer.parseInt(parts[5]);
            int g = Integer.parseInt(parts[6]);
            int b = Integer.parseInt(parts[7]);
            int width = Integer.parseInt(parts[8]);

            screenGraphic.setColor(new Color(r, g, b));
            screenGraphic.setStroke(new BasicStroke(width));
            screenGraphic.drawLine(x1, y1, x2, y2);
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 캔버스를 흰색으로 초기화 (전체 지우기)
     */
    public void clear() {
        checkImageBuffer();
        screenGraphic.setColor(Color.WHITE);
        screenGraphic.fillRect(0, 0, getWidth(), getHeight());
        repaint();
    }

    /**
     * 마우스 이벤트를 처리하는 내부 클래스
     * 드래그 시작 위치를 기록하고 드래그 중 선을 그림
     */
    class MyMouseListener extends MouseAdapter {
        /**
         * 마우스 클릭 시 시작 좌표를 저장
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (!isEnabled) {
                return;
            }

            checkImageBuffer();
            prevX = e.getX();
            prevY = e.getY();
        }

        /**
         * 마우스 드래그 시 이전 좌표부터 현재 좌표까지 선을 그리고 서버로 전송
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            if (!isEnabled || callback == null) {
                return;
            }

            checkImageBuffer();
            int x = e.getX();
            int y = e.getY();

            Color currentColor = callback.getCurrentColor();
            int strokeWidth = callback.getStrokeWidth();

            screenGraphic.setColor(currentColor);
            screenGraphic.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            screenGraphic.drawLine(prevX, prevY, x, y);

            String drawCommand = String.format("/draw %d %d %d %d %d %d %d %d",
                    prevX, prevY, x, y,
                    currentColor.getRed(),
                    currentColor.getGreen(),
                    currentColor.getBlue(),
                    strokeWidth);

            callback.sendProtocol(drawCommand);

            prevX = x;
            prevY = y;
            repaint();
        }
    }
}
