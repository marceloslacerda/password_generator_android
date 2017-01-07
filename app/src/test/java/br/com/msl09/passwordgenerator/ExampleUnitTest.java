package br.com.msl09.passwordgenerator;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void remainder_negativeBytes() throws Exception {
        byte b = -10 % 11;
        assertEquals(-10, b);
    }

    @Test
    public void test_getMessage() throws Exception {
        assertArrayEquals("foo.bar123456baz".getBytes("UTF-8"),
                PasswordInfo.getMessage("foo.bar", "123456", "baz"));
    }

    @Test
    public void test_byteToPositiveInt() throws Exception {
        assertEquals(246, PasswordInfo.signedByteToPositiveInt((byte) -10));
        assertEquals(5, PasswordInfo.signedByteToPositiveInt((byte) 5));
    }

    @Test
    public void test_translate() throws Exception {
        byte[] array = {13, -10, 35, 71, 112, 56};
        String expected = "DyZ9ou!@#";
        String result = PasswordInfo.translate(array, 12, "!@#");
        assertEquals(expected, result);
    }
}