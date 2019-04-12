import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class Encryption {
//    TODO dont store these in plain text
    private static Cipher cipher;
    static int RSA_KEY_LENGTH = 4096;
    static String ALGORITHM_NAME = "RSA";


//    Defines the ciphar var with a try catch
    static {
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    /**
     * generate rsa keys
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance(ALGORITHM_NAME);
        rsaKeyGen.initialize(RSA_KEY_LENGTH);
        return rsaKeyGen.generateKeyPair();
    }

    /**
     * creates aes keys
     * @return
     */
    public static SecretKey generateAESkey() {
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
     * @param publicKey
     * @param secretKey
     * @return
     */
    public static byte[] encryptAESwithRSA(PublicKey publicKey, SecretKey secretKey) {
        if(cipher == null)
            throw new IllegalStateException("Cipher cannot be null");
        try {
            cipher.init(Cipher.PUBLIC_KEY, publicKey);
            return cipher.doFinal(secretKey.getEncoded()/*Seceret Key From Step 1*/);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encrypt message with aes
     * @param plainText
     * @param secretKey
     * @return
     */
    public static byte[] encryptAES(String plainText, SecretKey secretKey) {
        if(cipher == null)
            throw new IllegalStateException("Cipher cannot be null");
        try {
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return aesCipher.doFinal(plainText.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Decrypt the aes key that has been encrypted with rsa
     *
     * @param encryptedKey
     * @return
     */
    public static byte[] decrypteAESkeyWithRSA(byte[] encryptedKey, PrivateKey privateKey) {
        try {
            cipher.init(Cipher.PRIVATE_KEY, privateKey);
            return cipher.doFinal(encryptedKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * If returns null than it failed to decrypt the file
     * @param decryptedKey
     * @param byteCipherText
     * @return
     */
//    TODO change this so that you can send it a pubsub message that has username and message included and it will use the username to figure out which aes key to decypt with and then decrypt the message
    public static String decryptWithAES(byte[] decryptedKey, byte[] byteCipherText) {
        try {
            SecretKey originalKey = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.DECRYPT_MODE, originalKey);
            byte[] bytePlainText = aesCipher.doFinal(byteCipherText);
            return new String(bytePlainText);
        } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
