package br.com.msl09.passwordgenerator;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class PasswordDetails extends AppCompatActivity {
    public static final String DELETE_PASSWORD = "br.com.msl09.passwordgenerator.DELETE_PASSWORD";
    public static final String SAVE_LIST = "br.com.msl09.passwordgenerator.SHOW_PASSWORD";
    EditText hostnameField;
    EditText usernameField;
    EditText extraField;
    EditText lengthField;
    EditText masterField;
    TextView saltLabel;
    TextView generatedPasswordLabel;
    private PasswordInfo passwordInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_details);
        hostnameField = (EditText) findViewById(R.id.hostname);
        usernameField = (EditText) findViewById(R.id.username);
        extraField = (EditText) findViewById(R.id.extra_symbols);
        lengthField = (EditText) findViewById(R.id.length);
        masterField = (EditText) findViewById(R.id.master_password);
        saltLabel = (TextView) findViewById(R.id.salt);
        generatedPasswordLabel = (TextView) findViewById(R.id.generated_password);

        Button saveButton = (Button) findViewById(R.id.save_password_button);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToMainActivity();
            }
        });

        Button saltButton = (Button) findViewById(R.id.regenerate_salt_button);

        saltButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordInfo.genNewSalt();
                saltLabel.setText(shortenText(passwordInfo.salt));
            }
        });

        Button deleteButton = (Button) findViewById(R.id.delete_password_button);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePassword();
            }
        });

        passwordInfo = (PasswordInfo) getIntent().getSerializableExtra(MainActivity.EXTRA_MESSAGE);
        setText(hostnameField, passwordInfo.hostname);
        setText(usernameField, passwordInfo.user);
        setText(extraField, passwordInfo.symbols);
        setText(lengthField, passwordInfo.length.toString());
        setText(masterField, "");
        saltLabel.setText(shortenText(passwordInfo.salt));
        generatedPasswordLabel.setText("");

    }

    private void deletePassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.delete_password)
                .setTitle(R.string.are_you_sure_delete)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sendDeleteIntent();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Nothing to do
                    }
                })
                .show();
    }

    private void sendDeleteIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(DELETE_PASSWORD, passwordInfo.key());
        this.startActivity(intent);
    }

    @NonNull
    private String shortenText(String string) {
        if (string.length() > 3) {
            return string.substring(0, 3) + "…";
        } else {
            return string;
        }
    }

    private void setText(EditText view, String text) {
        view.setText(text, TextView.BufferType.EDITABLE);
    }

    private void sendToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        updatePasswordInfo();
        intent.putExtra(SAVE_LIST, passwordInfo);
        this.startActivity(intent);
    }

    private void updatePasswordInfo() {
        passwordInfo.user = usernameField.getText().toString();
        passwordInfo.hostname = hostnameField.getText().toString();
        passwordInfo.symbols = extraField.getText().toString();
        try {
            passwordInfo.length = Integer.valueOf(lengthField.getText().toString());
        } catch (NumberFormatException ex) {
            Snackbar.make(findViewById(R.id.activity_password_details), R.string.length_format_error,
                    Snackbar.LENGTH_SHORT)
                    .show();
        }
        // salt is updated in place
        passwordInfo.hostname = hostnameField.getText().toString();
    }

    public void clickGenerateButton(View view) {
        PasswordTuple tuple = new PasswordTuple();
        tuple.master = masterField.getText().toString();
        updatePasswordInfo();
        tuple.passwordInfo = passwordInfo;
        new CalculatePasswordTask().execute(tuple);
    }

    public class PasswordTuple {
        String master;
        PasswordInfo passwordInfo;
    }

    private class CalculatePasswordTask extends AsyncTask<PasswordTuple, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Button genButton = (Button) findViewById(R.id.generate_password_button);
            genButton.setText(R.string.wait);
            genButton.setEnabled(false);
            Context context = getApplicationContext();
            Toast toast = Toast.makeText(context, R.string.calculatingPassword, Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        protected String doInBackground(PasswordTuple... passwordTuples) {
            PasswordTuple passwordTuple = passwordTuples[0];
            String generatedPassword = "";
            try {
                generatedPassword = PasswordInfo.getPassword(
                        passwordTuple.master,
                        passwordTuple.passwordInfo);
            } catch (UnsupportedEncodingException e) {
                System.out.println("Some field(s) could not be converted to UTF-8");
                e.printStackTrace();
            }
            return generatedPassword;
        }

        @Override
        protected void onPostExecute(String result) {
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, R.string.copyNotification, duration);
            toast.show();
            Button genButton = (Button) findViewById(R.id.generate_password_button);
            genButton.setText(R.string.copy);
            genButton.setEnabled(true);
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("simple text", result);
            clipboardManager.setPrimaryClip(clip);
            generatedPasswordLabel.setText(shortenText(result));
        }
    }

}
