package com.audiorecordejemplo;


import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

//Use of AudioRecord class
public class AudioRecordActivity extends AppCompatActivity {

    private static final int SAMPLING_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
     * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
     * size is determined by AudioRecord#getMinBufferSize(int, int, int)} and depends on the
     * recording settings.
     */
    private static final int BUFFER_SIZE_FACTOR = 2;

    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private final AtomicBoolean       recordingInProgress = new AtomicBoolean(false);

    private AudioRecord recorder = null;

    private Thread recordingThread = null;

    private Button startButton;

    private Button stopButton;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio);
        startButton = (Button) findViewById(R.id.btnStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(AudioRecordActivity.this,
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AudioRecordActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            1234);
                }
                if(ContextCompat.checkSelfPermission(AudioRecordActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AudioRecordActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1234);
                }
                if(ContextCompat.checkSelfPermission(AudioRecordActivity.this,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AudioRecordActivity.this,
                            new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                            1234);
                }
                startRecording();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });

        stopButton = (Button) findViewById(R.id.btnStop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1234: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        return ;
                } else {
                    Log.d("TAG", "permission denied by user");
                }
                return;
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopRecording();
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recorder.startRecording();

        recordingInProgress.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }

        recordingInProgress.set(false);

        recorder.stop();

        recorder.release();

        recorder = null;

        recordingThread = null;
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            final File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            Log.d("Filename", String.valueOf(Environment.getExternalStorageDirectory()));
            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }

    }
}
