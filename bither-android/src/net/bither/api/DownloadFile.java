package net.bither.api;

import android.os.Build;

import net.bither.http.HttpRequestException;
import net.bither.http.HttpSetting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFile {

    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private boolean hasErrors;

    private boolean downloadUrlToFile(String urlString, File file)
            throws IOException, HttpRequestException {
        disableConnectionReuseIfNecessary();
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        OutputStream outputStream = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection
                    .setConnectTimeout(HttpSetting.HTTP_CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(HttpSetting.HTTP_SO_TIMEOUT);
            int httpCode = urlConnection.getResponseCode();
            if (httpCode != 200) {
                throw new HttpRequestException("http exception " + httpCode);
            }

            outputStream = new FaultHidingOutputStream(new FileOutputStream(
                    file));
            in = new BufferedInputStream(urlConnection.getInputStream(),
                    IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream,
                    IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            throw e;
        } finally {

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (hasErrors) {
                    deleteIfExists(file);
                }

            } catch (final IOException e) {
                throw e;
            }
        }
    }

    /**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private class FaultHidingOutputStream extends FilterOutputStream {
        private FaultHidingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int oneByte) {
            try {
                out.write(oneByte);
            } catch (IOException e) {
                hasErrors = true;
            }
        }

        @Override
        public void write(byte[] buffer, int offset, int length) {
            try {
                out.write(buffer, offset, length);
            } catch (IOException e) {
                hasErrors = true;
            }
        }

        @Override
        public void close() {
            try {
                out.close();
            } catch (IOException e) {
                hasErrors = true;
            }
        }

        @Override
        public void flush() {
            try {
                out.flush();
            } catch (IOException e) {
                hasErrors = true;
            }
        }
    }

    private void deleteIfExists(File file) throws IOException {

        if (file.exists() && !file.delete()) {
            throw new IOException();
        }
    }

    public File downloadFile(String urlString, File file)
            throws Exception {
        if (file.exists()) {
            return file;
        }
        boolean isSuccess = downloadUrlToFile(urlString, file);
        if (isSuccess) {
            return file;
        }
        return null;
    }
}
