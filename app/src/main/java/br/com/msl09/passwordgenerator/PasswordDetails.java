package br.com.msl09.passwordgenerator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class PasswordDetails extends AppCompatActivity {
    EditText hostnameField;
    EditText usernameField;
    EditText extraField;
    EditText lengthField;
    EditText masterField;
    TextView saltLabel;
    TextView generatedPasswordLabel;
    private PasswordInfo passwordInfo;
    public static String SAVE_LIST = "br.com.msl09.passwordgenerator.SHOWPASSWORD";

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
                saltLabel.setText(passwordInfo.salt.substring(0, 3) + "...");
            }
        });

        passwordInfo = (PasswordInfo) getIntent().getSerializableExtra(MainActivity.EXTRA_MESSAGE);
        setText(hostnameField, passwordInfo.hostname);
        setText(usernameField, passwordInfo.user);
        setText(extraField, passwordInfo.symbols);
        setText(lengthField, passwordInfo.length.toString());
        setText(masterField, "");
        saltLabel.setText(passwordInfo.salt.substring(0, 3));
        generatedPasswordLabel.setText("");

    }

    private void setText(EditText view, String text) {
        view.setText(text, TextView.BufferType.EDITABLE);
    }

    private void sendToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        passwordInfo.user = usernameField.getText().toString();
        passwordInfo.hostname = hostnameField.getText().toString();
        passwordInfo.symbols = extraField.getText().toString();
        try {
            passwordInfo.length = new Integer(lengthField.getText().toString());
        } catch (NumberFormatException ex) {
            Snackbar.make(findViewById(R.id.main_coordinator), R.string.length_format_error,
                    Snackbar.LENGTH_SHORT)
                    .show();
        }
        // salt is updated in place
        passwordInfo.hostname = hostnameField.getText().toString();
        intent.putExtra(SAVE_LIST, passwordInfo);
        this.startActivity(intent);
    }

    public void clickGenerateButton(View view) {
        PasswordTuple tuple = new PasswordTuple();
        tuple.master = masterField.getText().toString();
        tuple.passwordInfo = passwordInfo;
        new CalculatePasswordTask().execute(tuple);
    }

    public class PasswordTuple {
        public String master;
        public PasswordInfo passwordInfo;
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
            generatedPasswordLabel.setText(result.substring(0, 3) + "...");
        }
    }

}
