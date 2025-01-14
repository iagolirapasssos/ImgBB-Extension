package com.bosonshiggs.imgbb;

import android.os.Handler;
import android.os.Looper;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@DesignerComponent(
	version = 3,
	versionName = "1.0",
	description = "An extension for making async HTTP POST requests to upload images to ImgBB.",
	iconName = "icon.png"
)
public class ImgBB extends AndroidNonvisibleComponent {

    private final Handler uiHandler;
    private static final String LINE_FEED = "\r\n";
    private static final String BOUNDARY = "----WebKitFormBoundary7MA4YWxkTrZu0gW";

    public ImgBB(ComponentContainer container) {
        super(container.$form());
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @SimpleFunction(description = "Sends an asynchronous POST request to upload an image file to ImgBB using multipart/form-data.")
    public void UploadImageToImgBB(final String imagePath, final String apiKey) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Setup URL with the API key
                    URL url = new URL("https://api.imgbb.com/1/upload?key=" + apiKey);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
                    con.setDoOutput(true);

                    // Start writing the request body
                    DataOutputStream outputStream = new DataOutputStream(con.getOutputStream());

                    // Add file part
                    File file = new File(imagePath);
                    String fileName = file.getName();
                    outputStream.writeBytes("--" + BOUNDARY + LINE_FEED);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"" + LINE_FEED);
                    outputStream.writeBytes("Content-Type: " + HttpURLConnection.guessContentTypeFromName(fileName) + LINE_FEED);
                    outputStream.writeBytes(LINE_FEED);

                    // Read file and write to stream
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    fileInputStream.close();

                    outputStream.writeBytes(LINE_FEED);
                    outputStream.writeBytes("--" + BOUNDARY + "--" + LINE_FEED);
                    outputStream.flush();
                    outputStream.close();

                    // Get the response
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    final StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Post the result back to the UI thread
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ImageUploaded(response.toString());
                        }
                    });
                } catch (final Exception e) {
                    // Post the error back to the UI thread
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ErrorOccurred(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    @SimpleEvent(description = "Event triggered when the image upload is successful.")
    public void ImageUploaded(String responseContent) {
        EventDispatcher.dispatchEvent(this, "ImageUploaded", responseContent);
    }

    @SimpleEvent(description = "Event triggered when an error occurs during a request.")
    public void ErrorOccurred(String errorMessage) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", errorMessage);
    }
}

