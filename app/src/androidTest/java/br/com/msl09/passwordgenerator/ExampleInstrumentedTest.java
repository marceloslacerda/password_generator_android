package br.com.msl09.passwordgenerator;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("br.com.msl09.passwordgenerator", appContext.getPackageName());
    }

    @Test
    public void test_getPassword() throws Exception {
        PasswordInfo passwordInfo = new PasswordInfo();
        passwordInfo.hostname = "foo.bar";
        passwordInfo.length = 12;
        passwordInfo.symbols = "#!@";
        passwordInfo.user = "baz";
        passwordInfo.salt = MainActivity.getExampleSalt();
        assertEquals(PasswordInfo.getPassword("123456", passwordInfo), "6dMX5kmw4#!@");

    }
}
