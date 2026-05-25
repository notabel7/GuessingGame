package com.guessinggame.util;

import java.awt.*;

/**
 * Central repository for all UI styling — colours, fonts, dimensions.
 * Deep-purple / amber gaming theme, distinct from the Conference Center palette.
 */
public final class UIConstants {

    private UIConstants() {}

    // ── Colours ───────────────────────────────────────────────────────────
    public static final Color PRIMARY      = new Color( 74,  20, 140); // deep purple
    public static final Color PRIMARY_DARK = new Color( 49,  27,  77); // darker purple (stats bar)
    public static final Color SECONDARY    = new Color(245, 127,  23); // amber
    public static final Color BACKGROUND   = new Color(237, 231, 246); // light lavender
    public static final Color SUCCESS      = new Color(  0, 105,  92); // dark teal  (win)
    public static final Color WARNING      = new Color(191,  54,  12); // deep orange (low guesses)
    public static final Color DANGER       = new Color(183,  28,  28); // red        (last guess / loss)
    public static final Color CARD_HOVER   = new Color(243, 229, 255); // hover highlight
    public static final Color BORDER_COLOR = new Color(206, 147, 216); // light purple border
    public static final Color INPUT_BG     = new Color(253, 246, 255); // near-white with purple tint
    public static final Color HINT_FG      = new Color( 55,  10, 100); // dark purple for hint text

    // ── Fonts ─────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  26);
    public static final Font FONT_H2     = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_HINT   = new Font("Segoe UI", Font.BOLD,  15);
    public static final Font FONT_AWARD  = new Font("Segoe UI", Font.BOLD,  22);

    // ── Sizes ─────────────────────────────────────────────────────────────
    public static final int FIELD_H    = 36;
    public static final int ROW_HEIGHT = 26;

    // ── DPI scale detection ───────────────────────────────────────────────
    private static final double SCALE = detectScale();

    private static double detectScale() {
        try {
            java.awt.geom.AffineTransform tx = java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getDefaultTransform();
            return Math.max(1.0, tx.getScaleX());
        } catch (Throwable t) { return 1.0; }
    }

    // ── HiDPI-aware icon wrapper ──────────────────────────────────────────
    private static final class HiDpiIcon extends javax.swing.ImageIcon {
        private final int lw, lh;

        HiDpiIcon(java.awt.image.BufferedImage img, int lw, int lh) {
            super(img);
            this.lw = lw; this.lh = lh;
        }

        @Override public int getIconWidth()  { return lw; }
        @Override public int getIconHeight() { return lh; }

        @Override
        public synchronized void paintIcon(java.awt.Component c,
                                           java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(getImage(), x, y, lw, lh, null);
            g2.dispose();
        }
    }

    // ── Icon loaders ──────────────────────────────────────────────────────

    public static javax.swing.ImageIcon loadIcon(String name) {
        return loadIcon(name, 18, 18);
    }

    public static javax.swing.ImageIcon loadIcon(String name, int w, int h) {
        int pw = (int) Math.ceil(w * SCALE);
        int ph = (int) Math.ceil(h * SCALE);

        String svgName = name.replaceAll("\\.[^.]+$", "") + ".svg";
        java.net.URL svgUrl = UIConstants.class.getResource("/icons/" + svgName);
        if (svgUrl != null) {
            java.awt.image.BufferedImage img = renderSVG(svgUrl, pw, ph);
            if (img != null) return new HiDpiIcon(img, w, h);
        }
        try {
            java.net.URL url = UIConstants.class.getResource("/icons/" + name);
            if (url != null) {
                java.awt.image.BufferedImage bi = scalePNG(url, pw, ph);
                if (bi != null) return new HiDpiIcon(bi, w, h);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Loads an icon and tints every visible pixel to the given colour,
     * preserving per-pixel alpha. Used for level icons on white card backgrounds.
     */
    public static javax.swing.ImageIcon loadIconTinted(String name, java.awt.Color tint) {
        int pw = (int) Math.ceil(18 * SCALE);
        int ph = (int) Math.ceil(18 * SCALE);

        java.awt.image.BufferedImage src = null;
        String svgName = name.replaceAll("\\.[^.]+$", "") + ".svg";
        java.net.URL svgUrl = UIConstants.class.getResource("/icons/" + svgName);
        if (svgUrl != null) src = renderSVG(svgUrl, pw, ph);

        if (src == null) {
            try {
                java.net.URL url = UIConstants.class.getResource("/icons/" + name);
                if (url != null) src = scalePNG(url, pw, ph);
            } catch (Exception ignored) {}
        }
        if (src == null) return null;

        int r = tint.getRed(), g = tint.getGreen(), b = tint.getBlue();
        java.awt.image.BufferedImage out =
            new java.awt.image.BufferedImage(pw, ph,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < pw; x++) {
            for (int y = 0; y < ph; y++) {
                int argb  = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) out.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return new HiDpiIcon(out, 18, 18);
    }

    public static javax.swing.ImageIcon loadIconWhite(String name) {
        int pw = (int) Math.ceil(18 * SCALE);
        int ph = (int) Math.ceil(18 * SCALE);

        java.awt.image.BufferedImage src = null;
        String svgName = name.replaceAll("\\.[^.]+$", "") + ".svg";
        java.net.URL svgUrl = UIConstants.class.getResource("/icons/" + svgName);
        if (svgUrl != null) src = renderSVG(svgUrl, pw, ph);

        if (src == null) {
            try {
                java.net.URL url = UIConstants.class.getResource("/icons/" + name);
                if (url != null) src = scalePNG(url, pw, ph);
            } catch (Exception ignored) {}
        }
        if (src == null) return null;

        java.awt.image.BufferedImage out =
            new java.awt.image.BufferedImage(pw, ph,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < pw; x++) {
            for (int y = 0; y < ph; y++) {
                int argb  = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) out.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        return new HiDpiIcon(out, 18, 18);
    }

    // ── Private rendering helpers ─────────────────────────────────────────

    private static java.awt.image.BufferedImage renderSVG(
            java.net.URL url, int pw, int ph) {
        try {
            com.kitfox.svg.app.beans.SVGIcon icon =
                new com.kitfox.svg.app.beans.SVGIcon();
            icon.setSvgURI(url.toURI());
            icon.setScaleToFit(true);
            icon.setAntiAlias(true);
            icon.setPreferredSize(new java.awt.Dimension(pw, ph));

            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(pw, ph,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL,
                java.awt.RenderingHints.VALUE_STROKE_PURE);
            icon.paintIcon(null, g2, 0, 0);
            g2.dispose();
            return img;
        } catch (Throwable t) { return null; }
    }

    private static java.awt.image.BufferedImage scalePNG(
            java.net.URL url, int pw, int ph) {
        try {
            javax.swing.ImageIcon raw = new javax.swing.ImageIcon(url);
            java.awt.image.BufferedImage bi =
                new java.awt.image.BufferedImage(pw, ph,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(
                raw.getImage().getScaledInstance(pw, ph, java.awt.Image.SCALE_SMOOTH),
                0, 0, null);
            g2.dispose();
            return bi;
        } catch (Exception e) { return null; }
    }
}
