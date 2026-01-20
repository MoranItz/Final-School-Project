package com.example.chatit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.Activities.CreateGroupActivity;
import com.example.chatit.Activities.GroupChatActivity;
import com.example.chatit.Activities.IntroductionActivity;
import com.example.chatit.Adapters.GroupAdapter;
import com.example.chatit.Struct_Classes.Group;
import com.example.chatit.Struct_Classes.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerViewGroups;
    private TextView tvEmptyState;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;
    private String currentUsername;

    // Sets up the main screen of the application and coordinates the loading of user data if logged in.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the session is invalid, the activity stops here to prevent unauthorized access
        if (!checkLoginStatus()) {
            return;
        }

        setContentView(R.layout.home_view);
        initData();
        initViews();
        loadUserGroups();
    }

    // Verifies the user's login state from local storage and redirects to the introduction screen if not logged in.
    // Input: None.
    // Output: boolean (true if logged in, false otherwise).
    private boolean checkLoginStatus() {
        if (isUserLoggedIn(this)) {
            return true;
        }

        startActivity(new Intent(this, IntroductionActivity.class));
        finish();
        return false;
    }

    // Retrieves the current user's username from SharedPreferences and prepares the group list container.
    // Input: None.
    // Output: None.
    private void initData() {
        currentUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "");
        groupList = new ArrayList<>();
    }

    // Connects layout views to variables and sets up the RecyclerView and button click listeners.
    // Input: None.
    // Output: None.
    private void initViews() {
        // Direct listener to open the group creation screen
        FloatingActionButton createGroupFab = findViewById(R.id.createGroupFab);
        createGroupFab.setOnClickListener(v ->
                startActivity(new Intent(this, CreateGroupActivity.class)));

        // Settings button opens an options menu
        ImageView settingsBtn = findViewById(R.id.settingsBtn);
        settingsBtn.setOnClickListener(this::showMenu);

        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerViewGroups = findViewById(R.id.homeRecyclerView);
        recyclerViewGroups.setLayoutManager(new LinearLayoutManager(this));

        groupAdapter = new GroupAdapter(groupList, this::onGroupClick);
        recyclerViewGroups.setAdapter(groupAdapter);
    }

    // Refreshes the user's group list every time the user returns to this screen.
    // Input: None.
    // Output: None.
    @Override
    protected void onResume() {
        super.onResume();
        loadUserGroups();
    }

    // Fetches all group documents from Firestore and triggers the processing of each group.
    // Input: None.
    // Output: None.
    private void loadUserGroups() {
        if (currentUsername.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("groups").get()
                .addOnSuccessListener(snapshots -> {
                    groupList.clear();
                    // Iterate through every group in the database to filter for the user's membership
                    for (QueryDocumentSnapshot doc : snapshots) {
                        processGroupDocument(doc);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error loading groups", e));
    }

    // Handles the complex data extraction for a single group document and adds it to the list if the user is a member.
    // Input: QueryDocumentSnapshot doc (the raw database document).
    // Output: None.
    private void processGroupDocument(QueryDocumentSnapshot doc) {
        try {
            // Firestore stores the member list as an array of objects/maps
            List<Object> membersList = (List<Object>) doc.get("members");
            if (membersList == null) {
                return;
            }

            Vector<User> members = parseMembers(membersList);
            // Only add the group to the home screen if the current user is actually in it
            if (!isUserInGroup(members)) {
                return;
            }

            groupList.add(createGroupFromDoc(doc, members));
        } catch (Exception e) {
            Log.e("FIREBASE", "Error parsing group", e);
        }
    }

    // Converts a list of raw objects from Firestore into a typed Vector of User objects.
    // Input: List<Object> membersList (the raw list from the database).
    // Output: Vector<User> (the parsed user objects).
    private Vector<User> parseMembers(List<Object> membersList) {
        Vector<User> members = new Vector<>();
        for (Object obj : membersList) {
            if (!(obj instanceof Map)) continue;

            // Cast the raw object to a Map to access the user data keys
            Map<String, Object> map = (Map<String, Object>) obj;
            members.add(new User(
                    (String) map.get("username"),
                    (String) map.get("password"),
                    (String) map.get("email")
            ));
        }
        return members;
    }

    // Checks if the current username exists within a given vector of group members.
    // Input: Vector<User> members (the members to check).
    // Output: boolean (true if the user is a member, false otherwise).
    private boolean isUserInGroup(Vector<User> members) {
        for (User m : members) {
            if (m.getUsername().equals(currentUsername)) return true;
        }
        return false;
    }

    // Constructs a Group object using data from a Firestore document and a pre-parsed list of members.
    // Input: QueryDocumentSnapshot doc (database document), Vector<User> members (pre-parsed members).
    // Output: Group (the finalized Group instance).
    private Group createGroupFromDoc(QueryDocumentSnapshot doc, Vector<User> members) {
        String owner = doc.getString("owner");
        Group g = new Group(
                doc.getLong("groupId").intValue(),
                doc.getString("groupName"),
                owner,
                doc.getString("description"),
                new Vector<>(),
                new User(owner, "", "")
        );
        g.setMembers(members);
        return g;
    }

    // Updates the visibility of the list and empty state text based on whether groups were found.
    // Input: None.
    // Output: None.
    private void updateUI() {
        groupAdapter.updateGroups(groupList);
        boolean isEmpty = groupList.isEmpty();
        // Toggle empty state message if the user has no groups
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerViewGroups.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // Navigates to the group chat screen for the selected group.
    // Input: Group group (the group that was clicked).
    // Output: None.
    private void onGroupClick(Group group) {
        Intent intent = new Intent(this, GroupChatActivity.class);
        // Pass group identifiers to the chat activity
        intent.putExtra("groupId", group.getGroupId());
        intent.putExtra("groupName", group.getGroupName());
        startActivity(intent);
    }

    // Displays a popup menu at the settings button location with options for Profile and Settings.
    // Input: View v (the view that was clicked to trigger the menu).
    // Output: None.
    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                startActivity(new Intent(this, com.example.chatit.Activities.ProfileActivity.class));
            } else if (id == R.id.menu_settings) {
                startActivity(new Intent(this, com.example.chatit.Activities.SettingsActivity.class));
            }
            return true;
        });
        popup.show();
    }

    // Clears the user's logged-in status in preferences and returns to the introduction activity.
    // Input: None.
    // Output: None.
    private void userLogout() {
        // Update local storage to reflect that the user has logged out
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("isLoggedIn", false).apply();
        startActivity(new Intent(this, IntroductionActivity.class));
        finish();
    }

    // Checks the local shared preferences to see if the login flag is set to true.
    // Input: Context context (the application context).
    // Output: boolean (true if the "isLoggedIn" flag is true).
    public boolean isUserLoggedIn(Context context) {
        return context.getSharedPreferences("UserPrefs", MODE_PRIVATE).getBoolean("isLoggedIn", false);
    }
}
