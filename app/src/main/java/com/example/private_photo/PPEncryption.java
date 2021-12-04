package com.example.private_photo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PPEncryption {

    public static void encryptData(InputStream plainInput, OutputStream encrypted, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        String salt = UUID.randomUUID().toString();
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 1024, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

        encrypted.write(salt.getBytes().length);
        encrypted.write(salt.getBytes());
        encrypted.write(iv.length);
        encrypted.write(iv);

        byte[] buffer = new byte[4096];
        int read = 0;
        while ((read = plainInput.read(buffer)) != -1) {
            byte[] encryptedData = cipher.update(buffer, 0, read);
            encrypted.write(encryptedData);
        }
        byte[] encryptedData = cipher.doFinal();
        encrypted.write(encryptedData);
    }

    public static void decryptData(InputStream encrypted, OutputStream decrypted, String password) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        int saltLength = encrypted.read();
        byte[] salt = new byte[saltLength];
        if (encrypted.read(salt) != saltLength) throw new Exception("Can not parse encrypted file!");

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1024, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), "AES");

        int ivLength = encrypted.read();
        byte[] iv = new byte[ivLength];
        if (encrypted.read(iv) != ivLength) throw new Exception("Can not parse encrypted file!");

        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] buffer = new byte[4096];
        int read = 0;
        while ((read = encrypted.read(buffer)) != -1) {
            byte[] decryptedChunk = cipher.update(buffer, 0, read);
            decrypted.write(decryptedChunk);
        }
        byte[] decryptedChunk = cipher.doFinal();
        decrypted.write(decryptedChunk);
    }
}
