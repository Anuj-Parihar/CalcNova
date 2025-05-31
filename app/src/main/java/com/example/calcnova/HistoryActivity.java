package com.example.calcnova;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity {

    private TextView historyDisplay;
    private Button clearButton;
    private List<String> history = new ArrayList<>();

    private static final String HISTORY_PREF = "calculator_history";
    private static final String HISTORY_ENTRIES_KEY = "history_entries";
    private static final String HISTORY_SIZE_KEY = "history_size";
    private static final String HISTORY_PREFIX = "history_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyDisplay = findViewById(R.id.historyDisplay);
        clearButton = findViewById(R.id.clearButton);

        clearButton.setOnClickListener(v -> {
            deleteAllHistory();
            updateHistoryDisplay();
        });

        loadHistory();
        updateHistoryDisplay();
    }

    private void loadHistory() {
        Set<String> tempSet = new HashSet<>();
        SharedPreferences preferences = getSharedPreferences(HISTORY_PREF, MODE_PRIVATE);

        // Load from both storage methods to ensure no history is missed
        Set<String> savedSet = preferences.getStringSet(HISTORY_ENTRIES_KEY, null);
        if (savedSet != null) {
            tempSet.addAll(savedSet);
        }

        // Always check indexed storage to catch any legacy entries
        int size = preferences.getInt(HISTORY_SIZE_KEY, 0);
        for (int i = 0; i < size; i++) {
            String entry = preferences.getString(HISTORY_PREFIX + i, "");
            if (!entry.isEmpty()) {
                tempSet.add(entry);
            }
        }

        history.clear();
        history.addAll(tempSet);
    }

    private void updateHistoryDisplay() {
        if (history.isEmpty()) {
            historyDisplay.setText("No History");
        } else {
            StringBuilder historyText = new StringBuilder();
            for (String entry : history) {
                historyText.append(entry).append("\n\n");
            }
            historyDisplay.setText(historyText.toString());
        }
    }

    private void deleteAllHistory() {
        // Clear memory
        history.clear();

        // Clear all possible storage locations
        SharedPreferences preferences = getSharedPreferences(HISTORY_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // Remove set-based storage
        editor.remove(HISTORY_ENTRIES_KEY);

        // Remove indexed storage
        int size = preferences.getInt(HISTORY_SIZE_KEY, 0);
        for (int i = 0; i < size; i++) {
            editor.remove(HISTORY_PREFIX + i);
        }
        editor.remove(HISTORY_SIZE_KEY);

        // Commit changes
        editor.apply();
    }
}
