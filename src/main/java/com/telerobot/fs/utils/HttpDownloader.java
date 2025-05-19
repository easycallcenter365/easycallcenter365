package com.telerobot.fs.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class HttpDownloader {
    
    private static final int BUFFER_SIZE = 8192;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java HttpDownloader <URL> <outputFilePath>");
            System.exit(1);
        }

        String url = args[0];
        String outputPath = args[1];

        try {
            downloadFile(url, outputPath);
            System.out.println("Download completed successfully!");
        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static boolean downloadFile(String url, String outputPath) throws IOException {
        OkHttpClient client = createUnsafeOkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is empty");
            }

            File outputFile = new File(outputPath);
            File parentDir = outputFile.getAbsoluteFile().getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            System.out.printf("Downloading %s to %s...%n", url, outputPath);
            try (BufferedInputStream inputStream = new BufferedInputStream(body.byteStream());
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                
                System.out.printf("Download completed. Total size: %.2f MB%n", 
                        totalBytesRead / (1024.0 * 1024.0));
            }
        }
        return true;
    }

    private static OkHttpClient createUnsafeOkHttpClient() {
        try {
            // 创建信任所有证书的TrustManager
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            // 配置SSL Context
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create unsafe OkHttpClient", e);
        }
    }
}