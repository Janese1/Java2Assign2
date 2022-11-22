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
            OutputStream outputStream1=s1.getOutputStream();
            PrintWriter out1=new PrintWriter(outputStream1);
            //定义其为玩家1
            out1.println("Connected:player1");
            out1.flush();
            System.out.println( "Player1 connected successfully!" );


            Socket s2=server.accept();
            OutputStream outputStream2=s2.getOutputStream();
            PrintWriter out2=new PrintWriter(outputStream2);
            //定义其为玩家2
            out2.println("Connected:player2");
            out2.flush();
            System.out.println( "Player2 connected successfully!" );


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
        out1.println("You first");
        out1.flush();

        //开始游戏
        while (!exit) {
            try {
                //看是否需要等待
                waitPlayers();//等待，直到收到player1发送来的坐标
                System.out.println("玩家一落子："+x+","+y);

                chessBoard[x][y] = PLAY_1;
                //System.out.println(chessBoard[x][y]);

                if (isWin(PLAY_1)) {//player1 win
                    send(out1,"RUN");
                    send(out1,"OVER:Player1 win");
                    send(out2,"RUN");
                    send(out2,"OVER:Player1 win");
                    //向客户端2发送坐标
                    send(out2,x+","+y);

                    System.out.println("Game over! Player1 win");
                    //结束
                    exit=true;
                    break;

                } else if (isFull(chessBoard)) {//chessboard is full
                    send(out1,"RUN");
                    send(out1,"OVER:Tie");
                    send(out2,"RUN");
                    send(out2,"OVER:Tie");
                    send(out2,x+","+y);

                    System.out.println("Game over! It's a tie");
                    exit=true;
                    break;
                } else { //chessboard is not full,game continue
                    send(out2,"RUN");
                    send(out2,"Continue:Your turn");
                    //out2.println("Continue:Your turn");
                    send(out2,x+","+y);
                    //System.out.println(x);
                    System.out.println("Game continue! It's player2's turn");
                }

                waitPlayers(); //等待直到收到player2发送来的坐标
                System.out.println("玩家二落子："+x+","+y);
                //System.out.println(x+","+y);

                chessBoard[x][y] = PLAY_2;

                if (isWin(PLAY_2)) { //player2 win
                    send(out1,"RUN");
                    send(out1,"OVER:Player2 win");
                    send(out2,"RUN");
                    send(out2,"OVER:Player2 win");
                    send(out1,x+","+y);

                    System.out.println("Game over! Player2 win");
                    exit=true;
                    break;
                } else { //chessboard is not full
                    send(out1,"RUN");
                    send(out1,"Continue:Your turn");
                   // out1.println("Continue:Your turn");
                    send(out1,x+","+y);
                    System.out.println("Game continue! It's player1's turn");
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

//实现服务器与两个客户端信息交互的类
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
            while (!quit) {
                try {
                    String line = fromPlayer1.nextLine();
                    if (line.equals("RUN")) {
                        //读取坐标
                        info = fromPlayer1.nextLine();
                        //System.out.println(info);
                        StringTokenizer s = new StringTokenizer(info, ",");
                        x = Integer.parseInt(s.nextToken());
                        y = Integer.parseInt(s.nextToken());
                        //等待结束  对needwait赋值会抛出异常
                        needWait = false;
                    }
                } catch(Exception e){
                        //有玩家退出
                        System.out.println("Player has quit");
                        players.remove(player1);
                        needWait = false;
                        send(toPlayer2, "ERROR");
                        try {
                            release();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
            }
        }

        public void release() throws IOException {
            quit=true;
            closeAll.close(player1,player2,fromPlayer1,toPlayer2);
        }
    }
}

