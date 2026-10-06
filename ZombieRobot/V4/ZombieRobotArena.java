
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/*
 * ZOMBIE ROBOT ARENA
 * Juego 2D hecho únicamente con Java Swing/Java2D.
 * Un solo archivo: ZombieRobotArena.java
 */
public class ZombieRobotArena extends JPanel implements ActionListener, KeyListener, MouseMotionListener, MouseListener {

    static final int W = 1100, H = 720;
    static final int MENU=0, PLAY=1, PAUSE=2, SHOP=3, GAMEOVER=4, WIN=5;
    int screen = MENU;

    final Random rnd = new Random();
    final javax.swing.Timer timer = new javax.swing.Timer(16, this);

    Player player;
    List<Enemy> enemies = new ArrayList<>();
    List<Bullet> bullets = new ArrayList<>();
    List<Coin> coinDrops = new ArrayList<>();
    List<Particle> particles = new ArrayList<>();
    List<FloatingText> texts = new ArrayList<>();

    boolean up,down,left,right,shooting;
    int mouseX=W/2, mouseY=H/2;

    int difficulty=1; // 0 fácil, 1 normal, 2 difícil
    String difficultyName="NORMAL";
    int level=1, wave=1, wavesThisLevel=0;
    int totalWaves=6;
    int enemiesToSpawn=0, spawned=0, spawnTimer=0;
    boolean bossWave=false, bossAlive=false;
    boolean transition=false;
    int transitionTimer=0;

    int selectedWeapon=0, selectedSkin=0;
    int coins=0, score=0, kills=0;

    String[] weaponNames={"PISTOLA","ESCOPETA","SMG","RIFLE"};
    String[] skinNames={"AZUL","ROJO","VERDE","MORADO","DORADO"};
    int[] weaponCost={0,100,250,500};
    int[] skinCost={0,150,300,600,1200};
    boolean[] ownedWeapons={true,false,false,false};
    boolean[] ownedSkins={true,false,false,false,false};
    int[] weaponLevel={1,1,1,1};

    Rectangle playButton=new Rectangle(425,300,250,58);
    Rectangle difficultyButton=new Rectangle(425,375,250,50);
    Rectangle shopButton=new Rectangle(425,440,250,50);
    Rectangle exitButton=new Rectangle(425,505,250,50);

    Rectangle resumeButton=new Rectangle(400,280,300,55);
    Rectangle pauseShopButton=new Rectangle(400,350,300,55);
    Rectangle pauseExitButton=new Rectangle(400,420,300,55);

    public ZombieRobotArena(){
        setPreferredSize(new Dimension(W,H));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        timer.start();
        player=new Player(W/2,H/2);
    }

    void startGame(){
        screen=PLAY;
        level=1; wave=1; wavesThisLevel=0;
        score=0;kills=0;coins=0;
        enemies.clear();bullets.clear();coinsClear();particles.clear();texts.clear();
        player=new Player(W/2,H/2);
        startWave();
    }

    void coinsClear(){ coinDrops.clear(); }

    void startWave(){
        enemies.clear(); bullets.clear();
        bossAlive=false; bossWave=false;
        wavesThisLevel++;
        if(wavesThisLevel>=totalWaves){
            bossWave=true;
            enemiesToSpawn=1;
            spawned=0;
        } else {
            enemiesToSpawn=8 + level*2 + wave*3 + difficulty*5;
            spawned=0;
        }
        spawnTimer=0;
        transition=false;
    }

    void nextLevel(){
        if(level>=50){ screen=WIN; return; }
        level++;
        wave=1;
        wavesThisLevel=0;
        player.hp=Math.min(player.maxHp,player.hp+35);
        player.ammo=player.maxAmmo;
        startWave();
    }

    void update(){
        if(screen==PLAY){
            if(transition){
                transitionTimer--;
                if(transitionTimer<=0){
                    transition=false;
                    if(bossWave) nextLevel();
                    else { wave++; startWave(); }
                }
                updateParticles();
                return;
            }

            player.update();

            if(shooting) player.shoot();

            if(spawned<enemiesToSpawn){
                spawnTimer--;
                if(spawnTimer<=0){
                    spawnEnemy();
                    spawned++;
                    spawnTimer=Math.max(8,35-difficulty*5-level/4);
                }
            } else if(enemies.isEmpty() && (!bossWave || !bossAlive)){
                transition=true;
                transitionTimer=130;
            }

            updateBullets();
            updateEnemies();
            updateCoins();
            updateParticles();
            updateTexts();

            if(player.hp<=0) screen=GAMEOVER;
        }
    }

    void spawnEnemy(){
        double x,y;
        int side=rnd.nextInt(4);
        if(side==0){x=rnd.nextInt(W);y=-40;}
        else if(side==1){x=W+40;y=rnd.nextInt(H);}
        else if(side==2){x=rnd.nextInt(W);y=H+40;}
        else{x=-40;y=rnd.nextInt(H);}

        if(bossWave){
            enemies.add(new Boss(x,y));
            bossAlive=true;
        } else {
            int roll=rnd.nextInt(100);
            int type=roll<65?0:(roll<88?1:2);
            enemies.add(new Enemy(x,y,type));
        }
    }

    void updateBullets(){
        Iterator<Bullet> it=bullets.iterator();
        while(it.hasNext()){
            Bullet b=it.next();
            b.update();
            if(b.life<=0){it.remove();continue;}
            boolean remove=false;
            if(b.enemyShot){
                continue;
            }
            Iterator<Enemy> ei=enemies.iterator();
            while(ei.hasNext()){
                Enemy e=ei.next();
                if(dist(b.x,b.y,e.x,e.y)<e.radius+b.radius){
                    e.hp-=b.damage;
                    particles(b.x,b.y,5,new Color(255,210,70));
                    remove=true;
                    if(e.hp<=0){
                        kills++;
                        score+=e instanceof Boss?1000:(e.type==2?50:e.type==1?30:15);
                        int amount=e instanceof Boss?1000:(e.type==2?40:(e.type==1?25:10));
                        dropCoins(e.x,e.y,amount);
                        particles(e.x,e.y,e instanceof Boss?55:20,
                                e instanceof Boss?new Color(190,50,255):new Color(80,220,100));
                        texts.add(new FloatingText("+"+amount+" monedas",e.x,e.y-25,new Color(255,210,50)));
                        if(e instanceof Boss) bossAlive=false;
                        ei.remove();
                    }
                    break;
                }
            }
            if(remove) it.remove();
        }
    }

    void updateEnemies(){
        for(Enemy e:enemies){
            e.update();
            if(dist(e.x,e.y,player.x,player.y)<e.radius+player.radius){
                if(player.damageCooldown<=0){
                    player.hp-=e.damage;
                    player.damageCooldown=35;
                    particles(player.x,player.y,12,new Color(255,60,60));
                }
            }
        }
    }

    void dropCoins(double x,double y,int amount){
        int n=Math.min(12,Math.max(2,amount/10));
        for(int i=0;i<n;i++){
            int value=amount/n;
            if(i==n-1)value+=amount%n;
            coinDrops.add(new Coin(x+rnd.nextInt(25)-12,y+rnd.nextInt(25)-12,value));
        }
    }

    void updateCoins(){
        Iterator<Coin> it=coinDrops.iterator();
        while(it.hasNext()){
            Coin c=it.next();
            c.update();
            if(dist(c.x,c.y,player.x,player.y)<player.radius+12){
                coins+=c.value;
                score+=c.value;
                texts.add(new FloatingText("+"+c.value,c.x,c.y,new Color(255,215,50)));
                it.remove();
            }
        }
    }

    void updateParticles(){
        Iterator<Particle> it=particles.iterator();
        while(it.hasNext()){
            Particle p=it.next();p.update();
            if(p.life<=0)it.remove();
        }
    }

    void updateTexts(){
        Iterator<FloatingText> it=texts.iterator();
        while(it.hasNext()){
            FloatingText t=it.next();t.update();
            if(t.life<=0)it.remove();
        }
    }

    double dist(double a,double b,double c,double d){return Math.hypot(a-c,b-d);}

    void particles(double x,double y,int n,Color c){
        for(int i=0;i<n;i++)particles.add(new Particle(x,y,c));
    }

    void buyOrSelectWeapon(int i){
        if(ownedWeapons[i]) selectedWeapon=i;
        else if(coins>=weaponCost[i]){
            coins-=weaponCost[i];ownedWeapons[i]=true;selectedWeapon=i;
        }
    }

    void upgradeWeapon(){
        int cost=weaponLevel[selectedWeapon]*180;
        if(weaponLevel[selectedWeapon]<8 && coins>=cost){
            coins-=cost;weaponLevel[selectedWeapon]++;
        }
    }

    void buyOrSelectSkin(int i){
        if(ownedSkins[i])selectedSkin=i;
        else if(coins>=skinCost[i]){
            coins-=skinCost[i];ownedSkins[i]=true;selectedSkin=i;
        }
    }

    void drawGame(Graphics2D g){
        drawArena(g);
        if(screen==MENU)drawMenu(g);
        else if(screen==PLAY)drawHUD(g);
        else if(screen==PAUSE){drawHUD(g);drawPause(g);}
        else if(screen==SHOP)drawShop(g);
        else if(screen==GAMEOVER)drawGameOver(g);
        else if(screen==WIN)drawWin(g);
    }

    void drawArena(Graphics2D g){
        // Fondo degradado
        GradientPaint bg=new GradientPaint(0,0,new Color(7,10,16),W,H,new Color(20,25,34));
        g.setPaint(bg);
        g.fillRect(0,0,W,H);

        // Zonas de luz
        g.setColor(new Color(25,85,115,45));
        g.fillOval(-180,-220,560,560);
        g.setColor(new Color(145,30,105,35));
        g.fillOval(W-430,H-360,700,700);

        // Suelo tecnológico
        g.setColor(new Color(25,31,41));
        g.fillRect(0,82,W,H-82);

        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(42,50,62,115));
        for(int x=0;x<W;x+=44) g.drawLine(x,82,x,H);
        for(int y=82;y<H;y+=44) g.drawLine(0,y,W,y);

        // Líneas diagonales decorativas
        g.setColor(new Color(80,190,220,20));
        for(int i=-H;i<W;i+=120) g.drawLine(i,H,i+H,82);

        // Bordes de la arena
        g.setColor(new Color(70,210,235,100));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(0,82,W,82);
        g.drawLine(8,88,8,H-8);
        g.drawLine(W-8,88,W-8,H-8);

        for(Coin c:coinDrops)c.draw(g);
        for(Particle p:particles)p.draw(g);
        for(Bullet b:bullets)b.draw(g);
        for(Enemy e:enemies)e.draw(g);
        player.draw(g);
    }

    void drawHUD(Graphics2D g){
        // Panel superior
        g.setPaint(new GradientPaint(0,0,new Color(9,14,22,245),0,82,new Color(18,25,36,235)));
        g.fillRect(0,0,W,82);

        g.setColor(new Color(70,210,235,130));
        g.fillRect(0,80,W,2);

        // Nivel
        g.setFont(new Font("Arial",Font.BOLD,17));
        g.setColor(Color.WHITE);
        g.drawString("NIVEL",20,25);
        g.setFont(new Font("Arial",Font.BOLD,25));
        g.setColor(new Color(80,220,245));
        g.drawString(level+"/50",20,52);

        // Oleada
        g.setFont(new Font("Arial",Font.BOLD,14));
        g.setColor(new Color(190,200,215));
        g.drawString("OLEADA",105,24);
        g.setFont(new Font("Arial",Font.BOLD,19));
        g.setColor(bossWave?new Color(255,85,150):Color.WHITE);
        g.drawString(wavesThisLevel+"/"+totalWaves+(bossWave?"  ★ JEFE":""),
                105,50);

        // Bajas
        g.setFont(new Font("Arial",Font.BOLD,14));
        g.setColor(new Color(190,200,215));
        g.drawString("BAJAS",275,24);
        g.setFont(new Font("Arial",Font.BOLD,19));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(kills),275,50);

        // Monedas
        g.setColor(new Color(255,215,70));
        g.fillOval(345,28,18,18);
        g.setColor(new Color(125,80,10));
        g.drawOval(345,28,18,18);
        g.setFont(new Font("Arial",Font.BOLD,19));
        g.setColor(new Color(255,225,90));
        g.drawString(String.valueOf(coins),372,44);

        // Vida
        int hpw=210;
        g.setColor(new Color(45,50,60));
        g.fillRoundRect(475,20,hpw,18,9,9);
        g.setColor(player.hp<30?new Color(235,55,70):new Color(55,220,115));
        g.fillRoundRect(475,20,(int)(hpw*Math.max(0,player.hp)/(double)player.maxHp),18,9,9);
        g.setColor(new Color(230,240,250));
        g.drawRoundRect(475,20,hpw,18,9,9);
        g.setFont(new Font("Arial",Font.BOLD,12));
        g.setColor(Color.WHITE);
        g.drawString("VIDA  "+Math.max(0,player.hp)+"/"+player.maxHp,535,34);

        // Arma equipada
        g.setColor(new Color(28,35,47));
        g.fillRoundRect(705,12,205,56,12,12);
        g.setColor(new Color(75,95,120));
        g.drawRoundRect(705,12,205,56,12,12);
        g.setFont(new Font("Arial",Font.BOLD,13));
        g.setColor(new Color(150,170,195));
        g.drawString("ARMA EQUIPADA",720,30);
        g.setFont(new Font("Arial",Font.BOLD,17));
        g.setColor(Color.WHITE);
        g.drawString(weaponNames[selectedWeapon]+"  ·  NIV."+weaponLevel[selectedWeapon],720,52);

        // Botones/ayuda
        g.setFont(new Font("Arial",Font.BOLD,12));
        g.setColor(new Color(160,175,195));
        g.drawString("[P] PAUSA",930,25);
        g.drawString("[B] ARSENAL",930,44);
        g.setFont(new Font("Arial",Font.PLAIN,11));
        g.drawString("WASD · MOVER",930,62);

        // Mira
        g.setColor(new Color(110,230,255,210));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(mouseX-9,mouseY-9,18,18);
        g.drawLine(mouseX-17,mouseY,mouseX-5,mouseY);
        g.drawLine(mouseX+5,mouseY,mouseX+17,mouseY);
        g.drawLine(mouseX,mouseY-17,mouseX,mouseY-5);
        g.drawLine(mouseX,mouseY+5,mouseX,mouseY+17);

        if(transition){
            g.setColor(new Color(0,0,0,185));
            g.fillRect(0,0,W,H);
            g.setColor(new Color(70,210,235,25));
            g.fillOval(W/2-260,H/2-180,520,360);
            String s=bossWave?"¡JEFE DERROTADO!":"¡OLEADA SUPERADA!";
            center(g,s,H/2-15,new Font("Arial",Font.BOLD,44),
                    bossWave?new Color(255,90,170):new Color(90,230,145));
            center(g,bossWave?"Preparando el siguiente nivel...":"Preparando la siguiente oleada...",
                    H/2+28,new Font("Arial",Font.PLAIN,20),Color.LIGHT_GRAY);
        }
    }

    void drawMenu(Graphics2D g){
        // Fondo
        g.setPaint(new GradientPaint(0,0,new Color(5,8,15),W,H,new Color(26,12,30)));
        g.fillRect(0,0,W,H);

        // Círculos/luces decorativas
        g.setColor(new Color(45,210,245,35));
        g.fillOval(-180,90,520,520);
        g.setColor(new Color(245,55,125,25));
        g.fillOval(W-360,-80,500,500);

        // Marco central
        g.setColor(new Color(18,24,35,235));
        g.fillRoundRect(270,55,560,605,28,28);
        g.setColor(new Color(75,200,230,100));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(270,55,560,605,28,28);

        center(g,"ZOMBIE",145,new Font("Arial",Font.BOLD,67),new Color(85,225,245));
        center(g,"ROBOT",205,new Font("Arial",Font.BOLD,67),new Color(255,70,120));
        center(g,"A R E N A",237,new Font("Arial",Font.BOLD,17),new Color(190,200,220));

        // Línea decorativa
        g.setColor(new Color(90,210,235,120));
        g.fillRect(370,260,360,2);

        button(g,playButton,"▶  JUGAR",new Color(35,175,105));
        button(g,difficultyButton,"⚙  DIFICULTAD: "+difficultyName,new Color(45,115,205));
        button(g,shopButton,"◆  ARSENAL / TIENDA",new Color(145,75,205));
        button(g,exitButton,"✕  SALIR",new Color(180,55,70));

        center(g,"50 NIVELES  •  JEFES  •  ARMAS  •  SKINS",590,
                new Font("Arial",Font.BOLD,12),new Color(145,160,180));
        center(g,"WASD · RATÓN · CLICK IZQUIERDO",620,
                new Font("Arial",Font.PLAIN,12),new Color(100,120,145));
    }

    void drawPause(Graphics2D g){
        g.setColor(new Color(0,0,0,185));g.fillRect(0,0,W,H);
        center(g,"PAUSA",190,new Font("Arial",Font.BOLD,60),Color.WHITE);
        button(g,resumeButton,"CONTINUAR",new Color(45,180,100));
        button(g,pauseShopButton,"TIENDA / MEJORAS",new Color(170,90,220));
        button(g,pauseExitButton,"VOLVER AL MENÚ",new Color(190,60,65));
    }

    void drawShop(Graphics2D g){
        g.setColor(new Color(11,14,20));g.fillRect(0,0,W,H);
        center(g,"ARSENAL Y PERSONALIZACIÓN",65,new Font("Arial",Font.BOLD,34),Color.WHITE);
        g.setFont(new Font("Arial",Font.BOLD,18));g.setColor(new Color(255,215,50));
        g.drawString("🪙 Monedas: "+coins,35,105);

        g.setFont(new Font("Arial",Font.BOLD,20));g.setColor(Color.WHITE);
        g.drawString("ARMAS",45,145);
        for(int i=0;i<4;i++){
            int y=165+i*68;
            Rectangle r=new Rectangle(35,y,315,52);
            Color c=i==selectedWeapon?new Color(40,130,200):new Color(35,40,50);
            button(g,r,weaponNames[i]+"  "+(ownedWeapons[i]?"[USAR]":"["+weaponCost[i]+" 🪙]"),c);
            if(i==selectedWeapon){
                g.setFont(new Font("Arial",Font.PLAIN,13));g.setColor(Color.LIGHT_GRAY);
                g.drawString("Nivel "+weaponLevel[i]+"   Daño: "+(weaponDamage(i)),
                        370,y+32);
            }
        }

        Rectangle upg=new Rectangle(370,165,250,52);
        button(g,upg,"MEJORAR ("+(weaponLevel[selectedWeapon]*180)+" 🪙)",
                new Color(220,150,40));

        g.setFont(new Font("Arial",Font.BOLD,20));g.setColor(Color.WHITE);
        g.drawString("SKINS",670,145);
        for(int i=0;i<5;i++){
            int y=165+i*60;
            Rectangle r=new Rectangle(650,y,400,46);
            button(g,r,skinNames[i]+"  "+(ownedSkins[i]?"[USAR]":"["+skinCost[i]+" 🪙]"),
                    skinColor(i));
        }

        Rectangle back=new Rectangle(420,620,260,52);
        button(g,back,"VOLVER",new Color(90,95,105));
        g.setFont(new Font("Arial",Font.PLAIN,14));g.setColor(Color.LIGHT_GRAY);
        g.drawString("Comprar: pulsa el botón. Mejorar: sube daño y cadencia.",
                370,250);
    }

    int weaponDamage(int i){return 20+(weaponLevel[i]-1)*7+(i==1?15:i==3?20:0);}

    Color skinColor(int i){
        switch(i){
            case 1: return new Color(200,65,70);
            case 2: return new Color(60,190,100);
            case 3: return new Color(165,75,220);
            case 4: return new Color(230,180,45);
            default: return new Color(55,155,235);
        }
    }

    void drawGameOver(Graphics2D g){
        g.setColor(new Color(0,0,0,205));g.fillRect(0,0,W,H);
        center(g,"HAS MUERTO",230,new Font("Arial",Font.BOLD,65),new Color(255,65,70));
        center(g,"Nivel alcanzado: "+level+"   •   Bajas: "+kills+"   •   Puntos: "+score,
                290,new Font("Arial",Font.BOLD,21),Color.WHITE);
        center(g,"Monedas acumuladas: "+coins,330,new Font("Arial",Font.PLAIN,19),Color.LIGHT_GRAY);
        center(g,"Pulsa ENTER para volver al menú",405,new Font("Arial",Font.BOLD,23),
                new Color(255,210,70));
    }

    void drawWin(Graphics2D g){
        g.setColor(new Color(0,0,0,215));g.fillRect(0,0,W,H);
        center(g,"¡HAS COMPLETADO LOS 50 NIVELES!",270,
                new Font("Arial",Font.BOLD,45),new Color(80,220,120));
        center(g,"Puntuación: "+score+"   •   Bajas: "+kills+"   •   Monedas: "+coins,
                325,new Font("Arial",Font.BOLD,21),Color.WHITE);
        center(g,"Pulsa ENTER para volver al menú",410,new Font("Arial",Font.BOLD,23),
                new Color(255,210,70));
    }

    void center(Graphics2D g,String s,int y,Font f,Color c){
        g.setFont(f);g.setColor(c);
        FontMetrics fm=g.getFontMetrics();
        g.drawString(s,(W-fm.stringWidth(s))/2,y);
    }

    void button(Graphics2D g,Rectangle r,String s,Color c){
        // Sombra
        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(r.x+4,r.y+6,r.width,r.height,14,14);

        // Cuerpo
        g.setPaint(new GradientPaint(r.x,r.y,c,r.x,r.y+r.height,
                new Color(Math.max(0,c.getRed()-35),Math.max(0,c.getGreen()-35),Math.max(0,c.getBlue()-35))));
        g.fillRoundRect(r.x,r.y,r.width,r.height,14,14);

        // Brillo superior
        g.setColor(new Color(255,255,255,35));
        g.fillRoundRect(r.x+2,r.y+2,r.width-4,r.height/2,12,12);

        g.setColor(new Color(255,255,255,170));
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(r.x,r.y,r.width,r.height,14,14);

        g.setFont(new Font("Arial",Font.BOLD,16));
        FontMetrics fm=g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(s,r.x+(r.width-fm.stringWidth(s))/2,
                r.y+(r.height+fm.getAscent())/2-4);
    }

    @Override protected void paintComponent(Graphics gg){
        super.paintComponent(gg);
        Graphics2D g=(Graphics2D)gg.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        drawGame(g);
        g.dispose();
    }

    @Override public void actionPerformed(ActionEvent e){update();repaint();}

    @Override public void keyPressed(KeyEvent e){
        int k=e.getKeyCode();
        if(k==KeyEvent.VK_W)up=true;
        if(k==KeyEvent.VK_S)down=true;
        if(k==KeyEvent.VK_A)left=true;
        if(k==KeyEvent.VK_D)right=true;

        if(k==KeyEvent.VK_P && screen==PLAY)screen=PAUSE;
        else if(k==KeyEvent.VK_P && screen==PAUSE)screen=PLAY;

        if(k==KeyEvent.VK_B && (screen==PLAY||screen==PAUSE))screen=SHOP;

        if(k==KeyEvent.VK_ENTER && (screen==GAMEOVER||screen==WIN))screen=MENU;
    }

    @Override public void keyReleased(KeyEvent e){
        int k=e.getKeyCode();
        if(k==KeyEvent.VK_W)up=false;
        if(k==KeyEvent.VK_S)down=false;
        if(k==KeyEvent.VK_A)left=false;
        if(k==KeyEvent.VK_D)right=false;
    }
    @Override public void keyTyped(KeyEvent e){}

    @Override public void mouseMoved(MouseEvent e){mouseX=e.getX();mouseY=e.getY();}
    @Override public void mouseDragged(MouseEvent e){mouseMoved(e);}

    @Override public void mousePressed(MouseEvent e){
        mouseX=e.getX();mouseY=e.getY();
        if(e.getButton()!=MouseEvent.BUTTON1)return;

        if(screen==MENU){
            if(playButton.contains(e.getPoint()))startGame();
            else if(difficultyButton.contains(e.getPoint())){
                difficulty=(difficulty+1)%3;
                difficultyName=difficulty==0?"FÁCIL":difficulty==1?"NORMAL":"DIFÍCIL";
            } else if(shopButton.contains(e.getPoint()))screen=SHOP;
            else if(exitButton.contains(e.getPoint()))System.exit(0);
        } else if(screen==PLAY)shooting=true;
        else if(screen==PAUSE){
            if(resumeButton.contains(e.getPoint()))screen=PLAY;
            else if(pauseShopButton.contains(e.getPoint()))screen=SHOP;
            else if(pauseExitButton.contains(e.getPoint()))screen=MENU;
        } else if(screen==SHOP){
            if(new Rectangle(420,620,260,52).contains(e.getPoint())){
                screen=PLAY;
            } else {
                for(int i=0;i<4;i++){
                    Rectangle r=new Rectangle(35,165+i*68,315,52);
                    if(r.contains(e.getPoint())){buyOrSelectWeapon(i);return;}
                }
                if(new Rectangle(370,165,250,52).contains(e.getPoint())){
                    upgradeWeapon();return;
                }
                for(int i=0;i<5;i++){
                    Rectangle r=new Rectangle(650,165+i*60,400,46);
                    if(r.contains(e.getPoint())){buyOrSelectSkin(i);return;}
                }
            }
        }
    }

    @Override public void mouseReleased(MouseEvent e){if(e.getButton()==MouseEvent.BUTTON1)shooting=false;}
    @Override public void mouseClicked(MouseEvent e){}
    @Override public void mouseEntered(MouseEvent e){}
    @Override public void mouseExited(MouseEvent e){}

    class Player{
        double x,y; int radius=19, maxHp=100,hp=100,damageCooldown=0,shootCooldown=0;
        double speed=4.3;
        int ammo=999,maxAmmo=999;
        Player(double x,double y){this.x=x;this.y=y;}

        void update(){
            double dx=0,dy=0;
            if(up)dy--;if(down)dy++;if(left)dx--;if(right)dx++;
            if(dx!=0||dy!=0){
                double l=Math.hypot(dx,dy);x+=dx/l*speed;y+=dy/l*speed;
            }
            x=Math.max(radius,Math.min(W-radius,x));
            y=Math.max(95,Math.min(H-radius,y));
            if(shootCooldown>0)shootCooldown--;
            if(damageCooldown>0)damageCooldown--;
        }

        void shoot(){
            if(shootCooldown>0)return;
            double dx=mouseX-x,dy=mouseY-y,l=Math.max(1,Math.hypot(dx,dy));
            dx/=l;dy/=l;
            int lvl=weaponLevel[selectedWeapon];
            int dmg=weaponDamage(selectedWeapon);
            if(selectedWeapon==1){
                for(int i=-2;i<=2;i++){
                    double a=Math.atan2(dy,dx)+i*0.12;
                    bullets.add(new Bullet(x+dx*25,y+dy*25,Math.cos(a)*10,Math.sin(a)*10,dmg));
                }
                shootCooldown=Math.max(8,20-lvl);
            } else if(selectedWeapon==2){
                for(int i=0;i<2;i++){
                    double a=Math.atan2(dy,dx)+(i==0?-0.04:0.04);
                    bullets.add(new Bullet(x+dx*25,y+dy*25,Math.cos(a)*13,Math.sin(a)*13,dmg));
                }
                shootCooldown=Math.max(3,8-lvl);
            } else if(selectedWeapon==3){
                bullets.add(new Bullet(x+dx*25,y+dy*25,dx*16,dy*16,dmg));
                shootCooldown=Math.max(4,13-lvl);
            } else {
                bullets.add(new Bullet(x+dx*25,y+dy*25,dx*12,dy*12,dmg));
                shootCooldown=Math.max(7,15-lvl);
            }
            particles(x+dx*27,y+dy*27,3,new Color(255,210,80));
        }

        void draw(Graphics2D g){
            // Sombra
            g.setColor(new Color(0,0,0,120));
            g.fillOval((int)x-radius+5,(int)y-radius+7,radius*2,radius*2);

            // Aura de selección
            g.setColor(new Color(80,220,245,35));
            g.fillOval((int)x-radius-7,(int)y-radius-7,(radius+7)*2,(radius+7)*2);

            // Cuerpo
            g.setColor(skinColor(selectedSkin));
            g.fillOval((int)x-radius,(int)y-radius,radius*2,radius*2);
            g.setColor(new Color(220,245,255,150));
            g.drawOval((int)x-radius,(int)y-radius,radius*2,radius*2);

            // Visor
            g.setColor(new Color(20,28,38));
            g.fillRoundRect((int)x-11,(int)y-8,22,10,5,5);
            g.setColor(new Color(105,235,255));
            g.fillOval((int)x-7,(int)y-5,5,5);
            g.fillOval((int)x+3,(int)y-5,5,5);

            // Arma apuntando
            double dx=mouseX-x,dy=mouseY-y,l=Math.max(1,Math.hypot(dx,dy));dx/=l;dy/=l;
            g.setStroke(new BasicStroke(9,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.setColor(new Color(35,40,50));
            g.drawLine((int)(x+dx*8),(int)(y+dy*8),(int)(x+dx*34),(int)(y+dy*34));
            g.setStroke(new BasicStroke(3f));
            g.setColor(new Color(130,145,165));
            g.drawLine((int)(x+dx*20),(int)(y+dy*20),(int)(x+dx*38),(int)(y+dy*38));

            if(damageCooldown>0){
                g.setColor(new Color(255,70,80,120));
                g.drawOval((int)x-27,(int)y-27,54,54);
            }
        }
    }

    class Enemy{
        double x,y,speed; int type,radius,hp,maxHp,damage;
        Enemy(double x,double y,int type){
            this.x=x;this.y=y;this.type=type;
            if(type==0){radius=17;maxHp=30+level*5;speed=1.45+level*.025;damage=8+difficulty*2;}
            else if(type==1){radius=22;maxHp=65+level*8;speed=1.0+level*.02;damage=12+difficulty*3;}
            else {radius=14;maxHp=22+level*4;speed=2.5+level*.03;damage=7+difficulty*2;}
            hp=maxHp;
        }
        void update(){
            double dx=player.x-x,dy=player.y-y,l=Math.max(1,Math.hypot(dx,dy));
            x+=dx/l*speed;y+=dy/l*speed;
        }
        void draw(Graphics2D g){
            Color c=type==0?new Color(55,200,105):type==1?new Color(190,65,220):new Color(235,145,45);

            g.setColor(new Color(0,0,0,110));
            g.fillOval((int)x-radius+5,(int)y-radius+7,radius*2,radius*2);

            g.setColor(c);
            g.fillRoundRect((int)x-radius,(int)y-radius,radius*2,radius*2,10,10);
            g.setColor(new Color(245,255,255,150));
            g.drawRoundRect((int)x-radius,(int)y-radius,radius*2,radius*2,10,10);

            // Cara robótica
            g.setColor(new Color(20,25,32));
            g.fillRoundRect((int)x-radius/2,(int)y-radius/3,radius,10,5,5);
            g.setColor(new Color(255,80,95));
            g.fillOval((int)x-radius/3,(int)y-radius/4,5,5);
            g.fillOval((int)x+radius/6,(int)y-radius/4,5,5);

            // Antena / núcleo
            g.setColor(new Color(220,235,245,120));
            g.drawLine((int)x,(int)y-radius,(int)x,(int)y-radius-7);
            g.fillOval((int)x-3,(int)y-radius-10,6,6);

            healthBar(g);
        }
        void healthBar(Graphics2D g){
            int bw=radius*2,c=(int)(bw*Math.max(0,hp)/(double)maxHp);
            g.setColor(new Color(40,40,40));g.fillRect((int)x-radius,(int)y-radius-8,bw,4);
            g.setColor(new Color(240,70,70));g.fillRect((int)x-radius,(int)y-radius-8,c,4);
        }
    }

    class Boss extends Enemy{
        int attackCooldown=0;
        Boss(double x,double y){super(x,y,1);radius=52;maxHp=900+level*100+difficulty*250;hp=maxHp;speed=.65+level*.008;damage=20+difficulty*5;}
        @Override void update(){
            super.update();
            attackCooldown--;
            if(attackCooldown<=0){
                // Ataque: proyectiles hacia el jugador
                double dx=player.x-x,dy=player.y-y,l=Math.max(1,Math.hypot(dx,dy));
                dx/=l;dy/=l;
                for(int i=-1;i<=1;i++){
                    double a=Math.atan2(dy,dx)+i*.18;
                    bullets.add(new Bullet(x,y,Math.cos(a)*5,Math.sin(a)*5,0,true));
                }
                attackCooldown=75;
                particles(x,y,8,new Color(255,70,180));
            }
        }
        @Override void draw(Graphics2D g){
            // Aura
            g.setColor(new Color(255,50,170,25));
            g.fillOval((int)x-radius-18,(int)y-radius-18,(radius+18)*2,(radius+18)*2);

            g.setColor(new Color(0,0,0,140));
            g.fillOval((int)x-radius+8,(int)y-radius+10,radius*2,radius*2);

            g.setColor(new Color(170,35,205));
            g.fillOval((int)x-radius,(int)y-radius,radius*2,radius*2);
            g.setColor(new Color(255,130,230,180));
            g.drawOval((int)x-radius,(int)y-radius,radius*2,radius*2);

            // Ojos
            g.setColor(new Color(70,15,90));
            g.fillOval((int)x-24,(int)y-19,18,18);
            g.fillOval((int)x+6,(int)y-19,18,18);
            g.setColor(new Color(255,80,190));
            g.fillOval((int)x-19,(int)y-14,8,8);
            g.fillOval((int)x+11,(int)y-14,8,8);

            // Núcleo
            g.setColor(new Color(255,90,205));
            g.fillOval((int)x-9,(int)y-9,18,18);
            g.setColor(Color.WHITE);
            g.fillOval((int)x-3,(int)y-3,6,6);

            // Barra de jefe
            g.setColor(new Color(20,22,30,230));
            g.fillRoundRect((int)x-125,(int)y-radius-31,250,16,8,8);
            g.setColor(new Color(255,55,105));
            g.fillRoundRect((int)x-125,(int)y-radius-31,
                    (int)(250*Math.max(0,hp)/(double)maxHp),16,8,8);
            g.setColor(new Color(255,220,240,190));
            g.drawRoundRect((int)x-125,(int)y-radius-31,250,16,8,8);

            g.setFont(new Font("Arial",Font.BOLD,15));
            g.setColor(new Color(255,190,235));
            g.drawString("★ JEFE ★",(int)x-35,(int)y-radius-38);
        }
    }

    class Bullet{
        double x,y,vx,vy;int life=70,radius=4,damage;boolean enemyShot;
        Bullet(double x,double y,double vx,double vy,int damage){this(x,y,vx,vy,damage,false);}
        Bullet(double x,double y,double vx,double vy,int damage,boolean enemyShot){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.damage=damage;this.enemyShot=enemyShot;
        }
        void update(){
            x+=vx;y+=vy;life--;
            if(enemyShot && dist(x,y,player.x,player.y)<player.radius+radius){
                if(player.damageCooldown<=0){player.hp-=damage;player.damageCooldown=25;}
                life=0;
                particles(x,y,8,new Color(255,50,120));
            }
        }
        void draw(Graphics2D g){
            g.setColor(enemyShot?new Color(255,70,170):new Color(255,210,70));
            g.fillOval((int)x-radius,(int)y-radius,radius*2,radius*2);
        }
    }

    class Coin{
        double x,y,vx,vy;int value;
        Coin(double x,double y,int value){this.x=x;this.y=y;this.value=value;
            vx=(rnd.nextDouble()-.5)*3;vy=(rnd.nextDouble()-.5)*3;}
        void update(){x+=vx;y+=vy;vx*=.95;vy*=.95;}
        void draw(Graphics2D g){
            g.setColor(new Color(255,215,40));g.fillOval((int)x-7,(int)y-7,14,14);
            g.setColor(new Color(140,90,10));g.drawOval((int)x-7,(int)y-7,14,14);
            g.setFont(new Font("Arial",Font.BOLD,8));g.setColor(new Color(120,80,0));g.drawString("$",(int)x-3,(int)y+3);
        }
    }

    class Particle{
        double x,y,vx,vy;int life=28,size;Color color;
        Particle(double x,double y,Color c){this.x=x;this.y=y;color=c;size=2+rnd.nextInt(5);
            vx=(rnd.nextDouble()-.5)*6;vy=(rnd.nextDouble()-.5)*6;}
        void update(){x+=vx;y+=vy;vx*=.95;vy*=.95;life--;}
        void draw(Graphics2D g){
            int a=Math.max(0,Math.min(255,life*9));
            g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),a));
            g.fillOval((int)x,(int)y,size,size);
        }
    }

    class FloatingText{
        double x,y;String s;Color c;int life=55;
        FloatingText(String s,double x,double y,Color c){this.s=s;this.x=x;this.y=y;this.c=c;}
        void update(){y-=.5;life--;}
        void draw(Graphics2D g){g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),Math.min(255,life*5)));
            g.setFont(new Font("Arial",Font.BOLD,14));g.drawString(s,(int)x,(int)y);}
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            JFrame f=new JFrame("Zombie Robot Arena");
            ZombieRobotArena game=new ZombieRobotArena();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);f.add(game);f.pack();f.setLocationRelativeTo(null);f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
