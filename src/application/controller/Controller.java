package application.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private static final int PLAY_1 = 1;
    private static final int PLAY_2 = 2;
    private static final int EMPTY = 0;
    private static final int BOUND = 90;
    private static final int OFFSET = 15;

    @FXML
    private Pane base_square;

    @FXML
    private Rectangle game_panel;

    private static boolean TURN = false;
//判断win的时候看chessboard
    private static final int[][] chessBoard = new int[3][3];
    private static final boolean[][] flag = new boolean[3][3];

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        game_panel.setOnMouseClicked(event -> {
            int x = (int) (event.getX() / BOUND);
            int y = (int) (event.getY() / BOUND);
            if (refreshBoard(x, y)) {
                TURN = !TURN;
            }
            Boolean win=judge(chessBoard);
        });
    }

    public boolean judge(int[][] chessBoard){
        int cnt=0; //判断棋盘满不满
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        //judge by row/column
        for (int i = 0; i < chessBoard.length; i++) {
            int circleRow=0;
            int circleColumn=0;
            int lineRow=0;
            int lineColumn=0;
            for (int j = 0; j < chessBoard[0].length; j++) {
                if (chessBoard[i][j]==PLAY_1){
                    circleRow++;
                    cnt++;
                } else if (chessBoard[i][j]==PLAY_2){
                    lineRow++;
                    cnt++;
                }
                if (chessBoard[j][i]==PLAY_1){
                    circleColumn++;
                } else if (chessBoard[j][i]==PLAY_2){
                    lineColumn++;
                }
            }
            if (circleRow==3||circleColumn==3) {
                alert.setTitle("Information Dialog");
                alert.setHeaderText("Game over!");
                alert.setContentText("Player1 win!");
                alert.showAndWait();
                return true;
            } else if (lineRow==3||lineColumn==3){
                alert.setTitle("Information Dialog");
                alert.setHeaderText("Game over!");
                alert.setContentText("Player2 win!");
                alert.showAndWait();
                return true;
            }
        }
        //judge by dialog
        if((chessBoard[0][0]==PLAY_1&chessBoard[1][1]==PLAY_1&chessBoard[2][2]==PLAY_1)
                ||(chessBoard[2][0]==PLAY_1&chessBoard[1][1]==PLAY_1&chessBoard[0][2]==PLAY_1)){
            alert.setTitle("Information Dialog");
            alert.setHeaderText("Game over!");
            alert.setContentText("Player1 win!");
            alert.showAndWait();
            return true;
        }else if((chessBoard[0][0]==PLAY_2&chessBoard[1][1]==PLAY_2&chessBoard[2][2]==PLAY_2)
                ||(chessBoard[2][0]==PLAY_2&chessBoard[1][1]==PLAY_2&chessBoard[0][2]==PLAY_2)) {
            alert.setTitle("Information Dialog");
            alert.setHeaderText("Game over!");
            alert.setContentText("Player2 win!");
            alert.showAndWait();
            return true;
        } else if (cnt==9){
            alert.setTitle("Information Dialog");
            alert.setHeaderText("Game over!");
            alert.setContentText("Full Warning!");
            alert.showAndWait();
            return true;
        }
        return true;
    }
    private boolean refreshBoard (int x, int y) {
        if (chessBoard[x][y] == EMPTY) {
            //turn是对的话就在chessboard的xy位置上放1，否则就放2，turn的默认值是false,故游戏从player2开始，先画叉
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
}
