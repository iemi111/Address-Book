package com.com2027.contacts;

import android.content.ContentValues;

import java.io.Serializable;

public class Contact implements Serializable {

    private int id;
    private final String name;
    private final String phone;
    private final String address;
    private final String email;
    private final String relation;
    private final Double latitude;
    private final Double longitude;
    private final String image;



    public Contact(String name, String phone, String address, String email, String relation, Double latitude, Double longitude, String image) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.email = email;
        this.relation = relation;
        this.latitude = latitude;
        this.longitude = longitude;
        this.image = image;
    }

    public Contact(int id, String name, String phone, String address, String email, String relation, Double latitude, Double longitude, String image) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.email = email;
        this.relation = relation;
        this.latitude = latitude;
        this.longitude = longitude;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public String getPhone() {
        return phone;
    }
    public String getAddress() {
        return address;
    }
    public String getEmail() {
        return email;
    }
    public String getRelation() {
        return relation;
    }
    public Double getLatitude() {
        return latitude;
    }
    public Double getLongitude() {
        return longitude;
    }
    public String getImage() {
        return image;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Helper Method for saving a contact to the database.
     * The order of the fields have to be the same as the column names in the database.
     * @return ContentValues which is used for insert method in the DB
     */
    public ContentValues fillOutFields() {
        ContentValues fields = new ContentValues();
//        fields.put("id", id);
        fields.put("name", name);
        fields.put("phone", phone);
        fields.put("address", address);
        fields.put("email", email);
        fields.put("relation", relation);
        fields.put("lat", latitude);
        fields.put("lng", longitude);
        fields.put("image", image);
        return fields;
    }

}
