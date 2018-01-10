import com.sun.crypto.provider.AESKeyGenerator;
//import sun.security.krb5.internal.crypto.dk.AesDkCrypto; // not used

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author Samuel Lewis (srl8336)
 */
public class Encryption {
    private static KeyPair rsaKeys;
    private PublicKey theirPublicKey;
    private SecretKey aesKey;

    /**
     * Null constructor, creates an encryption object without the public key of the intended communication partner
     * @throws Exception When theirPublicKey is null, an exception is thrown
     */
    public Encryption() throws Exception {
        //Create RSA keys if not created
        if (rsaKeys == null) {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            rsaKeys = keyGen.generateKeyPair();
        }
    }

    /**
     * Constructor, creates an encryption object with the public key encoded
     * as a byte[] of the intended communication partner preset
     * @param theirKey The provided public key for the communication partner
     * @throws Exception When theirPublicKey is null, an exception is thrown
     */
    public Encryption(byte[] theirKey) throws Exception {
        //Create RSA keys if not created
        if (rsaKeys == null) {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            rsaKeys = keyGen.generateKeyPair();
        }
        setTheirPublicKey(theirKey);
    }

    /**
     * myPublicKey returns the Public RSA key associated with this Encryption
     * @return Public key from KeyPair rsaKeys
     */
    public PublicKey getMyPublicKey() throws Exception {
        if (rsaKeys == null) {
            throw new Exception("TheirPublicKey not initialized");
        }
        return rsaKeys.getPublic();
    }

    /**
     * getTheirPublicKey returns the Public RSA key associated with this objects partner
     * as an encoded byte[]
     * @return Public key from theirPublicKey
     */
    public byte[] getTheirPublicKey() throws Exception {
        if (theirPublicKey == null) {
            throw new Exception("TheirPublicKey not initialized");
        }
        return theirPublicKey.getEncoded();
    }

    /**
     * Sets theirPublicKey to the provided PublicKey as a byte[]
     * This will allow you to reset the key later, simply overwriting any previous value
     * @param theirKey The public key associated with the intended communication partner
     */
    public void setTheirPublicKey(byte[] theirKey) throws Exception {
        KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
        theirPublicKey = rsaFactory.generatePublic(new X509EncodedKeySpec(theirKey));
    }

    /**
     * RSADecrypt decrypts a byte[] with myPublicKey and returns the plaintext as a string
     * @param cipherText byte[] to be decrypted
     * @return plaintext of byte[] as string
     * @throws Exception When theirPublicKey is null, an exception is thrown
     */
    private String RSADecrypt(byte[] cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaKeys.getPrivate());
        byte[] newPlainText = cipher.doFinal(cipherText);
        return( new String(newPlainText, "UTF8") );
    }

    /**
     * RSAEncrypt encrypts a String using theirPublicKey
     * @param inputPlainText String to be encrypted
     * @return byte[] representing the ciphertext created from the plaintext
     * @throws Exception When theirPublicKey is null, an exception is thrown
     */
    private byte[] RSAEncrypt(String inputPlainText) throws Exception {
        if (theirPublicKey == null) {
            throw new Exception("TheirPublicKey not initialized");
        }
        byte[] plainText = inputPlainText.getBytes("UTF8");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, theirPublicKey);
        return cipher.doFinal(plainText);
    }

    /**
     * Decrypts a RSA encrypted AES key and stores it
     * @param keyCipherText
     * @throws Exception
     */
    public void setAESKey(byte[] keyCipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaKeys.getPrivate());
        byte[] newAESKEY = cipher.doFinal(keyCipherText);
        aesKey = new SecretKeySpec(newAESKEY, "AES");

    }

    /**
     * generateAESKey generates a new AES key, stores it,
     * and returns it as a byte[] encrypted with your peer's RSA key
     * @return byte[] of AES key encrypted with peer's RSA key
     * @throws Exception
     */
    public byte[] generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        aesKey = keyGen.generateKey();
        if (theirPublicKey == null) {
            throw new Exception("TheirPublicKey not initialized");
        }
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, theirPublicKey);
        return cipher.doFinal(aesKey.getEncoded());
    }

    /**
     * Simple helper function for AESEncrypt. Offered to simplify Manager work.
     * @param in Input string to encrypt
     * @return byte[] representing encrypted message
     * @throws Exception
     */
    public byte[] encrypt(String in) throws Exception {
        return AESEncrypt(in);
    }

    /**
     * Simple helper function for AESDecrypt. Offered to simplify Manager work.
     * @param in Input byte[] to decrypt
     * @return String from decrypting the byte[]
     * @throws Exception
     */
    public String decrypt(byte[] in) throws Exception {
        return AESDecrypt(in);
    }
    /**
     * Encrypts a string with the stores AES key
     * @param inputPlainText text to be encrypted
     * @return the byte[] of the provided string after AES encryption
     * @throws Exception
     */
    public byte[] AESEncrypt(String inputPlainText) throws Exception {
        
        if (aesKey == null) {
            throw new Exception("AES key not initialized");
        }
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] ciphertext = cipher.doFinal(inputPlainText.getBytes());
            //System.out.println("encrypted bytes:");
            //for ( byte d : ciphertext) {
            //    System.out.printf ( String.format ( "%d ",
            //                                        d ) );
            //}

        return ciphertext;
    }

    /**
     * Decrypts a byte[] with the AES key
     * @param inputCipherText byte[] to be decrypted
     * @return the String from the decrypted byte[]
     * @throws Exception
     */
    public String AESDecrypt(byte[] inputCipherText) throws Exception {
            //System.out.println("decrypting bytes:");
            //for ( byte d : inputCipherText) {
            //    System.out.printf ( String.format ( "%d ",
            //                                        d ) );
            //}
        if (aesKey == null) {
            throw new Exception("AES key not initialized");
        }
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] newPlainText = cipher.doFinal(inputCipherText);
        return( new String(newPlainText, "UTF8") );
    }

    /**
     * reset() sets the stored encryption information to null and generates
     * a new pair of personal RSA keys
     */
    public void reset() throws Exception {
        theirPublicKey = null;
        aesKey = null;
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        rsaKeys = keyGen.generateKeyPair();
    }


    //testing PSVM, their key is set to the objects own public key to send an example message
    //left this in as an example, it should be cleaned up after everyone understands the process
    //aes example code added
    //generates an AES key and stores the encrypted copy
    //sampleText is encrypted via AES, then the encrypted AES key
    //is set using setAESKey to simulate a peer receiving the message
    public static void main (String[] args) throws Exception {
        System.out.println(args[0]);
        Encryption encrypt = new Encryption();
        byte[] myPub = encrypt.getMyPublicKey().getEncoded();

        encrypt.setTheirPublicKey(myPub);
        byte[] encrypted = encrypt.RSAEncrypt(args[0]);
        //System.out.println(new String(encrypted, "UTF8"));
        String decrypted = encrypt.RSADecrypt(encrypted);
        System.out.println(decrypted);

        byte[] aesEncrypted = encrypt.generateAESKey();
        byte[] aesTest = encrypt.AESEncrypt("sampleText");
        encrypt.setAESKey(aesEncrypted); //proves set key function works,
        //as we encrypted with the key, then reset it from the provided byte[]
        System.out.println(encrypt.AESDecrypt(aesTest));

    }

}
