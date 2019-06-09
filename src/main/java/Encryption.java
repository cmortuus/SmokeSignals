import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Objects;

class Encryption {
    //    TODO don't store these in plain text
    private static Cipher rsaCipher;
    private static final int RSA_KEY_LENGTH = 4096;
    private static final String ALGORITHM_NAME = "RSA";

    //    Defines the cipher var with a try catch
    static {
        try {
            rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    static String encrypt(String strToEncrypt, SecretKey secretKey, String initVector) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(MyBase64.decode(initVector));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return MyBase64.encode(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
    }

    static String decrypt(String strToDecrypt, SecretKey secretKey, String initVector) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(MyBase64.decode(initVector));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return new String(cipher.doFinal(MyBase64.decode(strToDecrypt)));
    }

    /**
     * generate rsa keys
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    static KeyPair generateKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance(ALGORITHM_NAME);
        rsaKeyGen.initialize(RSA_KEY_LENGTH);
        return rsaKeyGen.generateKeyPair();
    }

    /**
     * creates aes keys
     *
     * @return
     */
    static SecretKey generateAESkey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256); // The AES key size in number of bits
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encrypt the aes key with rsa
     *
     * @param publicKey
     * @param secretKey
     * @return
     */
    static byte[] encryptAesWithRsa(PublicKey publicKey, SecretKey secretKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Objects.requireNonNull(rsaCipher, "RSA Cipher cannot be null");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return rsaCipher.doFinal(secretKey.getEncoded());
    }

    /**
     * Decrypt the aes key that has been encrypted with rsa
     *
     * @param encryptedKey
     * @return
     */
    static SecretKey decryptAesKeyWithRsa(byte[] encryptedKey, PrivateKey privateKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Objects.requireNonNull(rsaCipher, "RSA Cipher cannot be null");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new SecretKeySpec(rsaCipher.doFinal(encryptedKey), "AES");
    }

    static byte[] encryptWithRsa(String strToEncrypt, PublicKey publicKey)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (rsaCipher == null)
            throw new IllegalStateException("Cipher cannot be null");
        rsaCipher.init(Cipher.PUBLIC_KEY, publicKey);
        return rsaCipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
    }

    static String decryptWithRsa(byte[] bytesToDecrypt, PrivateKey privateKey) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        rsaCipher.init(Cipher.PRIVATE_KEY, privateKey);
        byte[] bytes = rsaCipher.doFinal(bytesToDecrypt);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}