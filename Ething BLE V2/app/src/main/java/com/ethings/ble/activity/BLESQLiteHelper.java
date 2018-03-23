package com.ethings.ble.activity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ethings.ble.model.TagDevice;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dangv on 16/06/2017.
 */

public class BLESQLiteHelper extends SQLiteOpenHelper {

    public BLESQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, version);
    }


    public static final int version = 1;
    private static final String DATABASE_NAME = "bledevices.db";
    private static final String DEVICES_TABLE_NAME = "devices";
    private static final String DEVICES_COLUMN_ADDRESS = "address";
    private static final String DEVICES_COLUMN_NAME = "name";
    private static final String DEVICES_COLUMN_PIN = "pin";
    private static final String DEVICES_COLUMN_INTENSITY = "intensity";
    private static final String DEVICES_COLUMN_STATE = "state";
    private HashMap hp;

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table " + DEVICES_TABLE_NAME +
                        "(address text primary key, name text, pin integer, intensity integer, state boolean)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + DEVICES_TABLE_NAME);
        onCreate(db);
    }

    public boolean insertDevice(TagDevice device) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DEVICES_COLUMN_ADDRESS, device.getAddress());
        contentValues.put(DEVICES_COLUMN_NAME, device.getName());
        contentValues.put(DEVICES_COLUMN_PIN, device.getPin());
        contentValues.put(DEVICES_COLUMN_INTENSITY, device.getRSSI());
        int state = (device.getState()) ? 1 : 0;
        contentValues.put(DEVICES_COLUMN_STATE, state);
        db.insert(DEVICES_TABLE_NAME, null, contentValues);
        return true;
    }

    public Cursor getData(String address) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + DEVICES_TABLE_NAME + " where " + DEVICES_COLUMN_ADDRESS + " = " + address + "", null);
        return res;
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, DEVICES_TABLE_NAME);
        return numRows;
    }

    public boolean updateDevice(TagDevice device) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DEVICES_COLUMN_ADDRESS, device.getAddress());
        contentValues.put(DEVICES_COLUMN_NAME, device.getName());
        contentValues.put(DEVICES_COLUMN_PIN, device.getPin());
        contentValues.put(DEVICES_COLUMN_INTENSITY, device.getRSSI());
        int state = (device.getState()) ? 1 : 0;
        contentValues.put(DEVICES_COLUMN_STATE, state);
        db.update(DEVICES_TABLE_NAME, contentValues, DEVICES_COLUMN_ADDRESS + " = ? ", new String[]{device.getAddress()});
        return true;
    }

    public Integer deleteDevice(TagDevice device) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(DEVICES_TABLE_NAME,
                DEVICES_COLUMN_ADDRESS + " = ? ",
                new String[]{device.getAddress()});
    }

    public Integer deleteDevice(String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(DEVICES_TABLE_NAME,
                DEVICES_COLUMN_ADDRESS + " = ? ",
                new String[]{address});
    }

    public ArrayList<TagDevice> getAllDevices() {
        ArrayList<TagDevice> array_list = new ArrayList<TagDevice>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + DEVICES_TABLE_NAME, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            TagDevice device;
            String name = res.getString(res.getColumnIndexOrThrow(DEVICES_COLUMN_NAME));
            String address = res.getString(res.getColumnIndexOrThrow(DEVICES_COLUMN_ADDRESS));
            int pin = res.getInt(res.getColumnIndexOrThrow(DEVICES_COLUMN_PIN));
            int intensity = res.getInt(res.getColumnIndexOrThrow(DEVICES_COLUMN_INTENSITY));
            int numstate = res.getInt(res.getColumnIndexOrThrow(DEVICES_COLUMN_STATE));
            boolean state = (numstate == 1);
            device = new TagDevice(address, name, intensity, pin, state);
            array_list.add(device);
            res.moveToNext();
        }
        if (!res.isClosed()) {
            res.close();
        }
        return array_list;
    }
}
