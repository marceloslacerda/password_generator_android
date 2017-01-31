package br.com.msl09.passwordgenerator;

import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * Created by msl09 on 30/01/17.
 */

public class ActivityUtil {
    public static void showMessage(View view, @StringRes int message) {
        Snackbar.make(view, message,
                Snackbar.LENGTH_SHORT)
                .show();
    }
}
