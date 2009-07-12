package com.elsdoerfer.android.autostarts;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Due to stupid Android API limitations with respect to querying the
 * intent filters of a disabled receiver, we need to keep a list of
 * changed components ourselves.
 *
 * To be precise, we currently:
 * 		- Add a component to the cache when it is disabled.
 *      - Remove a component from the cache when it is found to no
 *        longer exist.
 *      - Remove a component from the cache when it is enabled again.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Represent a receiver for a single action.
     *
     * If a receiver supports multiple actions, multiple cached entries /
     * instances of this class might exist (for the same component).
     */
    static class ReceiverData implements Comparable<ReceiverData> {
        // These identify the component
        public String packageName;
        public String componentName;
        // This is the data we can't access in case of disabled
        // components, and for which we essentially do this whole
        // caching ordeal.
        public String action;
        public int priority;

        // This is the runtime data queried from the package
        // manager. We only load this when needed.
        public ActivityInfo activityInfo;
        public Boolean enabled;

        /**
         * Initialize runtime data.
         */
        public void init(PackageManager pm) throws NameNotFoundException {
            activityInfo = pm.getReceiverInfo(
                    new ComponentName(packageName, componentName),
                    PackageManager.GET_DISABLED_COMPONENTS);
            enabled = ListActivity.isComponentEnabled(pm, this);
        }

        @Override
        public String toString() {
            return String.format("%s/%s for %s",
                    packageName, componentName, action);
        }

        @Override
        public boolean equals(Object o) {
            // The code merging the cached components with those found in
            // recovery relies on this method.
            // See also hashCode(), of course.
            if (!(o instanceof ReceiverData))
                return false;
            return (((ReceiverData)o).componentName.equals(componentName) &&
                    ((ReceiverData)o).packageName.equals(packageName));
        }

        @Override
        public int hashCode() {
            return (packageName+componentName).hashCode();
        }

		public int compareTo(ReceiverData another) {
			int result = ((Integer)priority).compareTo(((ReceiverData)another).priority);
			if (result != 0)
				return result;
			else
				return componentName.compareToIgnoreCase(
						((ReceiverData)another).componentName);
		}
    }


    static final String DATABASE_NAME = "common.db";
    static final int DATABASE_VERSION = 2;

    static final String TABLE_CHANGED = "changed";
    static final String FIELD_PACKAGE_NAME = "package";
    static final String FIELD_COMPONENT_NAME = "component";
    static final String FIELD_ACTION = "action";
    static final String FIELD_PRIORITY = "priority";

    static final String[] CHANGED_PROJECTION = {
        FIELD_PACKAGE_NAME, FIELD_COMPONENT_NAME, FIELD_ACTION,
        FIELD_PRIORITY,
    };
    static final int INDEX_PACKAGE_NAME = 0;
    static final int INDEX_COMPONENT_NAME = 1;
    static final int INDEX_ACTION = 2;
    static final int INDEX_PRIORITY = 3;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CHANGED + " (" +
                FIELD_PACKAGE_NAME + " TEXT NOT NULL," +
                FIELD_COMPONENT_NAME + " TEXT NOT NULL," +
                FIELD_ACTION + " TEXT NOT NULL," +
                FIELD_PRIORITY + " INTEGER NULL" +
        ");");
        db.execSQL("CREATE UNIQUE INDEX idx_unique ON " +
                TABLE_CHANGED + " (" + FIELD_PACKAGE_NAME +
                ", "+ FIELD_COMPONENT_NAME + ", "+ FIELD_ACTION +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_CHANGED);
        onCreate(db);
    }

    /**
     * Return all cached components.
     */
    public ReceiverData[] getCachedComponents() {
        Cursor c = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            c = db.query(TABLE_CHANGED, CHANGED_PROJECTION,
                    null, null, null, null, null, null);

            if (c != null && c.moveToFirst()) {
                ReceiverData[] result = new ReceiverData[c.getCount()];

                do {
                    ReceiverData r = new ReceiverData();
                    r.packageName = c.getString(INDEX_PACKAGE_NAME);
                    r.componentName = c.getString(INDEX_COMPONENT_NAME);
                    r.action = c.getString(INDEX_ACTION);
                    r.priority = c.getInt(INDEX_PRIORITY);
                    result[c.getPosition()] = r;
                    c.moveToNext();
                }
                while (!c.isAfterLast());

                return result;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return null;
    }

    /**
     * Add a component to the cache.
     */
    public void cacheComponent(ReceiverData component) {
        ContentValues values = new ContentValues();
        values.put(FIELD_PACKAGE_NAME, component.packageName);
        values.put(FIELD_COMPONENT_NAME, component.componentName);
        values.put(FIELD_ACTION, component.action);
        values.put(FIELD_PRIORITY, component.priority);

        SQLiteDatabase db = getWritableDatabase();
        db.replace(TABLE_CHANGED, null, values);
    }

    /**
     * Delete a component from the cache.
     */
    public void uncacheComponent(ReceiverData c) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CHANGED,
                FIELD_PACKAGE_NAME+"=? AND "+FIELD_COMPONENT_NAME+"=? AND "+
                FIELD_ACTION+"=?", new String[] {
        			c.packageName, c.componentName, c.action });
    }

}
