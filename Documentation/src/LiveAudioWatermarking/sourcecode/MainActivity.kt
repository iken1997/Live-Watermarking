package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.preference.PreferenceManager
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Integer.max
import android.location.Location
import android.location.LocationListener
import android.widget.Toast
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class MainActivity : AppCompatActivity(), LocationListener {


    private var FREQ_BIT_START = 19000  // 19 kHz
    private var FREQ_BIT_STOP = 18500  // 19 kHz
    private var FREQ_BIT_1 = 18000  // 18 kHz
    private var FREQ_BIT_0 = 0  // no tone
    private var BIT_DURATION = 20  // Duration of each bit in milliseconds
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val locationPermissionCode = 2
    private var latitude = 0f
    private var longitude = 0f
    private var USERKEY = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val transmitButton: Button = findViewById(R.id.transmitButton)
        val dataToTransmit: EditText = findViewById(R.id.dataEditText)

        //retrieve the settings
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        //retrieve gps data and update preferences
        if (sharedPreferences.getBoolean("share_gps_position", false))
            getLocation()

        //update parameters
        BIT_DURATION = sharedPreferences.getString("bit_duration", "100")?.toIntOrNull() ?: 10
        FREQ_BIT_0 = sharedPreferences.getString("bit_frequency_0", "0")?.toIntOrNull() ?: 0
        FREQ_BIT_1 = sharedPreferences.getString("bit_frequency_1", "18000")?.toIntOrNull() ?: 18000
        FREQ_BIT_START =
            sharedPreferences.getString("bit_frequency_start", "19000")?.toIntOrNull() ?: 19000
        FREQ_BIT_STOP =
            sharedPreferences.getString("bit_frequency_stop", "18500")?.toIntOrNull() ?: 18500
        USERKEY = sharedPreferences.getString("encryption_key", "") ?: ""


        transmitButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                var transData = byteArrayOf() //data to transmit
                if (!sharedPreferences.getBoolean("share_gps_position", false))
                    transData += dataToTransmit.text.toString().toByteArray() //adding the GPS position to the data to send
                else {
                    transData += floatToByteArray(latitude)
                    transData += floatToByteArray(longitude)
                    transData += dataToTransmit.text.toString().toByteArray()
                }
                if (USERKEY != "") { //if a key s specified the data is encrypted
                    transData = encrypt(transData, USERKEY)
                }

                do {
                    transmitData(transData)
                } while (sharedPreferences.getBoolean("continuous_playing", false)) //continuous playing
            }
        }

    }

    private fun floatToByteArray(value: Float): ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.putFloat(value)
        return buffer.array()
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Retrieve the TextView
        val settingsTextView = findViewById<TextView>(R.id.settingsTextView)

        // Retrieve and format the current settings
        val bitDuration = sharedPreferences.getString("bit_duration", "100")?.toLongOrNull() ?: 100
        val bitFrequency1 =
            sharedPreferences.getString("bit_frequency_1", "18000")?.toIntOrNull() ?: 18000
        val bitFrequency0 = sharedPreferences.getString("bit_frequency_0", "0")?.toIntOrNull() ?: 0
        val bitFrequencyStart =
            sharedPreferences.getString("bit_frequency_start", "19000")?.toIntOrNull() ?: 19000
        val bitFrequencyStop =
            sharedPreferences.getString("bit_frequency_stop", "18500")?.toIntOrNull() ?: 18500
        val encryptionKey = sharedPreferences.getString("encryption_key", "") ?: ""
        val continuousPlaying = sharedPreferences.getBoolean("continuous_playing", false)
        val shareGpsPosition = sharedPreferences.getBoolean("share_gps_position", false)

        // Create a formatted string with the current settings
        val settingsText = """
            Current Settings:
                Bit Duration: $bitDuration ms
                Bit Frequency 1: $bitFrequency1 Hz
                Bit Frequency 0: $bitFrequency0 Hz
                Bit Frequency START: $bitFrequencyStart Hz
                Bit Frequency STOP: $bitFrequencyStop Hz
                Encryption Key: $encryptionKey
                Continuous Playing: ${if (continuousPlaying) "Enabled" else "Disabled"}
                Share GPS Position: ${if (shareGpsPosition) "Enabled" else "Disabled"}
                    - latitude $latitude
                    - longitude $longitude
        """

        // Update the TextView with the formatted settings
        settingsTextView.text = settingsText
        //update parameters
        BIT_DURATION = bitDuration.toInt()
        FREQ_BIT_0 = bitFrequency0
        FREQ_BIT_1 = bitFrequency1
        FREQ_BIT_START = bitFrequencyStart
        FREQ_BIT_STOP = bitFrequencyStop
        USERKEY = encryptionKey
        if (shareGpsPosition) getLocation()
    }


    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude.toFloat()
        longitude = location.longitude.toFloat()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encrypt(plaintext: ByteArray, input_key: String): ByteArray {

        val key: SecretKey = SecretKeySpec(input_key.padStart(32, '0').toByteArray(), "aes")

        val emptyArray = byteArrayOf(
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )

        // set cipher and options
        val cipher = Cipher.getInstance("AES/CFB/NOPADDING")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(emptyArray))

        return cipher.doFinal(plaintext)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Open the settings activity
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun generateTone(frequency: Int, durationMs: Int): ShortArray {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs / 1000)
        val sample = ShortArray(numSamples)
        val angle = 2.0 * Math.PI * frequency / sampleRate

        for (i in 0 until numSamples) {
            sample[i] = (32767.0 * Math.sin(i * angle)).toInt().toShort()
        }
        return sample
    }

    private fun stringToBits(input: String): String {
        val binaryStringBuilder = StringBuilder()

        for (char in input) {
            val asciiValue = char.toInt()
            val binaryChar = Integer.toBinaryString(asciiValue)

            // Ensure that the binary representation has 8 bits
            val paddedBinaryChar = binaryChar.padStart(8, '0')

            binaryStringBuilder.append(paddedBinaryChar)
        }

        return binaryStringBuilder.toString()
    }

    private fun dataToBits(input: ByteArray): String {
        val binaryStringBuilder = StringBuilder()

        for (byte in input) {
            val binary = Integer.toBinaryString(byte.toInt() and 0xFF)

            // Ensure that the binary representation has 8 bits
            val paddedBinary = binary.padStart(8, '0')

            binaryStringBuilder.append(paddedBinary)
        }

        return binaryStringBuilder.toString()
    }

    private fun transmitData(data: ByteArray) {
        val minBuff = AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max((44100 * BIT_DURATION / 1000 * data.toString().length), minBuff),
            AudioTrack.MODE_STREAM
        )
        audioTrack.play()
        val bitData = dataToBits(data)
        val start = generateTone(FREQ_BIT_START, BIT_DURATION)
        val stop = generateTone(FREQ_BIT_STOP, BIT_DURATION)
        audioTrack.write(start, 0, start.size, AudioTrack.WRITE_BLOCKING)
        for (bit in bitData) {
            val tone = if (bit == '1') generateTone(FREQ_BIT_1, BIT_DURATION)
            else generateTone(FREQ_BIT_0, BIT_DURATION)
            audioTrack.write(tone, 0, tone.size, AudioTrack.WRITE_BLOCKING)
        }
        audioTrack.write(stop, 0, start.size, AudioTrack.WRITE_BLOCKING)
        handler.postDelayed({
            audioTrack.stop()
            audioTrack.release()
        }, 100 * BIT_DURATION.toLong()) //delay added to finish the transmission
    }


}