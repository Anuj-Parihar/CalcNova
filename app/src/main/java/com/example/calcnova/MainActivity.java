package com.example.calcnova;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView display;
    private StringBuilder currentInput = new StringBuilder();
    private Deque<String> history = new ArrayDeque<>();
    private boolean isResultDisplayed = false;

    private static final String HISTORY_PREF = "calculator_history";
    private static final int MAX_HISTORY_DAYS = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.display);
        display.setText("0");

        Button historyButton = findViewById(R.id.historyButton);
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    public void onButtonClick(View view) {
        Button button = (Button) view;
        String buttonText = button.getText().toString();

        switch (buttonText) {
            case "C":
                clearAll();
                break;
            case "CE":
                clearEntry();
                break;
            case "⌫":
                backspace();
                break;
//            case ")":
//                break;
            case "sin":
            case "cos":
            case "tan":
            case "log":
            case "ln":
            case "√":
                handleFunction(buttonText);
                break;
            case "x²":
                handleSquare();
                break;
            case "x^y":
                handlePower();
                break;

            case "=":
                calculateResult();
                break;
            case "π":
                handleConstant(buttonText);
                break;
            default:
                appendToInput(buttonText);
                break;
        }
    }

    private void appendToInput(String input) {
        if (isResultDisplayed && isNumeric(input)) {
            currentInput.setLength(0);
            isResultDisplayed = false;
        } else if (isResultDisplayed) {
            isResultDisplayed = false;
        }

        if (input.equals("(")) {
            String lastNumber = getLastNumber();
            if (lastNumber.contains(")")) {
                return;
            }
        }

        currentInput.append(input);
        display.setText(currentInput.toString());
    }

    private void handleFunction(String function) {
        if (currentInput.length() == 0) return;

        try {
            double value = Double.parseDouble(currentInput.toString());
            double result = 0;

            switch (function) {
                case "sin":
                    result = Math.sin(Math.toRadians(value));
                    break;
                case "cos":
                    result = Math.cos(Math.toRadians(value));
                    break;
                case "tan":
                    result = Math.tan(Math.toRadians(value));
                    break;
                case "log":
                    result = Math.log10(value);
                    break;
                case "ln":
                    result = Math.log(value);
                    break;
                case "√":
                    result = Math.sqrt(value);
                    break;
            }

            addToHistory(function + "(" + value + ") = " + result);
            currentInput.setLength(0);
            currentInput.append(result);
            display.setText(currentInput.toString());
            isResultDisplayed = true;
        } catch (NumberFormatException e) {
            display.setText("Error");
        }
    }

    private void handleSquare() {
        if (currentInput.length() == 0) return;

        try {
            double value = Double.parseDouble(currentInput.toString());
            double result = Math.pow(value, 2);

            addToHistory(value + "² = " + result);
            currentInput.setLength(0);
            currentInput.append(result);
            display.setText(currentInput.toString());
            isResultDisplayed = true;
        } catch (NumberFormatException e) {
            display.setText("Error");
        }
    }

    private void handlePower() {
        if (currentInput.length() == 0) return;

        char lastChar = currentInput.charAt(currentInput.length() - 1);
        if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == '(' || lastChar == 'π') {
            currentInput.append("^");
            display.setText(currentInput.toString());
        } else if (lastChar == '^') {
            return;
        } else {
            display.setText("Error");
        }
    }

    private void handleConstant(String constant) {
        if (constant.equals("π")) {
            if (currentInput.length() == 0 || isOperator(currentInput.charAt(currentInput.length() - 1))) {
                currentInput.append(Math.PI);
                display.setText(currentInput.toString());
            }
        }
    }

    private void calculateResult() {
        if (currentInput.length() == 0) return;

        try {
            String expression = currentInput.toString();
            expression = expression.replace("π", String.valueOf(Math.PI));
            expression = expression.replace("^", "**");
            expression = expression.replaceAll("(\\d+\\.?\\d*)%", "($1/100)"); // Handle percentage

            double result = eval(expression);

            if (Double.isNaN(result) || Double.isInfinite(result)) {
                display.setText("Error");
            } else {
                addToHistory(currentInput.toString() + " = " + result);
                currentInput.setLength(0);
                currentInput.append(result);
                display.setText(currentInput.toString());
                isResultDisplayed = true;
            }
        } catch (Exception e) {
            display.setText("Error");
        }
    }

    private double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) {
                        if (eat('*')) {
                            x = Math.pow(x, parseFactor());
                        } else {
                            x *= parseFactor();
                        }
                    }
                    else if (eat('/')) x /= parseFactor();
                    else if (eat('%')) x %= parseFactor(); // Modulus operation
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }
                return x;
            }
        }.parse();
    }

    private void clearAll() {
        currentInput.setLength(0);
        display.setText("0");
        isResultDisplayed = false;
    }

    private void clearEntry() {
        if (currentInput.length() > 0) {
            currentInput.setLength(0);
            display.setText("0");
            isResultDisplayed = false;
        }
    }

    private void backspace() {
        if (currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
            if (currentInput.length() == 0) {
                display.setText("0");
            } else {
                display.setText(currentInput.toString());
            }
        }
    }

    private void addToHistory(String entry) {
        history.addFirst(entry);
        if (history.size() > 50) {
            history.removeLast();
        }
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '%';
    }

    private String getLastNumber() {
        String input = currentInput.toString();
        for (int i = input.length() - 1; i >= 0; i--) {
            char c = input.charAt(i);
            if (isOperator(c)) {
                return input.substring(i + 1);
            }
        }
        return input;
    }

    private void saveHistory() {
        SharedPreferences preferences = getSharedPreferences(HISTORY_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("last_save_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        int i = 0;
        for (String entry : history) {
            editor.putString("history_" + i, entry);
            i++;
        }
        editor.putInt("history_size", history.size());
        editor.apply();
    }

    private void loadHistory() {
        SharedPreferences preferences = getSharedPreferences(HISTORY_PREF, MODE_PRIVATE);
        String lastSaveDate = preferences.getString("last_save_date", "");

        try {
            Date savedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastSaveDate);
            long diffInDays = (new Date().getTime() - savedDate.getTime()) / (1000 * 60 * 60 * 24);

            if (diffInDays <= MAX_HISTORY_DAYS) {
                int size = preferences.getInt("history_size", 0);
                for (int i = 0; i < size; i++) {
                    String entry = preferences.getString("history_" + i, "");
                    if (!entry.isEmpty()) {
                        history.addLast(entry);
                    }
                }
            }
        } catch (Exception e) {
            // If error parsing dates, don't load history
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }
}