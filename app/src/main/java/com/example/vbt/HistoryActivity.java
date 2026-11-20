package com.example.vbt;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vbt.algorithm.history.HistoryStorage;
import com.example.vbt.algorithm.history.SavedSet;
import com.example.vbt.algorithm.history.SetFormat;
import com.example.vbt.algorithm.history.SetHistoryStore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class HistoryActivity extends AppCompatActivity {
    private SetHistoryStore store;
    private ListView listView;
    private TextView hint;
    private List<SavedSet> shown = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM HH:mm",
            Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        final int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.historyRoot),
                (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left + pad, bars.top + pad,
                    bars.right + pad, bars.bottom + pad);
            return insets;
        });

        store = HistoryStorage.open(this);
        listView = findViewById(R.id.lvHistory);
        hint = findViewById(R.id.tvHistoryHint);
        listView.setOnItemClickListener((parent, view, position, id) ->
                showDetails(shown.get(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) ->{
            confirmDelete(shown.get(position));
            return true;
        });
        reload();
    }

    private void reload(){
        List<SavedSet> all;
        try{
            all = store.loadAll();
        }catch (IOException e){
            all = new ArrayList<>();
        }
        Collections.reverse(all);
        shown = all;
        List<String> lines = new ArrayList<>();
        for(SavedSet s : shown){
            lines.add(SetFormat.summaryLine(s, dateFormat.format(new Date(s.timestampMs))));
        }
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines));
        if(shown.isEmpty()){
            hint.setText(R.string.history_empty);
        }else{
            hint.setText(R.string.history_hint);
        }
    }

    private void showDetails(SavedSet set){
        new AlertDialog.Builder(this)
                .setTitle(SetFormat.summaryLine(set, dateFormat.format(new Date(set.timestampMs))))
                .setMessage(SetFormat.details(set))
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void confirmDelete(final SavedSet set){
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_set_title)
                .setMessage(SetFormat.summaryLine(set, dateFormat.format(new Date(set.timestampMs))))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        store.delete(set.timestampMs);
                    } catch (IOException ignored) {

                    }
                    reload();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
