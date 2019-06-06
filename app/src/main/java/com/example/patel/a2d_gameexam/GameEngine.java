package com.example.patel.a2d_gameexam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class GameEngine extends SurfaceView implements Runnable {
    private final String TAG = "SPARROW";

    // game thread variables
    private Thread gameThread = null;
    private volatile boolean gameIsRunning;

    // drawing variables
    private Canvas canvas;
    private Paint paintbrush;
    private SurfaceHolder holder;

    // Screen resolution varaibles
    private int screenWidth;
    private int screenHeight;

    // VISIBLE GAME PLAY AREA
    // These variables are set in the constructor
    int VISIBLE_LEFT;
    int VISIBLE_TOP;
    int VISIBLE_RIGHT;
    int VISIBLE_BOTTOM;

    // SPRITES
    Square bullet;
    int SQUARE_WIDTH = 100;
    int initialCatPosX = this.screenWidth - 20 -200;
    int initialCatPosY = (int) (this.screenHeight * 0.9) -200;

    Square enemy;

    Sprite player;
    Sprite sparrow;
    Sprite cat;
    Rect cage;
    Rect cageHitBox;
    ArrayList<Square> bullets = new ArrayList<Square>();

    // GAME STATS
    int score = 0;

    public GameEngine(Context context, int screenW, int screenH) {
        super(context);

        // intialize the drawing variables
        this.holder = this.getHolder();
        this.paintbrush = new Paint();

        // set screen height and width
        this.screenWidth = screenW;
        this.screenHeight = screenH;

        // setup visible game play area variables
        this.VISIBLE_LEFT = 20;
        this.VISIBLE_TOP = 10;
        this.VISIBLE_BOTTOM = this.screenWidth - 20;
        this.VISIBLE_RIGHT = (int) (this.screenHeight * 0.9);


        // initalize sprites
        //player
        this.player = new Sprite(this.getContext(), 50, (VISIBLE_RIGHT - 200), R.drawable.player64);
        //sparrow
        this.sparrow = new Sprite(this.getContext(), 500, 200, R.drawable.bird64);
        //cat
        this.cat = new Sprite(this.getContext(), (VISIBLE_BOTTOM - 200), (VISIBLE_RIGHT - 200), R.drawable.cat64);
        //cage
        int initialLeft = screenWidth - 300;
        int initialTop = 20;
        int initialRight = screenWidth - 10;
        int initialBottom = 200;
        cage = new Rect(initialLeft,initialTop, initialRight,initialBottom);
        cageHitBox = new Rect(initialLeft,initialTop, initialRight,initialBottom);
        //bullet
        bullet = new Square(getContext(), (this.player.getxPosition()+80), this.player.getyPosition(), 50);


    }

    @Override
    public void run() {
        while (gameIsRunning == true) {
            updateGame();    // updating positions of stuff
            redrawSprites(); // drawing the stuff
            controlFPS();
        }
    }

    boolean isCageMovingLeft = true;
    boolean iscageMovingDown = false;
    boolean isCageMoving = true;
    boolean iscatMoving = true;
    boolean isCatMovingLeft = true;
    boolean isBirdMoving = true;

    long currentTime = 0;
    long previousTime = 0;

    // Game Loop methods
    public void updateGame() {
        //Moving Cage
        if(isCageMoving && iscageMovingDown == false) {
            if (isCageMovingLeft == true) {
                cage.left = cage.left - 10;
                cage.right = cage.right - 10;
                cageHitBox.left = cage.left - 10;
                cageHitBox.right = cage.right - 10;
            }

            if (isCageMovingLeft == false) {
                cage.left = cage.left + 10;
                cage.right = cage.right + 10;
                cageHitBox.left = cage.left + 10;
                cageHitBox.right = cage.right + 10;
            }

            if (cage.left <= 0) {
                Log.d(TAG, "You are on left");
                isCageMovingLeft = false;
            }

            if (cage.right >= screenWidth) {
                Log.d(TAG, "You are on right");
                isCageMovingLeft = true;
            }
        }

        //moving cat
        if(iscatMoving) {
            Log.d(TAG, "X = " + this.cat.getxPosition());
            if (isCatMovingLeft == true) {
                this.cat.setxPosition(this.cat.getxPosition() - 20);
            }

            if (isCatMovingLeft == false) {
                this.cat.setxPosition(this.cat.getxPosition() + 20);
            }

            if (this.cat.getxPosition() <= (screenWidth / 3)) {
                Log.d(TAG, "You are on left");
                isCatMovingLeft = false;
            }

            if ((this.cat.getxPosition() - 150) >= screenWidth) {
                Log.d(TAG, "You are on right");
                isCatMovingLeft = true;
            }
        }


        //moving bird
        if(isBirdMoving) {
            int[] newcoor;
            newcoor = randcoor();
            currentTime = System.currentTimeMillis();
            if ((currentTime - previousTime) > 2000) {
                this.sparrow.setxPosition(newcoor[0] + (screenWidth / 2));
                this.sparrow.setyPosition(newcoor[1]);
                previousTime = currentTime;
            }
        }

        //moving bullet
        if(userX != 0 && userY != 0) {
            this.bullet.setHitbox(new Rect(userX, userY, userX + this.bullet.getWidth(), userY + this.bullet.getWidth()));
        }
        //cage dropping
        if(this.bullet.getHitbox().intersect(this.cageHitBox)){
            iscageMovingDown = true;
        }

        if(iscageMovingDown && this.cage.bottom <= VISIBLE_RIGHT) {
            cage.top = cage.top + 10;
            cage.bottom = cage.bottom + 10;
            cageHitBox.top = cage.top + 10;
            cageHitBox.bottom = cage.bottom + 10;
        }

        //win conditions
        if(!this.cageHitBox.intersect(this.sparrow.getHitbox())) {
            if (this.cat.getHitbox().intersect(this.cageHitBox)) {
                canvas.drawText("You Win", (screenWidth/2), (screenHeight/2), paintbrush);
                iscatMoving = false;
                isBirdMoving = false;
                isCageMoving = false;
            }
        } else {
            canvas.drawText("You Loose", (screenWidth/2), (screenHeight/2), paintbrush);
            iscatMoving = false;
            isBirdMoving = false;
            isCageMoving = false;
        }
    }


    public void outputVisibleArea() {
        Log.d(TAG, "DEBUG: The visible area of the screen is:");
        Log.d(TAG, "DEBUG: Maximum w,h = " + this.screenWidth +  "," + this.screenHeight);
        Log.d(TAG, "DEBUG: Visible w,h =" + VISIBLE_RIGHT + "," + VISIBLE_BOTTOM);
        Log.d(TAG, "-------------------------------------");
    }



    public void redrawSprites() {
        if (holder.getSurface().isValid()) {

            // initialize the canvas
            canvas = holder.lockCanvas();
            // --------------------------------

            // set the game's background color
            canvas.drawColor(Color.argb(255,255,255,255));

            // setup stroke style and width
            paintbrush.setStyle(Paint.Style.FILL);
            paintbrush.setStrokeWidth(8);

            // --------------------------------------------------------
            // draw boundaries of the visible space of app
            // --------------------------------------------------------
            paintbrush.setStyle(Paint.Style.STROKE);
            paintbrush.setColor(Color.argb(255, 0, 128, 0));

            canvas.drawRect(VISIBLE_LEFT, VISIBLE_TOP, VISIBLE_BOTTOM, VISIBLE_RIGHT, paintbrush);
            this.outputVisibleArea();

            // --------------------------------------------------------
            // draw player and sparrow
            // --------------------------------------------------------

            // 1. player
            canvas.drawBitmap(this.player.getImage(), this.player.getxPosition(), this.player.getyPosition(), paintbrush);

            // 2. sparrow
            canvas.drawBitmap(this.sparrow.getImage(), this.sparrow.getxPosition(), this.sparrow.getyPosition(), paintbrush);

            //3. cat
            canvas.drawBitmap(this.cat.getImage(), this.cat.getxPosition(), this.cat.getyPosition(), paintbrush);

            //4. Cage
            paintbrush.setStyle(Paint.Style.FILL);
            paintbrush.setColor(Color.RED);
            canvas.drawRect(cage, paintbrush);
            paintbrush.setStyle(Paint.Style.STROKE);
            paintbrush.setColor(Color.YELLOW);
            canvas.drawRect(cageHitBox, paintbrush);

            // --------------------------------------------------------
            // draw hitbox on player
            // --------------------------------------------------------
            Rect r = player.getHitbox();
            paintbrush.setStyle(Paint.Style.STROKE);
            paintbrush.setColor(Color.GREEN);
            canvas.drawRect(r, paintbrush);


            // --------------------------------------------------------
            // draw hitbox on player
            // --------------------------------------------------------
            paintbrush.setTextSize(60);
            paintbrush.setStrokeWidth(5);
            String screenInfo = "Screen size: (" + this.screenWidth + "," + this.screenHeight + ")";
            paintbrush.setColor(Color.GREEN);
            canvas.drawText(screenInfo, 30, 100, paintbrush);

            // --------------------------------

            //draw bullet
            paintbrush.setStyle(Paint.Style.STROKE);
            paintbrush.setColor(Color.BLACK);
            canvas.drawRect(this.bullet.getHitbox(), paintbrush);
            paintbrush.setStyle(Paint.Style.FILL);
            paintbrush.setColor(Color.MAGENTA);
            canvas.drawRect(this.bullet.getHitbox(), paintbrush);
            //------------------------
            holder.unlockCanvasAndPost(canvas);
        }

    }

    public int[] randcoor(){
        int x = screenWidth / 2;
        int y = screenHeight - 150;
        Random randX = new Random();
        int rx = randX.nextInt(x+1);
        Random randY = new Random();
        int ry = randY.nextInt(y+1);
        int[] coor = new int[2];
        coor[0] = rx;
        coor[1] = ry;
        return coor;
    }


    public void controlFPS() {
        try {
            gameThread.sleep(17);
        }
        catch (InterruptedException e) {

        }
    }


    int userX = 0;
    int userY = 0;
    // Deal with user input
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                userX = (int)event.getX();
                userY = (int)event.getY();
                //this.makeBullet();
                break;
            case MotionEvent.ACTION_DOWN:
                break;
        }
        return true;
    }

    // Game status - pause & resume
    public void pauseGame() {
        gameIsRunning = false;
        try {
            gameThread.join();
        }
        catch (InterruptedException e) {

        }
    }
    public void  resumeGame() {
        gameIsRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

}