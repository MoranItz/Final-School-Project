package com.example.chatit.Activities.Groups;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.Helper_Classes.FireBaseHelper;
import com.example.chatit.R;
import com.example.chatit.Struct_Classes.Group;
import com.example.chatit.Struct_Classes.User;
import com.example.chatit.HomeActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etGroupName, etGroupDescription, etMemberSearch;
    private RecyclerView membersRecyclerView;
    private Button btnCreate;
    
    private List<String> userList; // Search results
    private Set<String> selectedUsernames; // Selected users
    private UserAdapter userAdapter;
    
    private Vector<User> selectedMembersObjs;
    private FireBaseHelper firebaseHelper;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.creategroup_view);

        etGroupName = findViewById(R.id.groupNameInput);
        etGroupDescription = findViewById(R.id.groupDescriptionInput);
        etMemberSearch = findViewById(R.id.memberSearchInput);
        membersRecyclerView = findViewById(R.id.membersRecyclerView);
        btnCreate = findViewById(R.id.createBtn);

        userList = new ArrayList<>();
        selectedUsernames = new HashSet<>();
        selectedMembersObjs = new Vector<>();
        firebaseHelper = new FireBaseHelper();
        
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUsername = prefs.getString("username", "");

        setupRecyclerView();
        setupSearch();

        btnCreate.setOnClickListener(v -> finishGroupCreation());
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(userList, selectedUsernames);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setAdapter(userAdapter);
    }

    private void setupSearch() {
        etMemberSearch.addTextChangedListener(new TextWatcher() {
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

    private void searchUsers(String query) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // \uf8ff is a high-range Unicode character used to create a "starts with" range query in Firestore
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            String username = document.getString("username");
                            // Ensure the current logged-in user doesn't appear in their own search results
                            if (username != null && !username.equals(currentUsername)) {
                                userList.add(username);
                            }
                        }
                        userAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("CreateGroupActivity", "Error searching users", task.getException());
                    }
                });
    }

    private void finishGroupCreation() {
        String groupName = etGroupName.getText().toString().trim();
        String groupDescription = etGroupDescription.getText().toString().trim();

        if (groupName.isEmpty()) {
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

        User currentUser = new User(currentUsername, currentPassword, currentEmail);

        selectedMembersObjs.clear();
        ArrayList<String> selectedList = new ArrayList<>(selectedUsernames);

        if (selectedList.isEmpty()) {
            createGroup(currentUser, groupName, groupDescription);
        } else {
            fetchUsersAndCreateGroup(selectedList, 0, currentUser, groupName, groupDescription);
        }
    }

    private void fetchUsersAndCreateGroup(ArrayList<String> usernames, int index, User currentUser, String groupName, String groupDescription) {
        if (index >= usernames.size()) {
            createGroup(currentUser, groupName, groupDescription);
            return;
        }

        String username = usernames.get(index);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(username).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists())  {
                String email = documentSnapshot.getString("email");
                String password = documentSnapshot.getString("password");
                selectedMembersObjs.add(new User(username, password, email));
            }
            fetchUsersAndCreateGroup(usernames, index + 1, currentUser, groupName, groupDescription);
        }).addOnFailureListener(e -> {
            fetchUsersAndCreateGroup(usernames, index + 1, currentUser, groupName, groupDescription);
        });
    }

    private void createGroup(User currentUser, String groupName, String groupDescription) {
        firebaseHelper.getNewGroupId(id -> {
            Group group = new Group(id, groupName, currentUser.getUsername(), groupDescription, selectedMembersObjs, currentUser);
            Group.registerGroup(group, CreateGroupActivity.this);
        });
    }

    // Inner Adapter Class
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
            String username = users.get(position);
            holder.bind(username, selected.contains(username));
            
            holder.checkBox.setOnClickListener(v -> {
                if (holder.checkBox.isChecked()) {
                    selected.add(username);
                } else {
                    selected.remove(username);
                }
            });
            
            // Also toggle on row click
            holder.itemView.setOnClickListener(v -> holder.checkBox.performClick());
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            CheckBox checkBox;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.memberName);
                checkBox = itemView.findViewById(R.id.memberCheckbox);
            }

            void bind(String username, boolean isSelected) {
                name.setText(username);
                checkBox.setChecked(isSelected);
            }
        }
    }
}
