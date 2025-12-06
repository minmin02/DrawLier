package org.example;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

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

    public void checkImageBuffer() {
        if (screenImage == null) {
            screenImage = createImage(getWidth(), getHeight());
            screenGraphic = (Graphics2D) screenImage.getGraphics();
            screenGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            screenGraphic.setColor(Color.WHITE);
            screenGraphic.fillRect(0, 0, getWidth(), getHeight());
        }
    }

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

    public void clear() {
        checkImageBuffer();
        screenGraphic.setColor(Color.WHITE);
        screenGraphic.fillRect(0, 0, getWidth(), getHeight());
        repaint();
    }

    class MyMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (!isEnabled) {
                return;
            }

            checkImageBuffer();
            prevX = e.getX();
            prevY = e.getY();
        }

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
