package com.example.private_photo;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public class PPEncryption {
    public interface PasswordProviderResultListener {
        void onPassword(@Nullable String password);
    }
    private final static String PWD_ENCRYPTION_KEY_ALIAS = BuildConfig.APPLICATION_ID + ".pwdKey";

    public static String encryptPassword(String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null); // Keystore must be loaded before it can be accessed
        Certificate cert = keyStore.getCertificate(PWD_ENCRYPTION_KEY_ALIAS);
        PublicKey key;
        if (cert == null) {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyGenerator.initialize(
                    new KeyGenParameterSpec.Builder(PWD_ENCRYPTION_KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT |
                                    KeyProperties.PURPOSE_DECRYPT)
                            .setDigests(KeyProperties.DIGEST_SHA256,
                                    KeyProperties.DIGEST_SHA512)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                            .setUserAuthenticationRequired(true)
                            .build());
            keyGenerator.generateKeyPair();
        }
        cert = keyStore.getCertificate(PWD_ENCRYPTION_KEY_ALIAS);
        key = cert.getPublicKey();
        PublicKey unrestrictedPublicKey = KeyFactory.getInstance(key.getAlgorithm()).generatePublic(
                new X509EncodedKeySpec(key.getEncoded()));
        OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, unrestrictedPublicKey, spec);

        byte[] encryptedPassword = cipher.doFinal(password.getBytes());

        byte[] base64EncodedPassword = Base64.getEncoder().encode(encryptedPassword);
        return new String(base64EncodedPassword);
    }

    public static void decryptPassword(String encryptedPassword, Context context, PasswordProviderResultListener listener) throws Exception {
        Executor executor = ContextCompat.getMainExecutor(context);
        BiometricPrompt biometricPrompt = new BiometricPrompt((FragmentActivity) context,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
//                Toast.makeText(getApplicationContext(),
//                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
//                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
               if (result.getCryptoObject() == null) return;
               Cipher cipher = result.getCryptoObject().getCipher();
               if (cipher == null) return;
               try {
                    byte[] encryptedBytes = encryptedPassword.getBytes();
                    byte[] decryptedPassword = cipher.doFinal(Base64.getDecoder().decode(encryptedBytes));
                    listener.onPassword(new String(decryptedPassword));
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                Toast.makeText(getApplicationContext(),
//                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
//                Toast.makeText(getApplicationContext(), "Authentication failed",
//                        Toast.LENGTH_SHORT)
//                        .show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.app_name))
                .setSubtitle("Authentication is needed to decode saved password")
                .setNegativeButtonText("Use account password")
                .build();

        KeyStore keystore = KeyStore.getInstance("AndroidKeyStore");
        keystore.load(null);
        PrivateKey key = (PrivateKey) keystore.getKey(PWD_ENCRYPTION_KEY_ALIAS, null);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));
    }

    private static SecretKeySpec getDataEncryptionKeyKey(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 1024, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), KeyProperties.KEY_ALGORITHM_AES);
    }
    private static Cipher getDataEncryptionCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        String transformation = String.format("%s/%s/%s",
                KeyProperties.KEY_ALGORITHM_AES,
                KeyProperties.BLOCK_MODE_CBC,
                KeyProperties.ENCRYPTION_PADDING_PKCS7);
        return Cipher.getInstance(transformation);
    }

    public static void encryptData(InputStream plainInput, OutputStream encrypted, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException {
        String salt = UUID.randomUUID().toString();
        SecretKeySpec key = getDataEncryptionKeyKey(password, salt);

        Cipher cipher = getDataEncryptionCipher();
        cipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

        encrypted.write(salt.getBytes().length);
        encrypted.write(salt.getBytes());
        encrypted.write(iv.length);
        encrypted.write(iv);

        byte[] buffer = new byte[4096];
        int read;
        while ((read = plainInput.read(buffer)) != -1) {
            byte[] encryptedData = cipher.update(buffer, 0, read);
            encrypted.write(encryptedData);
        }
        byte[] encryptedData = cipher.doFinal();
        encrypted.write(encryptedData);
    }

    public static void decryptData(InputStream encrypted, OutputStream decrypted, String password) throws Exception {
        int saltLength = encrypted.read();
        byte[] salt = new byte[saltLength];
        if (encrypted.read(salt) != saltLength) throw new Exception("Can not parse encrypted file!");

        int ivLength = encrypted.read();
        byte[] iv = new byte[ivLength];
        if (encrypted.read(iv) != ivLength) throw new Exception("Can not parse encrypted file!");

        Cipher cipher = getDataEncryptionCipher();
        SecretKeySpec key = getDataEncryptionKeyKey(password, new String(salt));
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] buffer = new byte[4096];
        int read;
        while ((read = encrypted.read(buffer)) != -1) {
            byte[] decryptedChunk = cipher.update(buffer, 0, read);
            decrypted.write(decryptedChunk);
        }
        byte[] decryptedChunk = cipher.doFinal();
        decrypted.write(decryptedChunk);
    }
}
