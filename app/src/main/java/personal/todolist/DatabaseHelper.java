package personal.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "todoList.db";
    private static final int DATABASE_VERSION = 8;
    private static final String TABLE_NAME = "todoList";
    private static final String COL_ID = "_id";
    private static final String COL_ITEM = "item";
    private static final String COL_COMPLETED = "completed";

    private static final String CREATE_TABLE = "CREATE TABLE todoList (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "item TEXT, " +
            "completed INTEGER DEFAULT 0, " +
            "archived INTEGER DEFAULT 0)"; // New column for archived status
    // New column for completed status

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insert(String item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ITEM, item);
        cv.put(COL_COMPLETED, 0); // default to not completed
        return db.insert(TABLE_NAME, null, cv);
    }

    public void updateCompletedStatus(String item, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_COMPLETED, isCompleted ? 1 : 0);
        db.update(TABLE_NAME, values, COL_ITEM + " = ?", new String[]{item});
    }

    public int delete(String item) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, COL_ITEM + "=?", new String[]{item});
    }

    public void archiveItem(String item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("archived", 1); // Set archived to 1 to mark as archived
        db.update("todoList", values, "item = ?", new String[]{item});
    }


    public Cursor readAll() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, new String[]{COL_ID, COL_ITEM, COL_COMPLETED}, null, null, null, null, null);
    }
}
