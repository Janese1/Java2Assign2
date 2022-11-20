package application.server;

import application.closeAll;
import application.controller.TicTacToeConstants;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

public class gameServer implements TicTacToeConstants {
    public static void main(String[] args) throws IOException {
        //服务器端口8000
        ServerSocket server = new ServerSocket(7000);
        System.out.println( "Waiting for clients to connect..." );
        while (true) {
            Socket s1 = server.accept();
            System.out.println( "Player1 connected successfully!" );
            OutputStream outputStream1=s1.getOutputStream();
            PrintWriter out1=new PrintWriter(outputStream1);
            //定义其为玩家1
            out1.println("Connected:player1");

            Socket s2=server.accept();
            System.out.println( "Player2 connected successfully!" );
            OutputStream outputStream2=s2.getOutputStream();
            PrintWriter out2=new PrintWriter(outputStream2);
            //定义其为玩家2
            out2.println("Connected:player2");

            //每两个玩家开启一局新游戏
            serverThread st=new serverThread(s1,s2);
            st.start();
        }
    }
}

//每两个socket开启一局新游戏
class serverThread extends Thread implements TicTacToeConstants {
    //存储每个在线玩家的信息
    private ArrayList<Socket> players = new ArrayList<>();

    private Socket s1;
    private Socket s2;
    private PrintWriter out1;
    private PrintWriter out2;
    private Scanner in1;
    private Scanner in2;

    //棋子定位
    private int x;
    private int y;
    //是否需要等待另一个玩家操作
    private boolean needWait = true;
    //游戏是否结束
    private boolean exit = false;

    private boolean continueGame = true;
    //棋盘
    private static final int[][] chessBoard = new int[3][3];

    public serverThread(Socket s1, Socket s2) {
        //构造方法，初始化一个空棋盘
        this.s1 = s1;
        this.s2 = s2;
        players.add(s1);
        players.add(s2);
        try {
            out1 = new PrintWriter(s1.getOutputStream());
            out2 = new PrintWriter(s2.getOutputStream());
            in1 = new Scanner(s1.getInputStream());
            in2 = new Scanner(s2.getInputStream());
        } catch (IOException e) {
            System.out.println("xxxx");
            release();
        }

        new Thread(new exchangeInfo(s1,s2)).start();
        new Thread(new exchangeInfo(s2,s1)).start();

        for (int[] ints : chessBoard) {
            Arrays.fill(ints, EMPTY);
        }
    }

    public void waitPlayers() throws InterruptedException {
        while (needWait) {
            Thread.sleep(100);
        }
        needWait = true;
    }

    //实现信息的交互
    public void run() {

        //告诉玩家一先开始游戏
        //out1.println("You first");
        //out1.flush();

        //开始游戏
        while (!exit) {
            try {
                //看是否需要等待
                waitPlayers();
                chessBoard[x][y] = PLAY_1;

                if (isWin(PLAY_1)) {//player1 win
                    out1.println(1);
                    out1.println("OVER:Player1 win");
                    out2.println(1);
                    out2.println("OVER:Player2 win");
                    //向客户端2发送坐标
                    send(out2,"Move:"+x+","+y);
                    //结束
                    exit=true;
                    break;

                } else if (isFull(chessBoard)) {//chessboard is full
                    out1.println(1);
                    out1.println("OVER:Tie");
                    out1.println(1);
                    out2.println("Over:Tie");
                    send(out2,"Move:"+x+","+y);
                    exit=true;
                    break;
                } else { //chessboard is not full,game continue
                    out2.println(1);
                    out2.println("Continue:Your turn");
                    send(out2,"Move:"+x+","+y);
                }
                waitPlayers();
                chessBoard[x][y] = PLAY_2;

                if (isWin(PLAY_2)) { //player2 win
                    out1.println(1);
                    out1.println("Over:Player2 win");
                    out1.println(1);
                    out2.println("Over:Player2 win");
                    send(out1,"Move:"+x+","+y);
                    exit=true;
                    break;
                } else { //chessboard is not full
                    out1.println("Continue:Your turn");
                    send(out1,"Move:"+x+","+y);
                }
            } catch(Exception e){
                release();
            }
        }
    }

    private boolean isWin(int a) {
        for (int i = 0; i < 3; i++) {
            if ((chessBoard[i][0] == a) && (chessBoard[i][1] == a) && (chessBoard[i][2] == a)) {
                return true;
            }
        }
        for (int j = 0; j < 3; j++) {
            if ((chessBoard[0][j] == a) && (chessBoard[1][j] == a) && (chessBoard[2][j] == a)) {
                return true;
            }
        }
        if ((chessBoard[0][0] == a) && (chessBoard[1][1] == a) && (chessBoard[2][2] == a)) {
            return true;
        }
        if ((chessBoard[0][2] == a) && (chessBoard[1][1] == a) && (chessBoard[2][0] == a)) {
            return true;
        }
        return false;
    }

    private boolean isFull(int[][] chessBoard) {
        //判断棋盘上有没有棋子
        int cnt = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (chessBoard[i][j] == PLAY_1 || chessBoard[i][j] == PLAY_2) {
                    cnt++;
                }
            }
        }
        return cnt == 9;
    }

    public void send(PrintWriter pr,String str){
        pr.println(str);
        pr.flush();
    }
    public void release(){
        exit=true;
        closeAll.close(s1,s2,out1,out2);
    }

//实现两个客户端信息交互的类
    class exchangeInfo implements Runnable{
        private Socket player1;
        private Socket player2;
        private String sign="";
        private String info="";
        private boolean quit;
        private PrintWriter toPlayer2;
        private Scanner fromPlayer1;

        public exchangeInfo(Socket player1,Socket player2){
            this.player1=player1;
            this.player2=player2;
            try {
                toPlayer2=new PrintWriter(player2.getOutputStream());
                fromPlayer1=new Scanner(player1.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            try {
                while (!quit){
                    String line=fromPlayer1.nextLine();
                    String[] strArray=line.split(":");
                    sign=strArray[0];
                    info=strArray[1];
                    if (sign.equals("Move")){
                        //读取坐标
                        needWait=false;
                        StringTokenizer s = new StringTokenizer(info, ",");
                        x= Integer.parseInt(s.nextToken());
                        y = Integer.parseInt(s.nextToken());

                    }
                }
            } catch (Exception e) {
                //有玩家退出
                System.out.println("Player has quited");
                players.remove(player1);
                needWait=false;
                toPlayer2.println(2);
                try {
                    release();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public void release() throws IOException {
            quit=true;
            closeAll.close(player1,player2,fromPlayer1,toPlayer2);
        }
    }
}

