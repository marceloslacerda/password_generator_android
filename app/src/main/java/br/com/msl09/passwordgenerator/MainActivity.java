package br.com.msl09.passwordgenerator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    public static final int PERMISSIONS_REQUEST_CODE = 0;
    public static final int FILE_PICKER_REQUEST_CODE = 1;
    public static String EXTRA_MESSAGE = "br.com.msl09.passwordgenerator.SHOW_PASSWORD";
    private Map<String, PasswordInfo> passwords = new TreeMap<>();
    private String INITIAL_ENTRY = ("{\n" +
            "          \"first.entryyour-user\" : {\n" +
            "            \"salt\": \"" + getExampleSalt() + "\",\n" +
            "            \"length\": 12,\n" +
            "            \"symbols\": \"!@#$\",\n" +
            "            \"hostname\": \"first.entry\",\n" +
            "            \"id\": \"your-user\"\n" +
            "          }\n" +
            "        }");
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPassword(new PasswordInfo());
            }
        });
        mergeJSON(getSavedPasswords());
        handleSavePasswordIntent();
        handleDeletePasswordIntent();
        regenPasswordList();
    }

    private void handleDeletePasswordIntent() {
        String passwordKey = (String) getIntent().getSerializableExtra(PasswordDetails.DELETE_PASSWORD);
        if (passwordKey != null) {
            this.passwords.remove(passwordKey);
            savePasswords(PasswordInfo.fromMapToJSON(this.passwords).toString());
        }
    }

    private void handleSavePasswordIntent() {
        PasswordInfo passwordInfo = (PasswordInfo) getIntent().getSerializableExtra(PasswordDetails.SAVE_LIST);
        if (passwordInfo != null) {
            this.passwords.put(passwordInfo.key(), passwordInfo);
            savePasswords(PasswordInfo.fromMapToJSON(this.passwords).toString());
            showMessage(R.string.password_saved);
        }
    }

    private void regenPasswordList() {
        ViewGroup vg = (ViewGroup) findViewById(R.id.list_container);
        vg.removeAllViews();
        for (Map.Entry<String, PasswordInfo> entry : this.passwords.entrySet()) {
            Button button = new Button(this);
            final PasswordInfo passwordInfo = entry.getValue();
            button.setText(passwordInfo.hostname);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showPassword(passwordInfo);
                }
            });
            vg.addView(button);
        }
    }

    private void mergeJSON(String passwordsText) {
        try {
            JSONObject jsonObject = new JSONObject(passwordsText);
            this.passwords.putAll(PasswordInfo.fromJSONToMap(jsonObject));
        } catch (JSONException e) {
            System.err.println("Error parsing password list:\n" + passwordsText);
            e.printStackTrace();
        }
        savePasswords(PasswordInfo.fromMapToJSON(this.passwords).toString());
    }

    private void savePasswords(String s) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.password_setting_key), s);
        editor.apply();
    }

    private String getSavedPasswords() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString(getString(R.string.password_setting_key), INITIAL_ENTRY);

    }

    private void showPassword(PasswordInfo passwordInfo) {
        Intent intent = new Intent(this, PasswordDetails.class);
        intent.putExtra(EXTRA_MESSAGE, passwordInfo);
        this.startActivity(intent);

    }

    public static String getExampleSalt() {
        String saltString = "salt";
        try {
            byte[] bytes = saltString.getBytes("UTF-8");
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;
            */
            case R.id.action_import:
                checkPermissionsAndOpenFilePicker();
                return true;

            case R.id.action_export:
                tryToSavePasswords();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void tryToSavePasswords() {
        if (isExternalStorageWritable()) {
            verifyStoragePermissions(this);
            writeToExternalStorage(PasswordInfo.fromMapToJSON(this.passwords).toString());
        } else {
            showMessage(R.string.export_error);
        }

    }

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity the activity calling this method
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void writeToExternalStorage(String data) {
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, ".pinfo.json");

        try {
            // Make sure the downloads directory exists.
            path.mkdirs();

            OutputStream os = new FileOutputStream(file);
            os.write(data.getBytes());
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            showMessage(R.string.export_error);
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }


    private void checkPermissionsAndOpenFilePicker() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showMessage(R.string.storage_permission_error);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            openFilePicker();
        }
    }

    private void showMessage(@StringRes int message) {
        Snackbar.make(findViewById(R.id.main_coordinator), message,
                Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                } else {
                    showMessage(R.string.storage_permission_error);
                }
            }
        }
    }

    private void openFilePicker() {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(FILE_PICKER_REQUEST_CODE)
                .withHiddenFiles(true)
                .withFilter(Pattern.compile(".*\\.json$"))
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            String path = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

            if (path != null) {
                Log.d("Path: ", path);
                String text = "file not found";
                try {
                    text = readTextFromUri(Uri.fromFile(new File(path)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mergeJSON(text);
                regenPasswordList();
                Snackbar.make(findViewById(R.id.main_coordinator), "Successfully merged the database",
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            showMessage(R.string.error_reading);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        return stringBuilder.toString();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_scrolling, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
