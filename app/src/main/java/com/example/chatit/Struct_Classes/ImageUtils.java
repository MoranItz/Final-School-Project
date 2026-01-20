package com.example.chatit.Struct_Classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ImageUtils {

    // Converts an image URI to a base64 encoded string.
    // Input: Context context (app context), Uri imageUri (the image URI from gallery/camera).
    // Output: String (base64 encoded image) or null if conversion fails.
    public static String convertImageToBase64(Context context, Uri imageUri) {
        try {
            // Open input stream from the URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;

            // Decode the image into a Bitmap
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // Compress and resize if needed (to avoid large base64 strings)
            bitmap = resizeBitmap(bitmap, 800); // Max 800px width/height

            // Convert bitmap to byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); // 80% quality
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            // Encode to base64
            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Converts a base64 string back to a Bitmap image.
    // Input: String base64String (the base64 encoded image).
    // Output: Bitmap (decoded image) or null if decoding fails.
    public static Bitmap convertBase64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Resizes a bitmap to fit within a maximum dimension while maintaining aspect ratio.
    // Input: Bitmap bitmap (original image), int maxSize (maximum width or height).
    // Output: Bitmap (resized image).
    private static Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Check if resizing is needed
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        // Calculate new dimensions maintaining aspect ratio
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
}
