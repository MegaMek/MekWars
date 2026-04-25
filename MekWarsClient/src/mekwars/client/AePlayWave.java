package mekwars.client;

/**
 * @author http://www.anyexample.com
 */
class AePlayWave extends Thread {

    private String filename;

    private static final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

    private enum Position {
        LEFT, RIGHT, NORMAL
    }

    ;

    private mekwars.client.AePlayWave.Position curPosition;

    public AePlayWave(String wavfile) {
        filename = wavfile;
        curPosition = mekwars.client.AePlayWave.Position.NORMAL;
    }

    public AePlayWave(String wavfile, mekwars.client.AePlayWave.Position p) {
        filename = wavfile;
        curPosition = p;
    }

    @Override
    public synchronized void run() {

        java.io.File soundFile = new java.io.File(filename);
        if (!soundFile.exists()) {
            MWLogger.errLog("Wave file not found: " + filename);
            return;
        }

        javax.sound.sampled.AudioInputStream audioInputStream = null;
        try {
            audioInputStream = javax.sound.sampled.AudioSystem.getAudioInputStream(soundFile);
        } catch (javax.sound.sampled.UnsupportedAudioFileException e1) {
            MWLogger.errLog(e1);
            return;
        } catch (java.io.IOException e1) {
            MWLogger.errLog(e1);
            return;
        }

        javax.sound.sampled.AudioFormat format = audioInputStream.getFormat();
        javax.sound.sampled.SourceDataLine auline = null;
        javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
              javax.sound.sampled.SourceDataLine.class, format);

        try {
            auline = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
            auline.open(format);
        } catch (javax.sound.sampled.LineUnavailableException e) {
            MWLogger.errLog(e);
            return;
        } catch (Exception e) {
            MWLogger.errLog(e);
            return;
        }

        if (auline.isControlSupported(javax.sound.sampled.FloatControl.Type.PAN)) {
            javax.sound.sampled.FloatControl pan = (javax.sound.sampled.FloatControl) auline
                                                                                            .getControl(javax.sound.sampled.FloatControl.Type.PAN);
            if (curPosition == mekwars.client.AePlayWave.Position.RIGHT) {
                pan.setValue(1.0f);
            } else if (curPosition == mekwars.client.AePlayWave.Position.LEFT) {
                pan.setValue(-1.0f);
            }
        }

        auline.start();
        int nBytesRead = 0;
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

        try {
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                if (nBytesRead >= 0) {
                    auline.write(abData, 0, nBytesRead);
                }
            }
        } catch (java.io.IOException e) {
            MWLogger.errLog(e);
            return;
        } finally {
            auline.drain();
            auline.close();
        }
    }

    public static void AePlayWaveNonThreaded(String filename) {

        java.io.File soundFile = new java.io.File(filename);
        if (!soundFile.exists()) {
            MWLogger.errLog("Wave file not found: " + filename);
            return;
        }

        javax.sound.sampled.AudioInputStream audioInputStream = null;
        try {
            audioInputStream = javax.sound.sampled.AudioSystem.getAudioInputStream(soundFile);
        } catch (javax.sound.sampled.UnsupportedAudioFileException e1) {
            MWLogger.errLog(e1);
            return;
        } catch (java.io.IOException e1) {
            MWLogger.errLog(e1);
            return;
        }

        javax.sound.sampled.AudioFormat format = audioInputStream.getFormat();
        javax.sound.sampled.SourceDataLine auline = null;
        javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
              javax.sound.sampled.SourceDataLine.class, format);

        try {
            auline = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
            auline.open(format);
        } catch (javax.sound.sampled.LineUnavailableException e) {
            MWLogger.errLog(e);
            return;
        } catch (Exception e) {
            MWLogger.errLog(e);
            return;
        }

        auline.start();
        int nBytesRead = 0;
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

        try {
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                if (nBytesRead >= 0) {
                    auline.write(abData, 0, nBytesRead);
                }
            }
        } catch (java.io.IOException e) {
            MWLogger.errLog(e);
            return;
        } finally {
            auline.drain();
            auline.close();
        }
    }
}
