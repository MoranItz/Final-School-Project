package com.example.chatit.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.R;
import com.example.chatit.Struct_Classes.Group;
import com.example.chatit.Struct_Classes.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText groupNameInput, groupDescriptionInput, memberSearchInput;
    private RecyclerView membersRecyclerView;
    private Button createGroupButton;
    
    private List<String> userList;
    private Set<String> selectedUsernames;
    private UserAdapter userAdapter;
    
    private Vector<User> selectedMemberObjects;
    private String currentUsername;

    // This function is responsible for creating the activity and setting up the UI and basic data.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.creategroup_view);

        groupNameInput = findViewById(R.id.groupNameInput);
        groupDescriptionInput = findViewById(R.id.groupDescriptionInput);
        memberSearchInput = findViewById(R.id.memberSearchInput);
        membersRecyclerView = findViewById(R.id.membersRecyclerView);
        createGroupButton = findViewById(R.id.createBtn);

        userList = new ArrayList<>();
        selectedUsernames = new HashSet<>();
        selectedMemberObjects = new Vector<>();
        
        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUsername = preferences.getString("username", "");

        setupRecyclerView();
        setupSearch();

        ImageView backButton = findViewById(R.id.backBtn);
        backButton.setOnClickListener(v -> finish());

        createGroupButton.setOnClickListener(v -> finishGroupCreation());
    }

    // This function is responsible for initializing the RecyclerView with the user adapter.
    // Input: None.
    // Output: None.
    private void setupRecyclerView() {
        userAdapter = new UserAdapter(userList, selectedUsernames);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setAdapter(userAdapter);
    }

    // This function is responsible for setting up the listener for the search input field.
    // Input: None.
    // Output: None.
    private void setupSearch() {
        memberSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchUsers(s.toString());
                } else {
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // This function is responsible for searching users in Firestore whose names start with the query.
    // Input: String query (the text to search for).
    // Output: None.
    private void searchUsers(String query) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        
        database.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            String name = document.getString("username");
                            if (name != null && !name.equals(currentUsername)) {
                                userList.add(name);
                            }
                        }
                        userAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("CreateGroupActivity", "Error searching users", task.getException());
                    }
                });
    }

    // This function is responsible for validating inputs and starting the group registration process.
    // Input: None.
    // Output: None.
    private void finishGroupCreation() {
        String name = groupNameInput.getText().toString().trim();
        String description = groupDescriptionInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "A new group must have a name", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String currentEmail = sharedPreferences.getString("email", null);
        String currentPassword = sharedPreferences.getString("password", null);

        if (currentUsername == null) {
            Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        User ownerObject = new User(currentUsername, currentPassword, currentEmail);
        selectedMemberObjects.clear();
        ArrayList<String> selectedList = new ArrayList<>(selectedUsernames);

        if (selectedList.isEmpty()) {
            createNewGroupEntry(ownerObject, name, description);
        } else {
            fetchMemberDetailsAndProceed(selectedList, 0, ownerObject, name, description);
        }
    }

    // This function is responsible for fetching full member details from Firestore before creating the group.
    // Input: ArrayList<String> usernames, int index, User owner, String name, String description.
    // Output: None.
    private void fetchMemberDetailsAndProceed(ArrayList<String> usernames, int index, User owner, String name, String description) {
        if (index >= usernames.size()) {
            createNewGroupEntry(owner, name, description);
            return;
        }

        String targetName = usernames.get(index);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        
        database.collection("users").document(targetName).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists())  {
                String email = snapshot.getString("email");
                String password = snapshot.getString("password");
                selectedMemberObjects.add(new User(targetName, password, email));
            }
            fetchMemberDetailsAndProceed(usernames, index + 1, owner, name, description);
        }).addOnFailureListener(error -> {
            fetchMemberDetailsAndProceed(usernames, index + 1, owner, name, description);
        });
    }

    // This function is responsible for generating a unique ID and registering the group in Firestore.
    // Input: User owner, String name, String description.
    // Output: None.
    private void createNewGroupEntry(User owner, String name, String description) {
        generateUniqueGroupId(uniqueId -> {
            Group newGroup = new Group(uniqueId, name, owner.getUsername(), description, selectedMemberObjects, owner);
            Group.registerGroup(newGroup, CreateGroupActivity.this);
        });
    }

    // This interface is responsible for returning the generated ID to the caller.
    private interface GroupIdResultCallback {
        void onResult(int id);
    }

    // This function is responsible for generating a unique 6-digit ID for a new group.
    // Input: GroupIdResultCallback callback (the function to call with the result).
    // Output: None.
    private void generateUniqueGroupId(GroupIdResultCallback callback) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        int randomId = (int)((Math.random() * 900000) + 100000);

        database.collection("groups").whereEqualTo("groupId", randomId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            callback.onResult(randomId);
                        } else {
                            generateUniqueGroupId(callback);
                        }
                    } else {
                        Log.e("FIREBASE", "Error checking for unique group ID", task.getException());
                    }
                });
    }

    // This class is responsible for managing the display of searchable users in the creation screen.
    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<String> users;
        private final Set<String> selected;

        UserAdapter(List<String> users, Set<String> selected) {
            this.users = users;
            this.selected = selected;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String name = users.get(position);
            holder.bind(name, selected.contains(name));
            
            holder.checkBox.setOnClickListener(v -> {
                if (holder.checkBox.isChecked()) {
                    selected.add(name);
                } else {
                    selected.remove(name);
                }
            });
            
            holder.itemView.setOnClickListener(v -> holder.checkBox.performClick());
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            CheckBox checkBox;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.memberName);
                checkBox = itemView.findViewById(R.id.memberCheckbox);
            }

            void bind(String name, boolean isSelected) {
                nameText.setText(name);
                checkBox.setChecked(isSelected);
            }
        }
    }
}
