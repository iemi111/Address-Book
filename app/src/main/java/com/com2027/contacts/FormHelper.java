package com.com2027.contacts;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

public class FormHelper {

    private final Activity activity;
    private LatLng latLngAddress;
    private String imageUri;

    public FormHelper(Activity activity) {
        this.activity = activity;
    }

    // Generalise getters for fields and tie them to the View
    private String getText(int fieldId) {
        EditText field = (EditText) activity.findViewById(fieldId);
        String text = field.getText().toString();
        return text;
    }

    // Create a new Contact Object
    public Contact createContact() {
        return new Contact(getName(), getPhone(), getAddress(), getEmail(), getRelation(), latLngAddress.latitude,
                latLngAddress.longitude, imageUri);
    }

    public String getName() {
        int field = R.id.contact_form_name;
        return getText(field);
    }
    public String getPhone() {
        int field = R.id.contact_form_phone;
        return getText(field);
    }
    public String getEmail() {
        int field = R.id.contact_form_email;
        return getText(field);
    }
    public String getRelation() {
        int field = R.id.contact_form_relation;
        return getText(field);
    }
    public String getAddress(){
        TextView field = (TextView) activity.findViewById(R.id.contact_form_address_text);
        return field.getText().toString();
    }

    public void setLatLngImage(LatLng latLng, String image){
        this.latLngAddress = latLng;
        this.imageUri = image;
    }

    /**
     * The edit version of FormActivity
     * Set the current fields from the previously saved contact as editable form fields
     * without address, LatLng and image
     * @param contact from db
     */
    public void fillForm(Contact contact) {
        formFill(R.id.contact_form_name, contact.getName());
        formFill(R.id.contact_form_phone, contact.getPhone());
        formFill(R.id.contact_form_email, contact.getEmail());
        formFill(R.id.contact_form_relation, contact.getRelation());

    }
    private void formFill(int id, String value) {
        EditText field = (EditText) activity.findViewById(id);
        field.setText((value));
    }

    /**
     * Used in ContactsViewActivity, where each contact can be viewed
     * @param contact from db
     */
    public void fillViewForm(Contact contact) {
        formViewFill(R.id.contact_view_name, contact.getName());
        formViewFill(R.id.contact_view_phone, contact.getPhone());
        formViewFill(R.id.contact_view_address, contact.getAddress());
        formViewFill(R.id.contact_view_email, contact.getEmail());
        formViewFill(R.id.contact_view_relation, contact.getRelation());

    }

    private void formViewFill(int id, String value) {
        TextView field = (TextView) activity.findViewById(id);
        field.setText((value));
    }
}
