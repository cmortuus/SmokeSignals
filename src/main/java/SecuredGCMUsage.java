import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * This class securely performs AES encryption in GCM mode, with 256 bits key size.
 */
public class SecuredGCMUsage {

    static int AES_KEY_SIZE = 256;
    public static int IV_SIZE = 96;
    public static int TAG_BIT_LENGTH = 128;
    private static String ALGO_TRANSFORMATION_STRING = "AES/GCM/PKCS5Padding";

    /**
     * Attempts to encrypt the {@code message} with a symmetric encryption technique
     *
     * @param message      the String of data you wish to encrypt
     * @param aesKey       the symmetrical encryption key to be used for the encryption
     * @param gcmParamSpec the GCM parameter specifications used for the encryption
     * @param aadData      the extra data tag to be included within the final encryption
     * @return a byte array of the encrypted message
     */
    public static byte[] aesEncrypt(String message, SecretKey aesKey, GCMParameterSpec gcmParamSpec, byte[] aadData) {
        Cipher c = null;

        try {
            c = Cipher.getInstance(ALGO_TRANSFORMATION_STRING); // Transformation specifies algortihm, mode of operation and padding
        } catch (NoSuchAlgorithmException noSuchAlgoExc) {
            System.out.println("Exception while encrypting. Algorithm being requested is not available in this environment " + noSuchAlgoExc);
            System.exit(1);
        } catch (NoSuchPaddingException noSuchPaddingExc) {
            System.out.println("Exception while encrypting. Padding Scheme being requested is not available this environment " + noSuchPaddingExc);
            System.exit(1);
        }


        try {
            c.init(Cipher.ENCRYPT_MODE, aesKey, gcmParamSpec, new SecureRandom());
        } catch (InvalidKeyException invalidKeyExc) {
            System.out.println("Exception while encrypting. Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized " + invalidKeyExc);
            System.exit(1);
        } catch (InvalidAlgorithmParameterException invalidAlgoParamExc) {
            System.out.println("Exception while encrypting. Algorithm parameters being specified are not valid " + invalidAlgoParamExc);
            System.exit(1);
        }

        try {
            c.updateAAD(aadData); // add AAD tag data before encrypting
        } catch (IllegalArgumentException illegalArgumentExc) {
            System.out.println("Exception thrown while encrypting. Byte array might be null " + illegalArgumentExc);
            System.exit(1);
        } catch (IllegalStateException illegalStateExc) {
            System.out.println("Exception thrown while encrypting. CIpher is in an illegal state " + illegalStateExc);
            System.exit(1);
        } catch (UnsupportedOperationException unsupportedExc) {
            System.out.println("Exception thrown while encrypting. Provider might not be supporting this method " + unsupportedExc);
            System.exit(1);
        }

        byte[] cipherTextInByteArr = null;
        try {
            cipherTextInByteArr = c.doFinal(message.getBytes());
        } catch (IllegalBlockSizeException illegalBlockSizeExc) {
            System.out.println("Exception while encrypting, due to block size " + illegalBlockSizeExc);
            System.exit(1);
        } catch (BadPaddingException badPaddingExc) {
            System.out.println("Exception while encrypting, due to padding scheme " + badPaddingExc);
            System.exit(1);
        }

        return cipherTextInByteArr;
    }

    /**
     * Attempts to decrypt the {@code encryptedMessage} with a symmetric encryption technique
     *
     * @param encryptedMessage the String of data you wish to encrypt
     * @param aesKey           the symmetrical encryption key to be used for the decryption
     * @param gcmParamSpec     the GCM parameter specifications used during encryption
     * @param aadData          the extra data tag to be included within the encryption
     * @return a byte array of the decrypted message
     */
    public static byte[] aesDecrypt(byte[] encryptedMessage, SecretKey aesKey, GCMParameterSpec gcmParamSpec, byte[] aadData) {
        Cipher c = null;

        try {
            c = Cipher.getInstance(ALGO_TRANSFORMATION_STRING); // Transformation specifies algortihm, mode of operation and padding
        } catch (NoSuchAlgorithmException noSuchAlgoExc) {
            System.out.println("Exception while decrypting. Algorithm being requested is not available in environment " + noSuchAlgoExc);
            System.exit(1);
        } catch (NoSuchPaddingException noSuchAlgoExc) {
            System.out.println("Exception while decrypting. Padding scheme being requested is not available in environment " + noSuchAlgoExc);
            System.exit(1);
        }

        try {
            c.init(Cipher.DECRYPT_MODE, aesKey, gcmParamSpec, new SecureRandom());
        } catch (InvalidKeyException invalidKeyExc) {
            System.out.println("Exception while encrypting. Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized " + invalidKeyExc);
            System.exit(1);
        } catch (InvalidAlgorithmParameterException invalidParamSpecExc) {
            System.out.println("Exception while encrypting. Algorithm Param being used is not valid. " + invalidParamSpecExc);
            System.exit(1);
        }

        try {
            c.updateAAD(aadData); // Add AAD details before decrypting
        } catch (IllegalArgumentException illegalArgumentExc) {
            System.out.println("Exception thrown while encrypting. Byte array might be null " + illegalArgumentExc);
            System.exit(1);
        } catch (IllegalStateException illegalStateExc) {
            System.out.println("Exception thrown while encrypting. CIpher is in an illegal state " + illegalStateExc);
            System.exit(1);
        }

        byte[] plainTextInByteArr = null;
        try {
            plainTextInByteArr = c.doFinal(encryptedMessage);
        } catch (IllegalBlockSizeException illegalBlockSizeExc) {
            System.out.println("Exception while decryption, due to block size " + illegalBlockSizeExc);
            System.exit(1);
        } catch (BadPaddingException badPaddingExc) {
            System.out.println("Exception while decryption, due to padding scheme " + badPaddingExc);
            System.exit(1);
        }

        return plainTextInByteArr;
    }
}