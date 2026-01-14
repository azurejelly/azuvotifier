package com.vexsoftware.votifier.util;

import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@UtilityClass
public final class CryptoUtil {

    /**
     * Loads an RSA key pair from a directory.
     *
     * @param directory The directory containing the RSA key pair.
     * @return The RSA key pair stored at the specified directory.
     * @throws GeneralSecurityException if the keys cannot be parsed or the algorithm is unavailable
     * @throws IOException              if an I/O error occurs while reading one or more files
     */
    public static KeyPair load(File directory) throws GeneralSecurityException, IOException {
        byte[] encodedPublicKey = readKeyFile(directory, "public.key");
        byte[] encodedPrivateKey = readKeyFile(directory, "private.key");

        KeyFactory factory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = factory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Encrypts a block of data.
     *
     * @param data The data to encrypt.
     * @param key  The {@link PublicKey public key} the data should be encrypted with.
     * @return The encrypted data.
     * @throws GeneralSecurityException if data fails to be encrypted.
     */
    public static byte[] encrypt(byte[] data, PublicKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts a block of data.
     *
     * @param data The data to decrypt.
     * @param key  The {@link PrivateKey private key} the data should be decrypted with.
     * @return The decrypted data.
     * @throws GeneralSecurityException if data fails to be decrypted.
     */
    public static byte[] decrypt(byte[] data, PrivateKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Saves a {@link KeyPair} to the disk.
     *
     * @param directory The directory to save to
     * @param pair      The {@link KeyPair} to save
     * @throws IOException if an I/O error occurs while saving the key pair
     */
    public static void save(File directory, KeyPair pair) throws IOException {
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKey.getEncoded());
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());

        try (FileOutputStream pubOut = new FileOutputStream(directory + File.separator + "public.key");
             FileOutputStream privOut = new FileOutputStream(directory + File.separator + "private.key")
        ) {
            pubOut.write(Base64.getEncoder().encode(publicSpec.getEncoded()));
            privOut.write(Base64.getEncoder().encode(privateSpec.getEncoded()));
        }
    }

    /**
     * Generates an RSA key pair.
     *
     * @param bits The amount of bits.
     * @return A new RSA key pair.
     * @throws GeneralSecurityException if the key pair fails to be generated
     */
    public static KeyPair generateKeyPair(int bits) throws GeneralSecurityException {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(bits, RSAKeyGenParameterSpec.F4);
        keygen.initialize(spec);

        return keygen.generateKeyPair();
    }

    /**
     * Reads a Base64-encoded key from a file and returns its decoded binary form.
     *
     * @param directory the directory containing the key file
     * @param name      the name of the key file
     * @return the decoded key bytes
     * @throws IOException              if an I/O error occurs while reading the file
     * @throws IllegalArgumentException if the file content is not valid Base64
     */
    private static byte[] readKeyFile(File directory, String name) throws IOException {
        File file = new File(directory, name);
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content = new String(bytes, StandardCharsets.US_ASCII).trim();

        try {
            return Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException ex) {
            String read = new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
            throw new IllegalArgumentException("Base64 decoding failure - this is probably due to a corrupted file." +
                    " In case it isn't, here is a Base64 representation of what we read: " + read, ex);
        }
    }
}
