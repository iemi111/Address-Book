package com.com2027.contacts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContactsListActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUESTS = 145;

    private AdapterList adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);
        requestPermission();


        registerForContextMenu(getContactsList());

        Button addContact = (Button) findViewById(R.id.contact_list_add);
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent form = new Intent(ContactsListActivity.this, ContactsFormActivity.class);
                startActivity(form);
            }
        });



    }

    private ListView getContactsList() {
        return (ListView) findViewById(R.id.contacts_list);
    }

    // Actions taken on Resume - reload contacts list from DB to refresh any changes
    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();

        getContactsList().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> contactList, View element, int position, long id) {
                Contact contact = (Contact) getContactsList().getItemAtPosition(position);
                Intent view = new Intent(ContactsListActivity.this, ContactsViewActivity.class);
                view.putExtra("contact", contact);
                startActivity(view);
            }
        });
    }

    // Load contacts from the database and display through ListView
    private void loadContacts() {
        ContactDB condb = new ContactDB(this);
        List<Contact> contacts = condb.loadAll();
        condb.close();

        //sort the contacts by name
        Collections.sort(contacts, new Comparator<Contact>() {
            public int compare(Contact m1, Contact m2) {
                return m1.getName().compareTo(m2.getName());
            }
        });

        adapter = new AdapterList(getApplicationContext(),contacts);

        //ArrayAdapter<Contact> adapter = new ArrayAdapter<Contact>(this, android.R.layout.simple_list_item_1, contacts);
        getContactsList().setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(ContactsListActivity.this, "Your contacts are displayed here. Select a contact to view. Long Click on a contact to edit or remove.", Toast.LENGTH_LONG).show();
        return super.onOptionsItemSelected(item);
    }

    // Creating menu invoked at long click on an item - to delete from list.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        // "Edit" selection method.
        if (v.equals(getContactsList())) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            final Contact contact = (Contact) getContactsList().getItemAtPosition(info.position);

            MenuItem edit = menu.add("Edit");
            MenuItem item = edit.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ContactDB db = new ContactDB(ContactsListActivity.this);
                    final Intent toEdit = new Intent(ContactsListActivity.this, ContactsFormActivity.class);

                    getContactsList().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Contact contact = (Contact) getContactsList().getItemAtPosition(position);
                        }
                    });
                    //passing the contact object as extra for form activity
                    toEdit.putExtra("contact", contact);
                    startActivity(toEdit);
                    return false;
                }
//                private MenuPresenter updateOrigContact () {
//                    Intent intent = getIntent();
//                    Contact contact = (Contact) intent.getSerializableExtra("contact");
//                    return (MenuPresenter) contact;
//                }

            });
        }

        // "Delete" selection method.
        if (v.equals(getContactsList())) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            final Contact contact = (Contact) getContactsList().getItemAtPosition(info.position);


            MenuItem delete = menu.add("Delete");
            delete.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ContactDB db = new ContactDB(ContactsListActivity.this);
                    db.delete(contact);
                    db.close();
                    loadContacts();
                    Toast.makeText(ContactsListActivity.this, "Deleted " + contact.getName() + " from contacts.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
        // "Cancel" selection method.
        if (v.equals(getContactsList())) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            final Contact contact = (Contact) getContactsList().getItemAtPosition(info.position);

            MenuItem cancel = menu.add("Cancel");
            cancel.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    loadContacts();
                    Toast.makeText(ContactsListActivity.this, "Returned to Contacts.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });

        }

    }

    // Request permission for location
    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA}, PERMISSIONS_REQUESTS);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // If requests are cancelled
            case PERMISSIONS_REQUESTS: {
              if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Unless location permission is granted, " +
                            "route to a contact's address would not be shown", Toast.LENGTH_LONG).show();
                }
                if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Unless permission is granted, selecting photos from gallery would not be allowed " +
                            "", Toast.LENGTH_LONG).show();
                }
                if (grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Unless permission is granted, taking a photo would not be allowed ", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }



}