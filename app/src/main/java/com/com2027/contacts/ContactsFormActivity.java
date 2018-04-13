package com.com2027.contacts;

import android.Manifest;
import android.app.MediaRouteButton;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ContactsFormActivity extends AppCompatActivity {

    //taking a photo using camera
    static final int REQUEST_IMAGE_CAPTURE = 3;
    //for selecting a photo from phone gallery
    private static final int REQUEST_GALLERY_PHOTO = 2;
    //private final static int MY_PERMISSION_FINE_LOCATION = 101;
    private final static int PLACE_PICKER_REQUEST = 1;
    //permission for use of camera
    private static final int PERMISSIONS_READ_EXTERNAL_STORAGE = 150;
    private static final int PERMISSIONS_CAMERA = 177;
    private boolean mPhotoPermissionGranted;
    private boolean mCameraPermissionGranted;
    //map view for place picker in London
    private final static LatLngBounds bounds = new LatLngBounds(new LatLng(51.5152192, -0.1321900), new LatLng(51.5166013, -0.1299262));
    TextView placeAddressText;
    Button getPlaceButton;
    Button takePhotoButton;
    Button selectPhotoButton;
    Button buttonSave;
    private ImageView photo;
    // Uri for photo taken with Camera
    String photoCameraUri;
    // Uri for the photo selected from gallery
    private Uri photoGalleryUri;
    private LatLng latLng;
    // the text format of the uri for the photo which is going to be stored in the Database
    private String uriForDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_form);

        photo = (ImageView) findViewById(R.id.contact_form_imageView);
        getPlaceButton = (Button) findViewById(R.id.contact_form_select_address);
        selectPhotoButton = (Button) findViewById(R.id.contact_form_select_photo);
        takePhotoButton = (Button) findViewById(R.id.contact_form_take_photo);
        placeAddressText = (TextView) findViewById(R.id.contact_form_address_text);
        buttonSave = (Button) findViewById(R.id.contact_form_save);

        getPlaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                builder.setLatLngBounds(bounds);
                try {
                    Intent intent = builder.build(ContactsFormActivity.this);
                    startActivityForResult(intent, PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();

                if (!mCameraPermissionGranted) {
                    //the app doesn't stop when we ask the permission, result is handled by onRequestPermissionsResult
                    if (ActivityCompat.shouldShowRequestPermissionRationale(ContactsFormActivity.this, Manifest.permission.CAMERA)) {
                        ActivityCompat.requestPermissions(ContactsFormActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSIONS_CAMERA);
                    }
                } else {
                    takePictureIntent();
                }

            }
        });

        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
                if (!mPhotoPermissionGranted) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(ContactsFormActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                ActivityCompat.requestPermissions(ContactsFormActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        PERMISSIONS_READ_EXTERNAL_STORAGE);
                            }
                        }
                    }
                } else {
                    selectPhotoIntent();
                }


            }
        });


        //if the user has selected the edit option for contact
        if (intentionUpdateContact()) {
            Contact contact = updateOrigContact();
            FormHelper fill = new FormHelper(this);

            //set the address text view
            placeAddressText.setText(contact.getAddress());
            //if the user has saved the address before, show it
            if (!contact.getAddress().equals("Address")) {
                placeAddressText.setVisibility(MediaRouteButton.VISIBLE);
            }


            //set the LatLng
            latLng = new LatLng(contact.getLatitude(), contact.getLongitude());

            //set the values from the saved contact
            fill.fillForm(contact);
            //check for saved photo first
            if (contact.getImage() != null) {
                //set the image
                uriForDB = contact.getImage();
                Uri uri = Uri.parse(uriForDB);
                try {
                    //set up a bitmap using it
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(ContactsFormActivity.this.getContentResolver(), uri);
                    //fix the orientation
                    if (bitmap.getHeight() < bitmap.getWidth()) {
                        photo.setRotation(90);
                    }
                    photo.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ContactsFormActivity.this, "Image could not be found", Toast.LENGTH_LONG).show();
                }
            }


        }


        buttonSave.setOnClickListener(new View.OnClickListener() {

            // Method for action at button click. Shows Toast notification.
            @Override
            public void onClick(View v) {

                //get the contact from the views
                FormHelper formHelper = new FormHelper(ContactsFormActivity.this);

                //DO NOT allow a contact to be saved without a name
                if (formHelper.getName().equals("")) {
                    //AlertDialog to warn that a contact cannot be saved without a name
                    AlertDialog.Builder builder = new AlertDialog.Builder(ContactsFormActivity.this);
                    builder.setTitle("Alert");
                    builder.setMessage("Name for the contact is required");
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alertDialog.show();

                } else if(!formHelper.getEmail().equals("")){
                    if(!Patterns.EMAIL_ADDRESS.matcher(formHelper.getEmail()).matches()){
                        //AlertDialog to warn that a contact cannot be saved without a name
                        AlertDialog.Builder builder = new AlertDialog.Builder(ContactsFormActivity.this);
                        builder.setTitle("Alert").setMessage("Email is invalid");
                        AlertDialog alertDialog = builder.create();
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        alertDialog.show();
                    }

                } else {
                    ContactDB conDB = new ContactDB(ContactsFormActivity.this);
                    if (placeAddressText.getText().equals("Address")) {
                        placeAddressText.setText("Address");
                    }
                    if (latLng == null) {
                        latLng = new LatLng(0.0, 0.0);
                    }
                    formHelper.setLatLngImage(latLng, uriForDB);
                    Contact contact = formHelper.createContact();

                    // Save the selected contact to the database
                    if (intentionUpdateContact()) {
                        conDB.update(contact, updateOrigContact().getId());
                    } else {
                        //save the new contact to the database
                        conDB.save(contact);
                    }
                    conDB.close();
                    Toast.makeText(ContactsFormActivity.this, "" + contact.getName() + " has been saved.", Toast.LENGTH_SHORT).show();
                    finish();
                }

            }
        });
    }

    //check permission for location and photo display
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCameraPermissionGranted = true;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                mPhotoPermissionGranted = true;
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPermissionGranted = true;
                    takePictureIntent();
                } else {
                    Toast.makeText(getApplicationContext(), "Unless current location is granted, " +
                            "route to this address would not be shown", Toast.LENGTH_LONG).show();
                }
                break;
            }

            case PERMISSIONS_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPhotoPermissionGranted = true;
                    selectPhotoIntent();
                }
                break;
            }
        }
    }


    /**
     * getting the selected contact to edit from the extra of the intent
     *
     * @return the selected contact
     */
    private Contact updateOrigContact() {
        Intent intent = getIntent();
        Contact contact = (Contact) intent.getSerializableExtra("contact");
        return contact;
    }

    /**
     * check whether the initiation of this activity was from clicking edit (the selected contact)
     *
     * @return
     */
    private boolean intentionUpdateContact() {
        return getIntent().hasExtra("contact");
    }

    // Method creating options menu.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact_form, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Method responding to item selected in options menu (Help Icon). Shows Toast notification.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ContactDB condb = new ContactDB(this);
        Toast.makeText(ContactsFormActivity.this, "Edit your contact details then press the Save button.", Toast.LENGTH_LONG).show();
        return super.onOptionsItemSelected(item);
    }

    //Method for taking a photo using camera intent
    private void takePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(ContactsFormActivity.this,
                        "com.com2027.contacts.FileProvider",
                        photoFile);
                uriForDB = photoURI.toString();
                //initiate the intent using a defined uri for the photo to be taken
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        mCameraPermissionGranted = true;
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        }
    }

    private void selectPhotoIntent() {
        Intent selectGalleryIntent = new Intent(Intent.ACTION_PICK);
        selectGalleryIntent.setType("image/*");
        startActivityForResult(selectGalleryIntent, REQUEST_GALLERY_PHOTO);
    }

    // Creating a unique name for the photo (from camera)
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        photoCameraUri = image.getAbsolutePath();
        return image;
    }

    //display the photo
    private void setPhoto() {
        // Get the dimensions of the View
        int viewW = photo.getWidth();
        int viewH = photo.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        //setting the size of the bitmap without allocating memory
        //so we set the dimensions
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoCameraUri, bitmapOptions);
        int photoW = bitmapOptions.outWidth;
        int photoH = bitmapOptions.outHeight;

        //set the correct rotation of the image
        if (photoH < photoW) {
            photo.setRotation(90);
        }

        // Decode the image file into a Bitmap sized to fill the View
        bitmapOptions.inJustDecodeBounds = false;
        //get the smaller version of the photo to save memory
        bitmapOptions.inSampleSize = Math.min(photoW / viewW, photoH / viewH);
        //decode ans set the image with the new dimensions
        Bitmap bitmap = BitmapFactory.decodeFile(photoCameraUri, bitmapOptions);
        photo.setImageBitmap(bitmap);
    }

    /**
     * Add the photo taken with the camera to gallery
     */
    private void galleryAddPic() {
        //Request the media scanner to scan the photo taken from the camera and add it to the media database.
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(photoGalleryUri);
        this.sendBroadcast(mediaScanIntent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(ContactsFormActivity.this, data);
                placeAddressText.setText(place.getAddress());
                placeAddressText.setVisibility(MediaRouteButton.VISIBLE);
                latLng = place.getLatLng();
            }
        }

        // image from camera
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setPhoto();
            galleryAddPic();
        }

        // image from gallery
        if (requestCode == REQUEST_GALLERY_PHOTO && resultCode == RESULT_OK) {
            //get the Uri for the photo from gallery
            photoGalleryUri = data.getData();
            uriForDB = photoGalleryUri.toString();

            try {
                //set up a bitmap using it
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(ContactsFormActivity.this.getContentResolver(), photoGalleryUri);
                //fix the orientation
                if (bitmap.getHeight() < bitmap.getWidth()) {
                    photo.setRotation(90);
                }
                photo.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(ContactsFormActivity.this, "Image path could not be found", Toast.LENGTH_LONG).show();
            }

        }
    }
}
