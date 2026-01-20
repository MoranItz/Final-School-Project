package com.example.chatit.Helper_Classes;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

public class FireBaseHelper {

    // This function is in charge of calling back the function
    // if it the random ID that it generated already exists
    // so it can generate a new ID and give an unused one.
    // (A helper function to the function "getNewGroupId")
    // Input: int newId, Once getNewGroupId finds an unused Id it returns it to this function
    // Output: None
    public interface GroupIdCallback {
        void onResult(int id);
    }

    // This function is in charge of creating a new unique identification for a new group.
    // It gets a callback function named "GroupIdCallBack" which stops it and returns its value
    // without stopping the program itself (deattaching it from the rest of the program)
    // Input: The callback function GroupIdCallback
    // Output: int newId, The new unique group id
    public void getNewGroupId(GroupIdCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Generate a new random number between 10000 and 99999
        int newId = (int)((Math.random() * 900000) + 100000);

        // Find if the generated Id exists anywhere
        db.collection("groups").whereEqualTo("id", newId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If no equal id was found
                        if (task.getResult().isEmpty()) {
                            callback.onResult(newId);
                        } else {
                            // Try with a new unique id
                            getNewGroupId(callback);
                        }
                    } else {
                        // If there was an error trying to connect to the firebase or finding the IDs
                        Log.e("FIREBASE", "Error checking for unique group ID", task.getException());
                    }
                });
    }
}
