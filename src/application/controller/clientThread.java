package application.controller;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class clientThread extends Thread{
    private Socket c;
    private PrintWriter out;
    private Scanner in;
    private int currentPlayer;
    private Alert alert = new Alert(Alert.AlertType.INFORMATION);
    public clientThread(Socket c){
        this.c=c;
        if (c!=null){
            try {
                out=new PrintWriter(c.getOutputStream());
                in=new Scanner(c.getInputStream());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void run(){
        String line=null;
        while (true){
            try {
                line=in.nextLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
            String[] strArray=line.split(":");
            String sign=strArray[0];
            String info;
            if (sign.equals("Connected")){
                if (strArray[1].equals("player1")){
                    System.out.println("You are player1");
                } else if (strArray[1].equals("player2")){
                    System.out.println("You are player2");
                }
            }
        }
    }
    public void chessmove(){

    }
}
