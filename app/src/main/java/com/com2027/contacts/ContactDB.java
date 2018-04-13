package com.com2027.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ContactDB extends SQLiteOpenHelper {


    public ContactDB(Context context) {
        super(context, "Contact", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlDB = "CREATE TABLE contacts (id INTEGER PRIMARY KEY, name TEXT NOT NULL, " +
                "phone TEXT, address TEXT, email TEXT, relation TEXT, lat REAL, lng REAL, image TEXT);";
        db.execSQL(sqlDB );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void save(Contact contact) {
        ContentValues fields = contact.fillOutFields();
        SQLiteDatabase database = getWritableDatabase();
        // null - empty a completely empty row cannot be inserted
        database.insert("contacts", null, fields);
    }

    /**
     * Get the saved contacts from the database
     * @return list with all Contact objects
     */
    public List<Contact> loadAll() {
        List<Contact> contacts = new ArrayList<>();
        //read database
        SQLiteDatabase database = getReadableDatabase();
        //set cursor for each contact in database
        Cursor c = database.rawQuery("SELECT * FROM contacts ORDER BY name ASC;", null);
        //iterate through all contacts while getting the fields
        while(c.moveToNext()) {
            int id = c.getInt(c.getColumnIndex("id"));
            String name = c.getString(c.getColumnIndex("name"));
            String phone = c.getString(c.getColumnIndex("phone"));
            String address = c.getString(c.getColumnIndex("address"));
            String email = c.getString(c.getColumnIndex("email"));
            String relation = c.getString(c.getColumnIndex("relation"));
            Double latitude = c.getDouble(c.getColumnIndex("lat"));
            Double longitude = c.getDouble(c.getColumnIndex("lng"));
            String image = c.getString(c.getColumnIndex("image"));
            Contact contact = new Contact(id, name, phone, address, email, relation, latitude, longitude, image);
            contacts.add(contact);
        }
        return contacts;
    }

    public void delete(Contact contact) {
        SQLiteDatabase db = getWritableDatabase();
        String[] s = {contact.getId() + ""};
        db.delete("contacts", "id = ?", s);
    }

    public void update(Contact contact, int origId) {
        ContentValues data = contact.fillOutFields();
        SQLiteDatabase db = getWritableDatabase();
        String[] s = { origId + ""};
        db.update("contacts", data, "id = ?", s);
    }

}
