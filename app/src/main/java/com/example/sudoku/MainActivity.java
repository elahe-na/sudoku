package com.example.sudoku;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import kotlin.text.Charsets;

public class MainActivity extends AppCompatActivity {

    private Thread solvingThread = null;
    private final TextView[][] textViews = new TextView[9][9];
    private final int[][] board = new int[9][9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout main = findViewById(R.id.main);
        for (int i = 0; i < 9; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            main.addView(row);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) row.getLayoutParams();
            params.weight = 1;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = 0;
            row.setLayoutParams(params);

            for (int j = 0; j < 9; j++) {
                TextView tv = new TextView(this);
                textViews[i][j] = tv;
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,22);
//                tv.setText("5");
                row.addView(tv);
                params = (LinearLayout.LayoutParams) tv.getLayoutParams();
                params.weight = 1;
                params.width = 0;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                tv.setLayoutParams(params);
            }
        }

        String sudoku = readInputStream(getResources().openRawResource(R.raw.sample));
        setSudokuFromString(sudoku);
        showBoard();

        findViewById(R.id.btn_read_file).setOnClickListener(v -> {
            solvingThread.interrupt();
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("*/*");
            chooseFile = Intent.createChooser(chooseFile, "Choose a file");
            startActivityForResult(chooseFile, 2);
        });

        findViewById(R.id.btn_solve).setOnClickListener(v -> {
            if (solvingThread == null) {
                solveSudokuStepByStep();
            } else {
                try {
                    solvingThread.interrupt();
                } catch (Exception e) {
                } finally {
                    solvingThread = null;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            try {
                String sudoku = readInputStream(getContentResolver().openInputStream(data.getData()));
                setSudokuFromString(sudoku);
                showBoard();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void showBoard() {
        runOnUiThread(() -> {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    int valueInt = board[i][j];
                    if (valueInt > 0) {
                        textViews[i][j].setText(String.valueOf(valueInt));
                    } else {
                        textViews[i][j].setText("");
                    }

                }
            }
        });
    }

    private void solveSudokuStepByStep() {
        if (solvingThread != null) {
            return;
        }
        solvingThread = new Thread() {
            private boolean running = true;

            @Override
            public void run() {
                int[][] gridNumbersCopy = new int[9][9];
                for (int i = 0; i < 9; i++) {
                    System.arraycopy(board[i], 0, gridNumbersCopy[i], 0, 9);
                }
                if (startSolveSudoku(gridNumbersCopy)) {
                    for (Step step : steps) {
                        board[step.row][step.column] = step.number;
                        TextView textview = textViews[step.row][step.column];

                        runOnUiThread(() -> {
                            textview.setText("" + step.number);
                            textview.setBackgroundColor(Color.CYAN);
                        });
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            running = false;
                        }

                        runOnUiThread(() -> {
                            textview.setBackgroundColor(Color.TRANSPARENT);
                        });
                        if (!running) {
                            break;
                        }
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "this sudoku is not solvable", Toast.LENGTH_LONG).show());
                }
                solvingThread = null;
            }
        };
        solvingThread.start();
    }

    private static class Step {
        public int row, column, number;

        public Step(int row, int column, int number) {
            this.row = row;
            this.column = column;
            this.number = number;
        }
    }

    private ArrayList<Step> steps = new ArrayList<>();

    private boolean startSolveSudoku(int grid[][]) {
        steps.clear();
        return solveSudoku(grid, 0, 0);
    }

    private boolean solveSudoku(int[][] grid, int row, int col) {
        if (row == 9 - 1 && col == 9)
            return true;

        if (col == 9) {
            row++;
            col = 0;
        }

        if (grid[row][col] != 0)
            return solveSudoku(grid, row, col + 1);

        for (int num = 1; num < 10; num++) {
            if (isSafe(grid, row, col, num)) {
                grid[row][col] = num;

                if (solveSudoku(grid, row, col + 1)) {
                    steps.add(new Step(row, col, num));
                    return true;
                }
            }
            grid[row][col] = 0;
        }
        return false;
    }

    private boolean isSafe(int[][] grid, int row, int col, int num) {
        for (int x = 0; x <= 8; x++)
            if (grid[row][x] == num)
                return false;
        for (int x = 0; x <= 8; x++)
            if (grid[x][col] == num)
                return false;

        int startRow = row - row % 3, startCol = col - col % 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (grid[i + startRow][j + startCol] == num)
                    return false;
        return true;
    }


    private String readInputStream(InputStream is) {
        StringBuilder out = new StringBuilder();
        try {
            int bufferSize = 1024;
            char[] buffer = new char[bufferSize];
            Reader in = new InputStreamReader(is, Charsets.UTF_8);
            for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                out.append(buffer, 0, numRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    private void setSudokuFromString(String s) {
        String[] lines = s.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] values = line.split("\\s*,\\s*");
            for (int j = 0; j < values.length; j++) {
                board[i][j] = Integer.parseInt(values[j].trim());
            }
        }
    }
}