package br.com.msl09.passwordgenerator;

import android.util.Base64;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.tls.DefaultTlsAgreementCredentials;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Created by msl09 on 18/12/16.
 */

public class PasswordInfo implements Serializable {
    public String hostname;
    public String user;
    public String salt;
    public Integer length;
    public String symbols;
    public final Integer version = 1;
    public static final int DEFAULT_PASSES = 150000;
    public static final int DEFAULT_SIZE = 16;
    public static final String ALPHANUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz";
    public static final int SALT_LENGTH = 32;

    public  PasswordInfo() {
        this.hostname = "";
        this.user = "";
        this.salt = Base64.encodeToString(getNewSalt(), Base64.DEFAULT);
        this.length = DEFAULT_SIZE;
        this.symbols = "";
    }

    private static byte[] getNewSalt() {
        byte[] b = new byte[SALT_LENGTH];
        new Random().nextBytes(b);
        return b;
    }

    public static JSONObject fromMapToJSON(Map<String, PasswordInfo> map){
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, PasswordInfo> entry : map.entrySet()) {
            try {
                jsonObject.put(entry.getKey(), entry.getValue().toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
                System.err.println("Should never be hit");
            }
        }
        return jsonObject;
    }

    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", this.user);
            jsonObject.put("hostname", this.hostname);
            jsonObject.put("length", this.length);
            jsonObject.put("symbols", this.symbols);
            jsonObject.put("salt", this.salt);
            jsonObject.put("version", this.version);
        } catch (JSONException e) {
            e.printStackTrace();
            System.err.println("Failed to serialize info with hostname:\n" + this.hostname);
        }
            return jsonObject;
    }

    public static PasswordInfo fromJSON(String key, JSONObject jsonObject) {
        PasswordInfo passwordInfo = new PasswordInfo();
        try {
            passwordInfo.user = jsonObject.getString("id");
            try {
                passwordInfo.hostname = jsonObject.getString("hostname");
            } catch (JSONException e) {
                System.err.println("Probably old format, attempting to convert.");
                passwordInfo.hostname = key.split(passwordInfo.user)[0];
            }
            passwordInfo.symbols = jsonObject.getString("symbols");
            passwordInfo.length = jsonObject.getInt("length");
            passwordInfo.salt = jsonObject.getString("salt");
        } catch (JSONException e) {
            e.printStackTrace();
            System.err.println("Malformed json:\n" + jsonObject);
        }
        return passwordInfo;
    }

    public static Map<String, PasswordInfo> fromJSONToMap(JSONObject jsonObject) {
        Iterator<String> keys = jsonObject.keys();
        Map<String, PasswordInfo> map = new TreeMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject object = jsonObject.optJSONObject(key);
            map.put(key, fromJSON(key, object));
        }
        return map;
    }

    public String key() {
        return hostname + user;
    }

    public static byte[] getEncryptedMessage(byte[] message, byte[] salt, int iterations, int derivedKeyLength) {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(message, salt, iterations);
        KeyParameter derivedKey = (KeyParameter) gen.generateDerivedParameters(derivedKeyLength * 8);
        return derivedKey.getKey();
    }

    public static String translate(byte[] message, int length, String symbols) {
        StringBuffer decoded = new StringBuffer();
        for (byte code : message) {
            int tableIndex = signedByteToPositiveInt(code) % ALPHANUM.length();
            decoded.append(ALPHANUM.charAt(tableIndex));
        }
        /*python and java handle substrings differently so the min is necessary*/
        int substringLength = Math.min(decoded.length(), length - symbols.length());
        return decoded.toString().substring(0, substringLength) + symbols;
    }

    public static byte[] getMessage(String hostname, String masterPassword, String user) throws UnsupportedEncodingException {
        return (hostname + masterPassword + user).getBytes("UTF-8");
    }

    public static int signedByteToPositiveInt(byte b) {
        /*because python uses unsigned bytes and java usese signed, complement-2 bytes*/
        if (b >= 0) return b;
        else return 256 + b;
    }

    public static String getPassword(String masterPassword, PasswordInfo passwordInfo) throws UnsupportedEncodingException {
        byte[] message = getMessage(passwordInfo.hostname, masterPassword, passwordInfo.user);
        byte[] code = getEncryptedMessage(message, passwordInfo.getSalt(), DEFAULT_PASSES, passwordInfo.length);
        return translate(code, passwordInfo.length, passwordInfo.symbols);
    }

    public byte[] getSalt() {
        return Base64.decode(salt, Base64.DEFAULT);
    }
}
