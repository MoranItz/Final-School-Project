package com.example.chatit.Struct_Classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    // This function is responsible for converting an image file into a Base64 string for storage.
    // Input: Context context, Uri imageUri.
    // Output: String (Base64 encoded image).
    public static String convertImageToBase64(Context context, Uri imageUri) {
        try {
            InputStream stream = context.getContentResolver().openInputStream(imageUri);
            if (stream == null) return null;

            Bitmap image = BitmapFactory.decodeStream(stream);
            stream.close();

            // Shrink the image so it takes less space in the database
            image = resizeImage(image, 800);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] bytes = outputStream.toByteArray();

            return Base64.encodeToString(bytes, Base64.DEFAULT);

        } catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    // This function is responsible for turning a Base64 string back into a viewable image.
    // Input: String base64Data.
    // Output: Bitmap (the decoded image).
    public static Bitmap convertBase64ToBitmap(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) return null;
        try {
            byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    // This function is responsible for scaling down an image if it is too large.
    // Input: Bitmap originalImage, int maximumSize.
    // Output: Bitmap (the resized image).
    private static Bitmap resizeImage(Bitmap originalImage, int maximumSize) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        if (originalWidth <= maximumSize && originalHeight <= maximumSize) {
            return originalImage;
        }

        float scaleFactor = Math.min((float) maximumSize / originalWidth, (float) maximumSize / originalHeight);
        int finalWidth = Math.round(originalWidth * scaleFactor);
        int finalHeight = Math.round(originalHeight * scaleFactor);

        return Bitmap.createScaledBitmap(originalImage, finalWidth, finalHeight, true);
    }
}
