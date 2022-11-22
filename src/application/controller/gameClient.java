package application.controller;

import application.closeAll;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
            System.out.println("Failed to connect to the server");
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        //System.out.println("巴拉巴拉");
        //System.out.println(socket);
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
                //System.out.println("巴拉巴拉");
                //while (fromServer.hasNext()) {
                //System.out.println(fromServer.nextLine());

                    String line = fromServer.nextLine();
                    System.out.println(line);
                    if (line.equals("Connected:player1")) {
                        currentPlayer = PLAY_1;
                        System.out.println("You are player1, please wait player to connect");

                        //等待用户二连接
                        System.out.println(fromServer.nextLine());

                        //fromServer.nextLine();
                            System.out.println("Game Start");
                            System.out.println("Player2 has connected, please settle your piece firstly");
                            //接收数据
                            receiveInfo();//副线程
                            TURN = true;//玩家一可以开始落子了
                    } else if (line.equals("Connected:player2")) {
                        currentPlayer = PLAY_2;
                        System.out.println("Game start");
                        System.out.println("You are player2, please wait player1 to settle");
                        //接收数据
                        receiveInfo();
                    }
                    //游戏继续
                    while (gameContinue) {
                        if (currentPlayer == PLAY_1) {
                            moveChess();//己方落子
                            while (needWait){
                                Thread.sleep(100);
                            }
                            needWait=true;
                            send();
                            analyze();
                            needWait=true;
                        } else if (currentPlayer == PLAY_2) {
                           //receiveInfo();
                            analyze();
                            needWait=true;
                            moveChess();
                            while (needWait){
                                Thread.sleep(100);
                            }
                            needWait=true;
                            //waitingForPlayer();
                            send();
                        }
                    }
                }
            catch(Exception e)
                {
                    System.out.println("Server Exception!");
                    release();
                }
       }).start();
    }

    //接收数据，主要是把表明游戏状态的信息变为info储存，传递给annaylize方法，并在棋盘同步对方棋子
    //只要游戏还在继续，就一直读数据（while循环）
    public void receiveInfo(){
        new Thread(()->{
            while (gameContinue){
                String sign;
                try {
                    //读第一行，RUN或者ERROR
                   sign=fromServer.nextLine();
                   //System.out.println(sign);

                   if (sign.equals("RUN")){

                       //正常游戏，读第二行，获取游戏状态
                       info=fromServer.nextLine();
                       //System.out.println(info);

                       // 得到了info，可以不用再等待接收信息，analyze可以运行
                       receive=false;

                    //读第三行，获取坐标，x,y形式
                    String position=fromServer.nextLine();
                   // System.out.println(position);

                    if (info.equals("OVER:Player1 win")){

                        if (currentPlayer==PLAY_2) {
                            readChess(position);
                        }
                        //receive=false;//接收数据完毕，可以停止等待
                        break;
                    } else if (info.equals("OVER:Player2 win")){

                        if (currentPlayer==PLAY_1) {
                            readChess(position);
                        }
                        //receive=false;
                        break;
                    } else if (info.equals("OVER:Tie")){

                        if (currentPlayer==PLAY_2) {
                            readChess(position);
                        }
                        //receive=false;
                        break;
                    } else {
                        readChess(position);
                        needWait=false;
                        //System.out.println("readchess功能正常");
                        //receive=false;
                        //TURN=true;//成功同步对方的坐标，己方可以再次落子
                    }
                } else if (sign.equals("ERROR")){
                    //有人退出
                    if (gameContinue){
                        System.out.println("The other has quit, you win");
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
                    System.out.println("Server Exception!");
                    gameContinue=false;
                    release();
                }
             }
         }).start();
    }

    //接收完数据(棋盘上同步了棋子)后，根据数据发出提示语
   public void analyze() throws InterruptedException {
       waitingForReceive();
        if (!gameContinue){
            return;
        }
        if (info.equals("OVER:Player1 win")){
            gameContinue=false;
            needWait=false;
            if (currentPlayer==PLAY_1){
                System.out.println("Congratulations! You win the game");
            } else if (currentPlayer==PLAY_2){
                System.out.println("Sorry! You lost the game");
            }
        } else if (info.equals("OVER:Player2 win")){
            gameContinue=false;
            needWait=false;
            if (currentPlayer==PLAY_2){
                System.out.println("Congratulations! You win the game");
            } else if (currentPlayer==PLAY_1){
                System.out.println("Sorry! You lost the game");
            }
        } else if (info.equals("OVER:Tie")){
            gameContinue=false;
            needWait=false;
            System.out.println("平局");
        } else {
            TURN=true;//轮到己方下棋
        }
    }

    //己方落子，并在棋盘上显示
    public void moveChess() {
        //turn是对的时，己方才可以落子
            game_panel.setOnMouseClicked(event -> {
                if (TURN) {
                    row = (int) (event.getX() / BOUND);
                    column = (int) (event.getY() / BOUND);

                    if (refreshBoard(row, column, currentPlayer)) {
                        //落子成功
                        System.out.println();
                        System.out.println("Your piece:"+row+","+column+" Please wait opposite to settle");
                        //落完一次子后不能再落子
                        TURN = false;
                        needWait = false;

                    }
                } else {
                    return;
                }
            });
        }


    //接受对方的旗子坐标，并在棋盘上画出来
    public void readChess(String position) throws InterruptedException {
        Platform.runLater(()->{
                    StringTokenizer s = new StringTokenizer(position, ",");
                    int i = Integer.parseInt(s.nextToken());
                    int j = Integer.parseInt(s.nextToken());

                    //System.out.println("对方落子"+i+"|"+j);

                    //画棋子
                    if (currentPlayer==PLAY_1){
                        chessBoard[i][j]=PLAY_2;
                        drawChess();
                        //System.out.println(refreshBoard(i,j,PLAY_2));
                        System.out.println("Opponent's piece:"+i+","+j+" Please settle a piece");
                    }
                    else if (currentPlayer==PLAY_2){
                        chessBoard[i][j]=PLAY_1;
                        drawChess();
                        //System.out.println(refreshBoard(i,j,PLAY_1));
                        System.out.println("Opponent's piece:"+i+","+j+" Please settle a piece");
                    }
                });
    }

   /* //等待玩家(己方)下棋 有问题
    public void waitingForPlayer() throws InterruptedException
    {
        while(needWait)
        {
            //阻止主线程
            Thread.sleep(100);
        }
        needWait = true;
    }*/

    //等待接收数据 有问题
    public void waitingForReceive() throws InterruptedException
    {
        while(receive)
        {
            //阻止主线程
            Thread.sleep(100);
        }
        receive = true;
    }

    //给服务器发信息
    public void send(){
        //toServer.println(1);
        toServer.println("RUN");
        toServer.flush();
        toServer.println(row+","+column);
        toServer.flush();
    }

    //关闭资源
    public void release(){
        gameContinue=false;
        closeAll.close(socket,fromServer,toServer);
    }

    //刷新棋盘，把棋子画出来
    private boolean refreshBoard (int x, int y, int z) {
        if (chessBoard[x][y] == EMPTY) {
            chessBoard[x][y]=z;
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

}
