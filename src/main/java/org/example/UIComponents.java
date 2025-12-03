package org.example;

import java.awt.*;
import javax.swing.*;

public class UIComponents {

    public static class RoundedButton extends JButton {
        private static final int RADIUS = 25;

        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(getBackground());
            } else if (getModel().isPressed()) {
                g2.setColor(getBackground().darker());
            } else if (getModel().isRollover()) {
                g2.setColor(getBackground().brighter());
            } else {
                g2.setColor(getBackground());
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    public static class RoundedTextField extends JTextField {
        private int radius;

        public RoundedTextField(int radius) {
            super();
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(new Color(220, 220, 220));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class RoundedBubblePanel extends JPanel {
        private Color backgroundColor;
        private boolean isRight;
        private static final int RADIUS = 20;
        private static final int TAIL_SIZE = 18;

        public RoundedBubblePanel(Color backgroundColor, boolean isRight) {
            this.backgroundColor = backgroundColor;
            this.isRight = isRight;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setColor(backgroundColor);

            if (isRight) {
                g2.fillRoundRect(0, 0, width - TAIL_SIZE, height, RADIUS, RADIUS);
                int[] xPoints = {width - TAIL_SIZE, width - 2, width - TAIL_SIZE};
                int[] yPoints = {height - 25, height - 3, height - 8};
                g2.fillPolygon(xPoints, yPoints, 3);
            } else {
                g2.fillRoundRect(TAIL_SIZE, 0, width - TAIL_SIZE, height, RADIUS, RADIUS);
                int[] xPoints = {TAIL_SIZE, 2, TAIL_SIZE};
                int[] yPoints = {height - 25, height - 3, height - 8};
                g2.fillPolygon(xPoints, yPoints, 3);
            }

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width += TAIL_SIZE;
            return size;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension size = super.getMaximumSize();
            size.width = Math.min(size.width, 140);
            return size;
        }
    }

    public static class RoundedBorder implements javax.swing.border.Border {
        private int radius;
        private Color backgroundColor;

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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(x + 2, y + 2, width - 3, height - 3, radius, radius);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(x, y, width - 4, height - 4, radius, radius);
            g2.dispose();
        }
    }

    public static class SpeechBubbleBorder implements javax.swing.border.Border {
        private int radius;
        private Color backgroundColor;
        private boolean isRight;
        private static final int TAIL_SIZE = 12;

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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(backgroundColor);

            if (isRight) {
                g2.fillRoundRect(x, y, width - TAIL_SIZE, height, radius, radius);
                int[] xPoints = {width - TAIL_SIZE, width - 2, width - TAIL_SIZE};
                int[] yPoints = {height - 20, height - 5, height - 10};
                g2.fillPolygon(xPoints, yPoints, 3);
            } else {
                g2.fillRoundRect(x + TAIL_SIZE, y, width - TAIL_SIZE, height, radius, radius);
                int[] xPoints = {TAIL_SIZE, 2, TAIL_SIZE};
                int[] yPoints = {height - 20, height - 5, height - 10};
                g2.fillPolygon(xPoints, yPoints, 3);
            }

            g2.dispose();
        }
    }

    public static class RoundedInputBorder implements javax.swing.border.Border {
        private int radius;
        private Color borderColor;

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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(250, 250, 250));
            g2.fillRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
    }
}
