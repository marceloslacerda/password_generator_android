package br.com.msl09.passwordgenerator;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ExportImportView extends AppCompatActivity {
    boolean exportMode=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exporter_view);
        Button button = (Button) findViewById(R.id.filesystem_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: call dropbox filepicker and export
            }
        });
        handleExportIntent();
    }

    private void handleExportIntent() {
        String passwordDatabase = (String) getIntent().getSerializableExtra(MainActivity.EXPORT_INTENT);
        if(passwordDatabase != null) {
            TextView textView = (TextView) findViewById(R.id.provider_hint_text_view);
            textView.setText(R.string.export_hint_string);
            exportMode = true;
        }
    }
}
