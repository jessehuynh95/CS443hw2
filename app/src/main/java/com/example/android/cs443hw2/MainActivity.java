package com.example.android.cs443hw2;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class MainActivity extends Activity {

    GridView gridView;

    private static int w=5,curx,cury;
    //the current position of the player
    private static int current;
    private static int next;
    //tracks the position of the next random treasure
    int position1;

    //tracks collected treasures and number of moves made
    int collected;
    int moved;

    //tracks the number of current treasures on the board
    int treasureCount = 0;
    Boolean status = true;

    //declaring the notification manager variable and notification channel
    private NotificationManagerCompat notificationManager;
    public static final String CHANNEL_1_ID = "channel1";

    private Random r=new Random();

    static String[] tiles = new String[w*w];


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialize the notification manager so we can send notifications
        notificationManager = NotificationManagerCompat.from(this);

        //creates the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(CHANNEL_1_ID, "channel 1", NotificationManager.IMPORTANCE_HIGH);
            channel1.setDescription("this is Channel 1");


            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);

        }



        setContentView(R.layout.activity_main);

        gridView = (GridView) findViewById(R.id.gridView1);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.list_item, tiles);

        gridView.setAdapter(adapter);

        init();
        //starts the program with one treasure placed
        newTreasure();
        //creates a new treasurePlacement thread and starts it
        treasurePlacement thread = new treasurePlacement();
        thread.start();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                //this time is set to delay the movement of the player position so it does not look like
                //it is teleporting to the new position.
                int time = 300;
                //sets the current position to a variable
                int currentpos = current;

                if(status == true) {
                    next = position;
                    //checks to make sure the position clicked is not the current position
                    while (position != currentpos) {
                        //this is the change in position based on a 25 space grid. +5/-5 to move up and down, +1/-1 to move right and left.
                        //calls threadStarter with the specified movement number to update the position of the player.

                        int change = -1;
                        //moves the player up one space
                        if ((position / 5) > (currentpos / 5)) {
                            threadStarter(5, time);
                            change = 5;
                            //moves the player down one space
                        } else if ((position / 5) < (currentpos / 5)) {
                            threadStarter(-5, time);
                            change = -5;
                            //moves the character right one space
                        } else if (position > currentpos) {
                            threadStarter(1, time);
                            change = 1;
                            //else moves the character left one space.
                        } else
                            threadStarter(1, time);
                            time += 300;
                            currentpos += change;
                    }
                }
                else {

                    Toast.makeText(getApplicationContext(),
                            (CharSequence) (new Integer(position).toString()),
                            Toast.LENGTH_SHORT).show();
                }

            }


        });

    }

    void randomize() {
        //assigns the curx and cury variables to a random space as determined by random variable r
        //and generates this into a random position on the board.
        curx = r.nextInt(w);
        cury = r.nextInt(w);
        position1 = cury * w + curx;
    }

    void newTreasure() {
        //randomizes the values of curx and cury to find a random place to place a treasure
        randomize();
        //checks if the position already has a placed treasure or not
        //while there is a placed treasure in the specified position,
        //randomizes the position again continues to do so until finding an empty position.
        while (tiles[position1] != " ") {
            randomize();
        }
        //checks if the number of treasures is less than the limit, in this case I set it to 9.
        //if the count is less than 8, it will simply add another. If it is equal to 8,
        //it will add another and send a notification saying that the limit is reached.
        if (treasureCount < 8) {
            tiles[position1] = "X";
            treasureCount++;
        } else if (treasureCount == 8) {
            tiles[position1] = "X";
            treasureCount++;

            //creates the notification and sends it.
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                    .setSmallIcon(R.drawable.ic_one)
                    .setContentTitle("Max treasures reached!")
                    .setContentText("There are now 9 treasures! Collect them now!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .build();

            notificationManager.notify(1, notification);


        }
        ((ArrayAdapter)gridView.getAdapter()).notifyDataSetChanged();

    }


    //Handler to proceed with placing treasures. called by the thread and then runs the newTreasure() function
    //sets the random placement and assigns the X to the spot.
    public Handler treasureUpdater = new Handler() {
        public void handleMessage (android.os.Message message) {
            newTreasure();
        }
    };

    //this thread handles placing the new treasures on the board
    public class treasurePlacement extends Thread {
        //this sets the interval between placements to about 2 seconds
        private static final int interval = 2000;

        public treasurePlacement () {
            super("treasurePlacement");
        }
        public void run() {
            try {
                while (true) {
                    //causes the thread to sleep for the length of the delay then calls the handler
                    Thread.sleep(interval);
                    treasureUpdater.sendEmptyMessage(0);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    };

    public void threadStarter(int movement, int time) {
        status = false;
        final TextView cell = (TextView) findViewById(R.id.textCell);
        final TextView treasure = (TextView) findViewById(R.id.textTreasure);
        final int movement2 = movement;
        final int delaymovement = time;

        Thread t = new Thread ( new Runnable() {
        @Override
        public void run() {
            //causes a slight delay in the movement of the character piece, so it does not look like it is teleporting.
           SystemClock.sleep(delaymovement);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //sets the current tile (tile prior to click) to blank
                    tiles[current] = " ";
                    //if the tile clicked contains a treasure, increment collected counter and decrement total treasure counter.
                    if (tiles[current + movement2] == "X") {
                        collected++;
                        treasureCount--;
                    }
                    //sets the clicked location to "O" to indicate the player being there
                    tiles[current + movement2] = "O";
                    //increments the number of cells moved.
                    moved++;

                    //updates the textviews to show treasures collected and cells moved
                    cell.setText(String.valueOf(moved) + " Cells ");
                    treasure.setText(String.valueOf(collected) + " Treasures");
                    ((ArrayAdapter) gridView.getAdapter()).notifyDataSetChanged();

                    //sets current to the new current position by adding the movement to it.
                    current += movement2;
                    //checks to make sure the current position is equal to the next position after making the move.
                    if (current == next)
                        status = true;
                }});
        }});
        //starts the thread
        t.start();
    }

    public void reset(View view){
        // resets all counters to zero
        treasureCount = 0;
        collected = 0;
        moved = 0;
        //initializes the game, spawns the first treasure.
        init();
        newTreasure();

        //resets the collected displays to show zero.
        TextView cell = (TextView) findViewById(R.id.textCell);
        TextView treasure = (TextView) findViewById(R.id.textTreasure);
        cell.setText(String.valueOf(moved) + " Cells ");
        treasure.setText(String.valueOf(collected) + " Treasures");
    }

    void init(){
        for(int i=0;i<tiles.length;i++) tiles[i]=" ";

        curx=r.nextInt(w);
        cury=r.nextInt(w);

        tiles[cury*w+curx]="O";
        //determines the current position
        current = cury * w + curx;

        ((ArrayAdapter)gridView.getAdapter()).notifyDataSetChanged();
    }




}
