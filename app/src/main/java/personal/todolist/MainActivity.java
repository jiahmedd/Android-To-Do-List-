package personal.todolist;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private SimpleCursorAdapter myAdapter;
    private Cursor c;

    private ListView listView;
    private EditText elem;

    private static final String[] all_columns = { "_id", "item", "completed" };
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize
        listView = findViewById(R.id.myList);
        elem = findViewById(R.id.input);
        dbHelper = new DatabaseHelper(this);

        loadDatabase();
    }

    // Open and read database
    @Override
    protected void onResume() {
        super.onResume();
        loadDatabase();
    }

    // Close database and cursor
    @Override
    protected void onStop() {
        super.onStop();
        if (c != null) {
            c.close();
        }
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    // Remove done items
    public void clearEdit(View v) {
        if (c != null && c.moveToFirst()) {
            do {
                int itemIndex = c.getColumnIndex("item");
                int completedIndex = c.getColumnIndex("completed");
                if (itemIndex != -1 && completedIndex != -1 && c.getInt(completedIndex) == 1) {
                    String itemText = c.getString(itemIndex);
                    dbHelper.archiveItem(itemText);
                }
            } while (c.moveToNext());
            loadDatabase(); // Refresh the list to exclude archived items
        }
    }

    public void toggleArchivedView(View view) {
        ListView archivedList = findViewById(R.id.archivedList);
        Button archiveButton = findViewById(R.id.archive); // Reference to the button

        if (archivedList.getVisibility() == View.GONE) {
            loadArchivedItems(); // Load archived items into the list
            archivedList.setVisibility(View.VISIBLE); // Show archived list
            archiveButton.setText("Close Completed Items List"); // Change button text
        } else {
            archivedList.setVisibility(View.GONE); // Hide archived list
            archiveButton.setText("View Completed Items"); // Revert button text
        }
    }


    public void loadArchivedItems() {
        executorService.execute(() -> {
            Cursor archivedData = db.query("todoList", all_columns, "archived = 1", null, null, null, "_id");

            mainHandler.post(() -> {
                if (archivedData != null) {
                    SimpleCursorAdapter archivedAdapter = new SimpleCursorAdapter(
                            getApplicationContext(),
                            R.layout.list_item, // Use the same item layout
                            archivedData,
                            new String[]{"item"},
                            new int[]{R.id.item_text},
                            0
                    );
                    ListView archivedList = findViewById(R.id.archivedList);
                    archivedList.setAdapter(archivedAdapter);
                }
            });
        });
    }




    // Add new list item to database
    public void addElem(View v) {
        String input = elem.getText().toString();
        if (!input.isEmpty()) {
            dbHelper.insert(input);
            Toast.makeText(getApplicationContext(), "Adding " + input, Toast.LENGTH_SHORT).show();
            elem.setText("");
            loadDatabase();
        } else {
            Toast.makeText(getApplicationContext(), "Not adding an empty item", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to load the database asynchronously
    private void loadDatabase() {
        executorService.execute(() -> {
            db = dbHelper.getWritableDatabase();
            Cursor data = db.query("todoList", all_columns, "archived = 0", null, null, null, "_id");

            mainHandler.post(() -> {
                if (data != null) {
                    myAdapter = new SimpleCursorAdapter(
                            getApplicationContext(),
                            R.layout.list_item,
                            data,
                            new String[]{"item"},
                            new int[]{R.id.item_text},
                            0
                    ) {
                        @Override
                        public void bindView(View view, android.content.Context context, Cursor cursor) {
                            super.bindView(view, context, cursor);

                            CheckBox checkBox = view.findViewById(R.id.item_checkbox);
                            TextView textView = view.findViewById(R.id.item_text);

                            int itemIndex = cursor.getColumnIndex("item");
                            int completedIndex = cursor.getColumnIndex("completed");

                            if (itemIndex >= 0 && completedIndex >= 0) {
                                String itemText = cursor.getString(itemIndex);
                                boolean isCompleted = cursor.getInt(completedIndex) == 1;

                                textView.setText(itemText);
                                checkBox.setOnCheckedChangeListener(null); // Temporarily remove listener
                                checkBox.setChecked(isCompleted);

                                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                    dbHelper.updateCompletedStatus(itemText, isChecked);
                                    loadDatabase(); // Refresh the list after updating the status
                                });
                            }
                        }
                    };
                    c = data;
                    listView.setAdapter(myAdapter);
                }
            });
        });
    }

}
