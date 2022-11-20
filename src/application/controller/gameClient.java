package application.controller;

import application.closeAll;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.StringTokenizer;

public class gameClient implements Initializable {
    private static final int PLAY_1 = 1;
    private static final int PLAY_2 = 2;
    private static final int EMPTY = 0;
    private static final int BOUND = 90;
    private static final int OFFSET = 15;

    @FXML
    private Pane base_square;

    @FXML
    private Rectangle game_panel;

    private static boolean needWait=true;
    private static boolean TURN = false;
    private static boolean gameContinue=true;
    private static boolean receive=true;
    private static int row;
    private static int column;
    private static int currentPlayer;
    private static String info="";

    private static Socket socket;
    private static PrintWriter toServer;
    private static Scanner fromServer;

    //判断win的时候看chessboard
    private static final int[][] chessBoard = new int[3][3];
    private static final boolean[][] flag = new boolean[3][3];

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            //建立连接（本地连接）
            socket = new Socket("127.0.0.1", 7000);
        } catch (Exception e){
            System.out.println("连接服务器失败！");
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        connectWithServer();

    }

    public void connectWithServer(){
        try {
            fromServer=new Scanner(socket.getInputStream());
            toServer=new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                String line=fromServer.nextLine();
                if (line.equals("Connected:player1")) {
                    currentPlayer=PLAY_1;
                    System.out.println("你是玩家1，先手，请等待对手上线");
                    //等待用户二连接
                    fromServer.nextLine();
                    System.out.println("玩家二连接成功，请先手落子！");
                    //接收数据
                    receiveInfo();
                    TURN = true;
                } else if (line.equals("Connected:player2")) {
                    currentPlayer=PLAY_2;
                    System.out.println("你是玩家2，请等待对方下棋");
                    //接收数据
                    receiveInfo();
                }
                //游戏继续
                while (gameContinue)
                {
                    if (currentPlayer==PLAY_1)
                    {
                        moveChess();
                        waitingForPlayer();
                        //sendMove();
                        annaylize();
                    } else if (currentPlayer==PLAY_2)
                    {
                        annaylize();
                        moveChess();
                        waitingForPlayer();
                        //判断玩家是否中途退出
                       // sendMove();
                    }
                }
            }
            catch (Exception e)
            {
                release();
            }
        }).start();
    }

    public void receiveInfo(){
        new Thread(()->{
            while (gameContinue){
                int m;
                try {
                    //读第一行，数字1或2
                   m=fromServer.nextInt();
                   if (m==1){
                    //正常游戏，读第二行，获取游戏状态
                    info=fromServer.nextLine();
                    receive=false;
                    //读第三行，获取坐标，Move:x,y形式
                    String[] strarray=fromServer.nextLine().split(":");
                    //x，y形式
                    String position=strarray[1];
                    if (info.equals("OVER:Player1 win")){
                        readChess(position);
                        break;
                    } else if (info.equals("OVER:Player2 win")){
                        readChess(position);
                        break;
                    } else if (info.equals("OVER:Tie")){
                        readChess(position);
                        break;
                    } else {
                        readChess(position);
                    }
                } else if (m==2){
                    //有人退出
                    if (gameContinue){
                        System.out.println("对手已退出，你赢了");
                        TURN=false;
                        gameContinue=false;
                    }
                    if (receive){
                        receive=false;
                    }
                    if (needWait){
                        needWait=false;
                    }
                    break;
                }
            } catch (Exception e) {
                    gameContinue=false;
                    release();
                }
            }
        }).start();
    }

    public void annaylize() throws InterruptedException {
        waitingForReceive();
        if (!gameContinue){
            return;
        }
        if (info.equals("OVER:Player1 win")){
            gameContinue=false;
            needWait=false;
            toServer.println(8);
            if (currentPlayer==PLAY_1){
                System.out.println("恭喜你赢了");
            } else if (currentPlayer==PLAY_2){
                System.out.println("很遗憾你输了");
            }
        } else if (info.equals("OVER:Player2 win")){
            gameContinue=false;
            needWait=false;
            toServer.println(8);
            if (currentPlayer==PLAY_2){
                System.out.println("恭喜你赢了");
            } else if (currentPlayer==PLAY_1){
                System.out.println("很遗憾你输了");
            }
        } else if (info.equals("OVER:Tie")){
            gameContinue=false;
            needWait=false;
            System.out.println("平局");
        } else {
            TURN=true;
        }
    }

    //己方落子
    public void moveChess(){
        game_panel.setOnMouseClicked(event -> {
            int x = (int) (event.getX() / BOUND);
            int y = (int) (event.getY() / BOUND);
            if (refreshBoard(x, y)) {
                TURN = !TURN;
                send("Move"+":"+x+","+y);
            }
        });
    }

    //接受对方的旗子
    public void readChess(String position){
        StringTokenizer s = new StringTokenizer(position, ",");
        int i = Integer.parseInt(s.nextToken());
        int j = Integer.parseInt(s.nextToken());
        refreshBoard(i,j);
    }

    //等待玩家下棋
    public void waitingForPlayer() throws InterruptedException
    {
        while(needWait)
        {
            Thread.sleep(100);
        }
        needWait = true;
    }
    //等待接收数据
    public void waitingForReceive() throws InterruptedException
    {
        while(receive)
        {
            Thread.sleep(100);
        }
        receive = true;
    }

    private boolean refreshBoard (int x, int y) {
        if (chessBoard[x][y] == EMPTY) {
            //turn是对的话就在chessboard的xy位置上放1，否则就放2，turn的默认值是true,故游戏从player1开始，先画圆
            //turn是false时player2开始，turn是true是player1开始
            chessBoard[x][y] = TURN ? PLAY_1 : PLAY_2;
            drawChess();
            return true;
        }
        return false;
    }

    //刷新，遍历chessboard数组，1的位置画圆，2的位置画叉，已经画过的地方不变
    private void drawChess () {
        for (int i = 0; i < chessBoard.length; i++) {
            for (int j = 0; j < chessBoard[0].length; j++) {
                if (flag[i][j]) {
                    // This square has been drawing, ignore.
                    continue;
                }
                switch (chessBoard[i][j]) {
                    case PLAY_1:
                        drawCircle(i, j);
                        break;
                    case PLAY_2:
                        drawLine(i, j);
                        break;
                    case EMPTY:
                        // do nothing
                        break;
                    default:
                        System.err.println("Invalid value!");
                }
            }
        }
    }

    private void drawCircle (int i, int j) {
        Circle circle = new Circle();
        base_square.getChildren().add(circle);
        circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
        circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
        circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
        circle.setStroke(Color.RED);
        circle.setFill(Color.TRANSPARENT);
        flag[i][j] = true;
    }

    private void drawLine (int i, int j) {
        Line line_a = new Line();
        Line line_b = new Line();
        base_square.getChildren().add(line_a);
        base_square.getChildren().add(line_b);
        line_a.setStartX(i * BOUND + OFFSET * 1.5);
        line_a.setStartY(j * BOUND + OFFSET * 1.5);
        line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
        line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_a.setStroke(Color.BLUE);

        line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
        line_b.setStartY(j * BOUND + OFFSET * 1.5);
        line_b.setEndX(i * BOUND + OFFSET * 1.5);
        line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_b.setStroke(Color.BLUE);
        flag[i][j] = true;
    }

    public void send(String str){
        toServer.println(1);
        toServer.println("Move:"+row+","+column);
    }

    public void release(){
        gameContinue=false;
        closeAll.close(socket,fromServer,toServer);
    }
}
