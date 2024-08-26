import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


fun encrypt(plaintext: ByteArray, input_key: String): ByteArray {

    // input key gets padded with character '0' until length 32 (256 bit key length) is reached
    val key: SecretKey = SecretKeySpec(input_key.padStart(32, '0').toByteArray(), "aes")

    // empty IV for easiness of implementation, should be changed to random for increased security
    val emptyInitializationVector =
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

    // set cipher and options (AES256, CFB mode with no padding)
    val cipher = Cipher.getInstance("AES/CFB/NOPADDING")
    cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(emptyInitializationVector))

    return cipher.doFinal(plaintext)
}

fun decrypt(ciphertext: ByteArray, input_key: String): ByteArray {

    // exactly same code as function "encrypt" but in decryption mode
    val key: SecretKey = SecretKeySpec(input_key.padStart(32, '0').toByteArray(), "aes")
    val iv = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    val cipher = Cipher.getInstance("AES/CFB/NOPADDING")
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    //println(cipher.blockSize)


    return cipher.doFinal(ciphertext)
}

fun main(args: Array<String>) {
    System.setProperty("file.encoding","Unicode")
    // check for arguments soundness
    if (args.size != 2)
    {
        println("Correct usage is: java -jar cyber.jar string_to_decrypt key")
    }
    else
    {
        var stringToDecrypt = args[0] // first argument from command line

        // cut the incoming string to decrypt into chunks of 8 characters, then
        // convert each chunk into a byte and append it to an array
        var stringToDecryptAsByteArray: ByteArray = byteArrayOf()
        for (i in 0..stringToDecrypt.length - 1 step 8) {
            //println(exampleString.slice(IntRange(i, i+7))) //.toInt(2).toByte()
            stringToDecryptAsByteArray += stringToDecrypt.slice(IntRange(i, i + 7)).toInt(2).toByte()
        }

        // decrypt the string of bits with the key from the second argument from command line
        // then print it
        val message=(decrypt(stringToDecryptAsByteArray, args[1]))
        //println(String(message))
        val binaryString = message.joinToString("") { byte -> String.format("%8s", Integer.toBinaryString(byte.toInt() and 0xFF)).replace(' ', '0') }
        print(binaryString)

    }
}