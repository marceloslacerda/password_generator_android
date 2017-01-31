package br.com.msl09.passwordgenerator;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class ExportImportView extends AppCompatActivity {
    public static final int PERMISSIONS_REQUEST_CODE = 0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    static final String IMPORT_SUCCESS_INTENT = "br.com.msl09.passwordgenerator.IMPORT_SUCCESS_INTENT";
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static final int FILE_PICKER_REQUEST_CODE = 1;
    boolean exportMode=true;
    private String passwordJSON = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exporter_view);
        Button button = (Button) findViewById(R.id.filesystem_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryToSavePasswords();
            }
        });
        handleExportIntent();
        handleImportIntent();
    }

    private void handleImportIntent() {
        if(getIntent().getAction().equals(MainActivity.IMPORT_INTENT)) {
            checkPermissionsAndOpenFilePicker();
        }
    }

    private void handleExportIntent() {
        String passwordDatabase = (String) getIntent().getSerializableExtra(MainActivity.EXPORT_INTENT);
        if(passwordDatabase != null) {
            TextView textView = (TextView) findViewById(R.id.provider_hint_text_view);
            textView.setText(R.string.export_hint_string);
            exportMode = true;
            this.passwordJSON = passwordDatabase;
        }
    }

    private void tryToSavePasswords() {
        if (isExternalStorageWritable()) {
            verifyStoragePermissions(this);
            writeToExternalStorage(this.passwordJSON);
            showMessage(R.string.export_success);
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

    private void openFilePicker() {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(FILE_PICKER_REQUEST_CODE)
                .withHiddenFiles(true)
                .withFilter(Pattern.compile(".*\\.json$"))
                .start();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            String path = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

            if (path != null) {
                Log.d("Path: ", path);

                try {
                    String text = readTextFromUri(Uri.fromFile(new File(path)));
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra(IMPORT_SUCCESS_INTENT, text);
                    this.startActivity(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            showMessage(R.string.error_reading);
            return "";
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

    private void showMessage(@StringRes int message) {
        ActivityUtil.showMessage(findViewById(R.id.activity_exporter_view), message);
    }
}
