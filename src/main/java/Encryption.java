import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class Encryption {

//    TODO dont store these in plain text
//    TODO send the aes key once it has been encrypted with rsa and then test reading and writing from the room.
    SecretKey secKey;
    PrivateKey privateKey;
    PublicKey publicKey;
    Cipher cipher;

    public Encryption() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256); // The AES key size in number of bits
            secKey = generator.generateKey();

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            KeyPair keyPair = kpg.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] encryptAESwithRSA() {
        try {
            cipher.init(Cipher.PUBLIC_KEY, publicKey);
            return cipher.doFinal(secKey.getEncoded()/*Seceret Key From Step 1*/);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "hi".getBytes();
    }


    public byte[] encryptAES(String plainText) {
        try {
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
            return aesCipher.doFinal(plainText.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "hi".getBytes();
    }

    public String decrypt(byte[] encryptedKey, byte[] byteCipherText) {
        try {
            cipher.init(Cipher.PRIVATE_KEY, privateKey);
            byte[] decryptedKey = cipher.doFinal(encryptedKey);
            SecretKey originalKey = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.DECRYPT_MODE, originalKey);
            byte[] bytePlainText = aesCipher.doFinal(byteCipherText);
            return new String(bytePlainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
