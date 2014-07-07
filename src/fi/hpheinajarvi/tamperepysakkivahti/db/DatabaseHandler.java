package fi.hpheinajarvi.tamperepysakkivahti.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import fi.hpheinajarvi.tamperepysakkivahti.model.Stop;
import fi.hpheinajarvi.tamperepysakkivahti.model.StopCache;
import fi.hpheinajarvi.tamperepysakkivahti.model.StopSuggestion;

public class DatabaseHandler extends SQLiteOpenHelper{
	private static final String TAG = "Tampere-Pysakkivahti";	// Log tag
	private static final int UPDATE_THRESHOLD = 60 * 5;
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "trepysakkivahti";
	
	/* Cache */
	private static final String CACHE_TABLE_NAME = "stopcache";
	private static final String CACHE_KEY_ID = "id";
	private static final String CACHE_KEY_STOP = "stop";
	private static final String CACHE_KEY_DATA = "data";
	private static final String CACHE_KEY_TIMESTAMP = "timestamp";
	
	private static final String CREATE_TABLE_STOPCACHE = "CREATE TABLE " + CACHE_TABLE_NAME + "(" +
			CACHE_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			CACHE_KEY_STOP + " TEXT," + 
			CACHE_KEY_DATA + " TEXT," +
			CACHE_KEY_TIMESTAMP + " NUMERIC)";
	
	/* Stops */
	private static final String STOP_TABLE_NAME = "stops";
	private static final String STOP_KEY_ID = "id";
	private static final String STOP_KEY_STOP = "stop";
	private static final String STOP_KEY_NAME = "name";
	private static final String STOP_KEY_ALIAS = "alias";
	private static final String STOP_KEY_WEIGHT = "weight";
	
	private static final String CREATE_TABLE_STOPS = "CREATE TABLE " + STOP_TABLE_NAME + "(" +
			STOP_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			STOP_KEY_STOP + " TEXT," + 
			STOP_KEY_NAME + " TEXT," +
			STOP_KEY_ALIAS + " TEXT," +
			STOP_KEY_WEIGHT + " NUMERIC)";
	
	public DatabaseHandler(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_STOPCACHE);
		db.execSQL(CREATE_TABLE_STOPS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	public void addCache(StopCache cache) {
		SQLiteDatabase db = getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(CACHE_KEY_STOP, cache.getStop());
		values.put(CACHE_KEY_DATA, cache.getData());
		values.put(CACHE_KEY_TIMESTAMP, cache.getTimestamp());
		
		try {
			Cursor c = db.query(CACHE_TABLE_NAME, new String[] {CACHE_KEY_ID}, CACHE_KEY_STOP + "=?", new String[] {cache.getStop()}, null, null, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				int oldCacheId = c.getInt(0);
				Log.i(TAG, cache.getStop() + " cache update " + oldCacheId);
				db.update(CACHE_TABLE_NAME, values, CACHE_KEY_ID + "=?", new String[] { String.valueOf(oldCacheId)});
			} else {
				Log.i(TAG, cache.getStop() + " cache insert");
				db.insert(CACHE_TABLE_NAME, null, values);
			}
		} catch (SQLiteException sqle) {
			sqle.printStackTrace();
		}

		db.close();
	}
	
	public String getCache(String stop) {
		String data = "";
		
		SQLiteDatabase db = getWritableDatabase();
		long updateTime = (System.currentTimeMillis() / 1000) - UPDATE_THRESHOLD;
		try {
			Cursor c = db.query(CACHE_TABLE_NAME, new String[] {CACHE_KEY_DATA}, "(" + CACHE_KEY_STOP + "=?" + " AND " + CACHE_KEY_TIMESTAMP + ">=?)", new String[] {stop, String.valueOf(updateTime)}, null, null, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				Log.i(TAG, stop + " cache hit");
				data = c.getString(0);
			}
			db.close();
		} catch (SQLiteException sqle) {
			sqle.printStackTrace();
		}
			
		return data;
	}
	
	public void addStop(Stop stop) {
		SQLiteDatabase db = getWritableDatabase();
		
		Cursor c = db.query(STOP_TABLE_NAME, new String[] {STOP_KEY_ID}, null, null, null, null, null);
		int weight = c.getCount();
		
		ContentValues values = new ContentValues();
		values.put(STOP_KEY_STOP, stop.getStop());
		values.put(STOP_KEY_NAME, stop.getName());
		values.put(STOP_KEY_ALIAS, stop.getAlias());
		values.put(STOP_KEY_WEIGHT, weight);
		
		db.insert(STOP_TABLE_NAME, null, values);

		db.close();
	}
	
	public void addStop(StopSuggestion stopSuggestion) {
		SQLiteDatabase db = getWritableDatabase();
		
		Cursor c = db.query(STOP_TABLE_NAME, new String[] {STOP_KEY_ID}, null, null, null, null, null);
		int weight = c.getCount();
		
		ContentValues values = new ContentValues();
		values.put(STOP_KEY_STOP, stopSuggestion.getCode());
		values.put(STOP_KEY_NAME, stopSuggestion.getName());
		values.put(STOP_KEY_ALIAS, stopSuggestion.getName());
		values.put(STOP_KEY_WEIGHT, weight);
		
		db.insert(STOP_TABLE_NAME, null, values);

		db.close();
	}
	
	public void deleteStop(String stop) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(STOP_TABLE_NAME, STOP_KEY_STOP + "=?", new String[] {stop});
		db.close();
	}
	
	public void renameStop(Stop stop, String alias) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		if (alias.equalsIgnoreCase("") || alias.equalsIgnoreCase(stop.getName())) {
			values.put(STOP_KEY_ALIAS, stop.getName());
		} else {
			values.put(STOP_KEY_ALIAS, alias);
		}
		db.update(STOP_TABLE_NAME, values, STOP_KEY_ID + "=?", new String[] { String.valueOf(stop.getId())});
		db.close();
	}
	
	public boolean stopExists(StopSuggestion stopSuggestion) {
		boolean exists = false;
		SQLiteDatabase db = getWritableDatabase();

		Cursor c = db.query(STOP_TABLE_NAME, new String[] {STOP_KEY_ID}, "(" + STOP_KEY_STOP + "=?)", new String[] {stopSuggestion.getCode()}, null, null, null);
		if (c.getCount() > 0) {
			exists = true;
		}
		
		db.close();
		
		return exists;
	}
	
	public Stop getStop(int id) {
		Stop stop = null;
		
		SQLiteDatabase db = getWritableDatabase();
		Cursor c = db.query(STOP_TABLE_NAME, new String[] {STOP_KEY_ID, STOP_KEY_STOP, STOP_KEY_NAME, STOP_KEY_ALIAS, STOP_KEY_WEIGHT}, STOP_KEY_ID + "=?", new String[] {String.valueOf(id)}, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			stop = new Stop();
			stop.setId(Integer.parseInt(c.getString(0)));
			stop.setStop(c.getString(1));
			stop.setName(c.getString(2));
			stop.setAlias(c.getString(3));
			try {
				stop.setWeight(Integer.parseInt(c.getString(4)));
			} catch (NumberFormatException nfe) {
				stop.setWeight(0);
			}			
		}
		db.close();
		
		return stop;
	}
	
	public ArrayList<Stop> getStops() {
		ArrayList<Stop> stops = new ArrayList<Stop>();
		SQLiteDatabase db = getWritableDatabase();

		Cursor c = db.query(STOP_TABLE_NAME, new String[] {STOP_KEY_ID, STOP_KEY_STOP, STOP_KEY_NAME, STOP_KEY_ALIAS, STOP_KEY_WEIGHT}, null, null, null, null, STOP_KEY_WEIGHT + " ASC, " + STOP_KEY_ALIAS + " ASC");
		while (c.moveToNext() != false) {
			Stop stop = new Stop();
			stop.setId(Integer.parseInt(c.getString(0)));
			stop.setStop(c.getString(1));
			stop.setName(c.getString(2));
			stop.setAlias(c.getString(3));
			try {
				stop.setWeight(Integer.parseInt(c.getString(4)));
			} catch (NumberFormatException nfe) {
				stop.setWeight(0);
			}
			
			stops.add(stop);
		}
		db.close();
		return stops;
	}
	
	public void updateWeight(int id, int weight) {
		ContentValues values = new ContentValues();
		values.put(STOP_KEY_WEIGHT, weight);
		SQLiteDatabase db = getWritableDatabase();
		db.update(STOP_TABLE_NAME, values, STOP_KEY_ID + "=?", new String[] { String.valueOf(id)});
		db.close();
	}
}
