package com.guessinggame.gui;

import com.guessinggame.util.SoundPlayer;
import com.guessinggame.util.UIConstants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A high-impact, 4-screen cinematic onboarding flow.
 * Uses a 60FPS animation loop to provide smooth transitions and modern visuals.
 */
public class IntroSequencePanel extends JPanel {

    private enum State { COMIC, RULES, READY, LAUNCH }
    private State currentState = State.COMIC;
    private final MainFrame frame;
    private final Timer animTimer;
    private long startTime;
    private double phase = 0;

    // Animation Assets
    private final List<Particle> particles = new ArrayList<>();
    private final Random rnd = new Random();
    private float pulseScale = 1.0f;
    private final JButton btnLetsGo;

    public IntroSequencePanel(MainFrame frame) {
        this.frame = frame;
        this.setOpaque(true);
        this.setBackground(Color.BLACK);
        this.setLayout(null); // Manual positioning for high-impact effects

        // Screen 3 "LET'S GO" Button - Hidden initially
        btnLetsGo = new JButton("LET'S GO!");
        btnLetsGo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        btnLetsGo.setBackground(new Color(255, 215, 0)); // Gold
        btnLetsGo.setForeground(new Color(50, 30, 0));
        btnLetsGo.setFocusPainted(false);
        btnLetsGo.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
        btnLetsGo.setBounds(280, 450, 200, 60);
        btnLetsGo.setVisible(false);
        btnLetsGo.addActionListener(e -> transitionToLaunch());
        this.add(btnLetsGo);

        startTime = System.currentTimeMillis();
        
        // Main Animation Loop (~60 FPS)
        animTimer = new Timer(16, e -> {
            updateAnimation();
            repaint();
        });
        
        startSequence();
    }

    private void startSequence() {
        animTimer.start();
        SoundPlayer.play("intro_narration.wav"); // Audio: "In a world of numbers..."
    }

    private void updateAnimation() {
        long elapsed = System.currentTimeMillis() - startTime;
        phase = elapsed / 1000.0;

        // State Transitions based on time
        if (currentState == State.COMIC && phase > 5.0) {
            currentState = State.RULES;
            SoundPlayer.play("rules_voiceover.wav"); // Audio: "You have limited guesses..."
        } else if (currentState == State.RULES && phase > 12.0) {
            currentState = State.READY;
            btnLetsGo.setVisible(true);
            SoundPlayer.play("ready_voiceover.wav"); // Audio: "Are... you... ready?"
        }

        // Update Particles
        if (rnd.nextInt(5) == 0) particles.add(new Particle(getWidth(), getHeight(), currentState == State.LAUNCH));
        particles.removeIf(p -> !p.update());

        // Pulse logic for Screen 3
        if (currentState == State.READY) {
            pulseScale = 1.0f + (float)Math.sin(phase * 4) * 0.05f;
        }
    }

    private void transitionToLaunch() {
        currentState = State.LAUNCH;
        btnLetsGo.setVisible(false);
        SoundPlayer.play("launch_voiceover.wav"); // Audio: "Trust your instincts..."
        // The game launch hook is triggered after the voiceover length (approx 4s)
        new Timer(4000, e -> {
            animTimer.stop();
            frame.completeIntro(); 
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);

        switch (currentState) {
            case COMIC -> drawComicScreen(g2);
            case RULES -> drawRulesScreen(g2);
            case READY -> drawReadyScreen(g2);
            case LAUNCH -> drawLaunchScreen(g2);
        }
    }

    private void drawBackground(Graphics2D g2) {
        // Cinematic Gradient Background
        Paint gp = new RadialGradientPaint(getWidth()/2, getHeight()/2, getWidth(), 
            new float[]{0f, 1f}, new Color[]{new Color(40, 20, 80), Color.BLACK});
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw Particles
        for (Particle p : particles) p.draw(g2);
    }

    private void drawComicScreen(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Impact", Font.ITALIC, 48));
        double slide = Math.min(1.0, phase * 2);
        
        // Panel 1
        drawComicPanel(g2, "IN A WORLD OF NUMBERS...", 50, 100, slide);
        if (phase > 2) drawComicPanel(g2, "ONE DIGIT HOLDS THE SECRET.", 300, 250, (phase-2)*2);
    }

    private void drawComicPanel(Graphics2D g2, String text, int x, int y, double progress) {
        float alpha = (float) Math.max(0.0, Math.min(1.0, progress));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(x, y, 400, 100);
        g2.drawString(text, x + 20, y + 65);
    }

    private void drawRulesScreen(Graphics2D g2) {
        g2.setFont(UIConstants.FONT_TITLE);
        g2.setColor(UIConstants.SECONDARY);
        g2.drawString("THE CHALLENGE", 50, 80);

        String[] rules = {"🎯 Find the secret number", "⏱️ 10 seconds per guess", "🔼🔽 Higher / Lower hints", "🏆 Stats saved to profile"};
        for (int i = 0; i < rules.length; i++) {
            float alpha = (float) Math.max(0.0, Math.min(1.0, (phase - 5.5) - (i * 0.8)));
            if (alpha > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(50, 120 + (i * 70), 500, 50, 15, 15);
                g2.setColor(UIConstants.PRIMARY);
                g2.drawString(rules[i], 70, 155 + (i * 70));
            }
        }
    }

    private void drawReadyScreen(Graphics2D g2) {
        g2.setComposite(AlphaComposite.SrcOver);
        AffineTransform old = g2.getTransform();
        g2.translate(getWidth()/2, getHeight()/2 - 50);
        g2.scale(pulseScale, pulseScale);
        
        g2.setFont(new Font("Impact", Font.PLAIN, 80));
        g2.setColor(Color.WHITE);
        String t = "ARE YOU READY?";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(t, -fm.stringWidth(t)/2, 0);
        
        g2.setTransform(old);
    }

    private void drawLaunchScreen(Graphics2D g2) {
        // Screen flashes and particles go wild
        float alpha = (float)Math.max(0, 1.0 - (phase % 1.0));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
        String t = "GOOD LUCK!";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(t, (getWidth()-fm.stringWidth(t))/2, getHeight()/2);
    }

    // Simple particle system for speed lines and bursts
    private class Particle {
        double x, y, vx, vy;
        int life = 100;
        Color color;

        Particle(int w, int h, boolean burst) {
            x = w / 2.0; y = h / 2.0;
            double angle = rnd.nextDouble() * Math.PI * 2;
            double speed = burst ? rnd.nextDouble() * 15 + 5 : rnd.nextDouble() * 5 + 2;
            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed;
            color = burst ? Color.YELLOW : new Color(200, 180, 255, 150);
        }

        boolean update() {
            x += vx; y += vy; life--;
            return life > 0;
        }

        void draw(Graphics2D g2) {
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(x, y, 3, 3));
        }
    }
}