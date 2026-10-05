import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class ZombieShooter extends JPanel implements ActionListener, KeyListener, MouseMotionListener, MouseListener {

    static final int WIDTH = 1000;
    static final int HEIGHT = 700;

    Timer timer = new Timer(16, this);
    Random random = new Random();

    Player player;
    ArrayList<Enemy> enemies = new ArrayList<>();
    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Particle> particles = new ArrayList<>();

    boolean up, down, left, right, shooting;
    int mouseX = WIDTH / 2, mouseY = HEIGHT / 2;

    int score = 0;
    int wave = 1;
    int enemiesToSpawn = 8;
    int spawned = 0;
    int spawnTimer = 0;
    int kills = 0;
    boolean gameOver = false;

    public ZombieShooter() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(12, 14, 18));
        setFocusable(true);

        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        player = new Player(WIDTH / 2, HEIGHT / 2);
        timer.start();
    }

    void resetGame() {
        player = new Player(WIDTH / 2, HEIGHT / 2);
        enemies.clear();
        bullets.clear();
        particles.clear();
        score = 0;
        wave = 1;
        enemiesToSpawn = 8;
        spawned = 0;
        spawnTimer = 0;
        kills = 0;
        gameOver = false;
        shooting = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
        }
        repaint();
    }

    void updateGame() {
        player.update();

        // Generar enemigos
        if (spawned < enemiesToSpawn) {
            spawnTimer--;
            if (spawnTimer <= 0) {
                spawnEnemy();
                spawned++;
                spawnTimer = Math.max(12, 45 - wave * 2);
            }
        } else if (enemies.isEmpty()) {
            wave++;
            enemiesToSpawn = 7 + wave * 3;
            spawned = 0;
            score += 100 * wave;
            player.hp = Math.min(player.maxHp, player.hp + 20);
        }

        // Disparo automático mientras se mantiene pulsado el botón izquierdo
        if (shooting) {
            player.shoot();
        }

        Iterator<Bullet> bit = bullets.iterator();
        while (bit.hasNext()) {
            Bullet b = bit.next();
            b.update();

            if (b.x < -20 || b.x > WIDTH + 20 || b.y < -20 || b.y > HEIGHT + 20) {
                bit.remove();
                continue;
            }

            boolean hit = false;
            Iterator<Enemy> eit = enemies.iterator();

            while (eit.hasNext()) {
                Enemy enemy = eit.next();

                if (distance(b.x, b.y, enemy.x, enemy.y) < enemy.radius + b.radius) {
                    enemy.hp -= b.damage;
                    hit = true;

                    createParticles(b.x, b.y, 5, new Color(255, 190, 60));

                    if (enemy.hp <= 0) {
                        score += enemy.type == 2 ? 50 : 20;
                        kills++;
                        createParticles(enemy.x, enemy.y, 18,
                                enemy.type == 2 ? new Color(190, 50, 255) : new Color(80, 220, 90));
                        eit.remove();
                    }
                    break;
                }
            }

            if (hit) {
                bit.remove();
            }
        }

        for (Enemy enemy : enemies) {
            enemy.update();

            if (distance(enemy.x, enemy.y, player.x, player.y) < enemy.radius + player.radius) {
                if (player.damageCooldown <= 0) {
                    player.hp -= enemy.type == 2 ? 18 : 10;
                    player.damageCooldown = 35;
                    createParticles(player.x, player.y, 12, new Color(255, 60, 60));

                    if (player.hp <= 0) {
                        gameOver = true;
                    }
                }
            }
        }

        if (player.damageCooldown > 0) player.damageCooldown--;

        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) {
            Particle p = pit.next();
            p.update();
            if (p.life <= 0) pit.remove();
        }
    }

    void spawnEnemy() {
        int side = random.nextInt(4);
        double x, y;

        if (side == 0) {
            x = random.nextInt(WIDTH);
            y = -30;
        } else if (side == 1) {
            x = WIDTH + 30;
            y = random.nextInt(HEIGHT);
        } else if (side == 2) {
            x = random.nextInt(WIDTH);
            y = HEIGHT + 30;
        } else {
            x = -30;
            y = random.nextInt(HEIGHT);
        }

        int type = random.nextInt(100) < Math.min(15 + wave, 40) ? 2 : 1;
        enemies.add(new Enemy(x, y, type));
    }

    void createParticles(double x, double y, int amount, Color color) {
        for (int i = 0; i < amount; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Fondo tipo arena
        drawBackground(g2);

        for (Particle p : particles) p.draw(g2);
        for (Bullet b : bullets) b.draw(g2);
        for (Enemy e : enemies) e.draw(g2);
        player.draw(g2);

        drawHUD(g2);

        if (gameOver) {
            drawGameOver(g2);
        }

        g2.dispose();
    }

    void drawBackground(Graphics2D g) {
        g.setColor(new Color(18, 21, 27));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Rejilla
        g.setColor(new Color(28, 32, 40));
        for (int x = 0; x < WIDTH; x += 40) g.drawLine(x, 0, x, HEIGHT);
        for (int y = 0; y < HEIGHT; y += 40) g.drawLine(0, y, WIDTH, y);

        // Luces decorativas
        g.setColor(new Color(30, 55, 70));
        g.fillOval(-150, -150, 400, 400);
        g.setColor(new Color(55, 30, 70));
        g.fillOval(WIDTH - 250, HEIGHT - 250, 450, 450);
    }

    void drawHUD(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.WHITE);

        g.drawString("PUNTOS: " + score, 20, 30);
        g.drawString("BAJAS: " + kills, 20, 55);
        g.drawString("OLEADA: " + wave, 20, 80);

        // Barra de vida
        int barW = 250;
        int barH = 24;
        int hpW = (int) (barW * Math.max(0, player.hp) / (double) player.maxHp);

        g.setColor(new Color(50, 50, 55));
        g.fillRoundRect(20, HEIGHT - 50, barW, barH, 12, 12);

        g.setColor(new Color(40, 210, 90));
        g.fillRoundRect(20, HEIGHT - 50, hpW, barH, 12, 12);

        g.setColor(Color.WHITE);
        g.drawRoundRect(20, HEIGHT - 50, barW, barH, 12, 12);
        g.drawString("VIDA", 280, HEIGHT - 31);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(210, 210, 215));
        g.drawString("WASD: mover   |   RATÓN: apuntar   |   CLICK IZQ.: disparar", 20, HEIGHT - 15);

        // Mira
        g.setColor(new Color(255, 255, 255, 190));
        g.drawOval(mouseX - 8, mouseY - 8, 16, 16);
        g.drawLine(mouseX - 14, mouseY, mouseX - 4, mouseY);
        g.drawLine(mouseX + 4, mouseY, mouseX + 14, mouseY);
        g.drawLine(mouseX, mouseY - 14, mouseX, mouseY - 4);
        g.drawLine(mouseX, mouseY + 4, mouseX, mouseY + 14);
    }

    void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        centerText(g, "GAME OVER", 270, new Font("Arial", Font.BOLD, 64),
                new Color(255, 70, 70));
        centerText(g, "Puntuación: " + score, 330, new Font("Arial", Font.BOLD, 28),
                Color.WHITE);
        centerText(g, "Bajas: " + kills + "   |   Oleada: " + wave, 370,
                new Font("Arial", Font.PLAIN, 20), Color.LIGHT_GRAY);
        centerText(g, "Pulsa R para volver a jugar", 440,
                new Font("Arial", Font.BOLD, 22), new Color(255, 210, 70));
    }

    // Método auxiliar para centrar texto sin depender de APIs adicionales.
    void centerText(Graphics2D g, String text, int y, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) up = true;
        if (e.getKeyCode() == KeyEvent.VK_S) down = true;
        if (e.getKeyCode() == KeyEvent.VK_A) left = true;
        if (e.getKeyCode() == KeyEvent.VK_D) right = true;

        if (e.getKeyCode() == KeyEvent.VK_R && gameOver) resetGame();
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) up = false;
        if (e.getKeyCode() == KeyEvent.VK_S) down = false;
        if (e.getKeyCode() == KeyEvent.VK_A) left = false;
        if (e.getKeyCode() == KeyEvent.VK_D) right = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    @Override public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) shooting = true;
    }

    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) shooting = false;
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    class Player {
        double x, y;
        double speed = 4.2;
        int radius = 18;
        int maxHp = 100;
        int hp = 100;
        int damageCooldown = 0;
        int shootCooldown = 0;

        Player(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            double dx = 0, dy = 0;

            if (up) dy--;
            if (down) dy++;
            if (left) dx--;
            if (right) dx++;

            if (dx != 0 || dy != 0) {
                double len = Math.hypot(dx, dy);
                x += dx / len * speed;
                y += dy / len * speed;
            }

            x = Math.max(radius, Math.min(WIDTH - radius, x));
            y = Math.max(radius, Math.min(HEIGHT - radius, y));

            if (shootCooldown > 0) shootCooldown--;
        }

        void shoot() {
            if (shootCooldown > 0) return;

            double dx = mouseX - x;
            double dy = mouseY - y;
            double len = Math.hypot(dx, dy);

            if (len == 0) return;

            dx /= len;
            dy /= len;

            bullets.add(new Bullet(x + dx * 25, y + dy * 25, dx * 11, dy * 11));
            createParticles(x + dx * 25, y + dy * 25, 3, new Color(255, 210, 80));

            shootCooldown = 7;
        }

        void draw(Graphics2D g) {
            // sombra
            g.setColor(new Color(0, 0, 0, 90));
            g.fillOval((int)x - radius + 4, (int)y - radius + 5, radius * 2, radius * 2);

            // cuerpo
            g.setColor(new Color(55, 160, 255));
            g.fillOval((int)x - radius, (int)y - radius, radius * 2, radius * 2);

            // arma
            double dx = mouseX - x;
            double dy = mouseY - y;
            double len = Math.max(1, Math.hypot(dx, dy));
            dx /= len;
            dy /= len;

            g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(40, 45, 55));
            g.drawLine((int)x, (int)y, (int)(x + dx * 34), (int)(y + dy * 34));

            g.setColor(Color.WHITE);
            g.fillOval((int)x - 5, (int)y - 5, 10, 10);

            if (damageCooldown > 0) {
                g.setColor(new Color(255, 255, 255, 100));
                g.drawOval((int)x - 25, (int)y - 25, 50, 50);
            }
        }
    }

    class Enemy {
        double x, y;
        int type;
        int radius;
        int hp;
        double speed;

        Enemy(double x, double y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;

            if (type == 2) {
                radius = 25;
                hp = 80 + wave * 8;
                speed = 1.0 + wave * 0.025;
            } else {
                radius = 17;
                hp = 30 + wave * 4;
                speed = 1.5 + wave * 0.035;
            }
        }

        void update() {
            double dx = player.x - x;
            double dy = player.y - y;
            double len = Math.max(1, Math.hypot(dx, dy));

            x += dx / len * speed;
            y += dy / len * speed;
        }

        void draw(Graphics2D g) {
            Color body = type == 2 ? new Color(180, 60, 220) : new Color(65, 205, 90);

            g.setColor(new Color(0, 0, 0, 100));
            g.fillOval((int)x - radius + 4, (int)y - radius + 5, radius * 2, radius * 2);

            g.setColor(body);
            g.fillOval((int)x - radius, (int)y - radius, radius * 2, radius * 2);

            g.setColor(new Color(30, 30, 35));
            g.fillOval((int)x - radius / 2, (int)y - radius / 3, 7, 7);
            g.fillOval((int)x + radius / 4, (int)y - radius / 3, 7, 7);

            // Barra de vida
            int barWidth = radius * 2;
            int current = (int)(barWidth * Math.max(0, hp) / (double)(type == 2 ? 80 + wave * 8 : 30 + wave * 4));
            g.setColor(new Color(40, 40, 40));
            g.fillRect((int)x - radius, (int)y - radius - 9, barWidth, 4);
            g.setColor(new Color(240, 70, 70));
            g.fillRect((int)x - radius, (int)y - radius - 9, current, 4);
        }
    }

    class Bullet {
        double x, y, vx, vy;
        int radius = 4;
        int damage = 25;

        Bullet(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        void update() {
            x += vx;
            y += vy;
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(255, 210, 70));
            g.fillOval((int)x - radius, (int)y - radius, radius * 2, radius * 2);
        }
    }

    class Particle {
        double x, y, vx, vy;
        int life = 25;
        Color color;
        int size;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (random.nextDouble() - 0.5) * 5;
            this.vy = (random.nextDouble() - 0.5) * 5;
            this.size = 2 + random.nextInt(5);
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.96;
            vy *= 0.96;
            life--;
        }

        void draw(Graphics2D g) {
            int alpha = Math.max(0, Math.min(255, life * 10));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillOval((int)x, (int)y, size, size);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Zombie Shooter - Java");
            ZombieShooter game = new ZombieShooter();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}
