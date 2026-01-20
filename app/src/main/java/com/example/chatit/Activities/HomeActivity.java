package com.example.chatit.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.view.ContextThemeWrapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatit.Adapters.GroupAdapter;
import com.example.chatit.R;
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

    private RecyclerView groupsRecyclerView;
    private TextView emptyStateText;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;
    private String currentUsername;

    // This function is responsible for creating the home screen and checking if the user is logged in.
    // Input: Bundle savedInstanceState (the saved state of the activity).
    // Output: None.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isSessionValid()) {
            return;
        }

        setContentView(R.layout.home_view);
        initializeData();
        initializeViews();
        loadGroupsFromFirestore();

        startService(new Intent(this, com.example.chatit.Services.MessageNotificationService.class));
    }

    // This function is responsible for verifying if the user has an active login session.
    // Input: None.
    // Output: boolean (returns true if the user is logged in).
    private boolean isSessionValid() {
        if (checkLoginFlag(this)) {
            return true;
        }

        startActivity(new Intent(this, IntroductionActivity.class));
        finish();
        return false;
    }

    // This function is responsible for initializing the local data structures and getting the username.
    // Input: None.
    // Output: None.
    private void initializeData() {
        currentUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", "");
        groupList = new ArrayList<>();
    }

    // This function is responsible for connecting properties to the XML layout and setting up listeners.
    // Input: None.
    // Output: None.
    private void initializeViews() {
        FloatingActionButton createGroupButton = findViewById(R.id.createGroupFab);
        createGroupButton.setOnClickListener(v ->
                startActivity(new Intent(this, CreateGroupActivity.class)));

        ImageView settingsButton = findViewById(R.id.settingsBtn);
        settingsButton.setOnClickListener(this::showPopupMenu);

        emptyStateText = findViewById(R.id.tvEmptyState);
        groupsRecyclerView = findViewById(R.id.homeRecyclerView);
        groupsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupAdapter = new GroupAdapter(groupList, this::onGroupItemClicked);
        groupsRecyclerView.setAdapter(groupAdapter);
    }

    // This function is responsible for refreshing the group list whenever the user returns to the screen.
    // Input: None.
    // Output: None.
    @Override
    protected void onResume() {
        super.onResume();
        loadGroupsFromFirestore();
    }

    // This function is responsible for fetching all groups from the database.
    // Input: None.
    // Output: None.
    private void loadGroupsFromFirestore() {
        if (currentUsername.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("groups").get()
                .addOnSuccessListener(snapshots -> {
                    groupList.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        handleGroupDocument(document);
                    }
                    refreshInterface();
                })
                .addOnFailureListener(error -> Log.e("FIREBASE", "Error loading groups", error));
    }

    // This function is responsible for checking if a specific group document should be shown to the user.
    // Input: QueryDocumentSnapshot document.
    // Output: None.
    private void handleGroupDocument(QueryDocumentSnapshot document) {
        try {
            List<Object> memberDataList = (List<Object>) document.get("members");
            if (memberDataList == null) return;

            Vector<User> memberObjects = parseMemberData(memberDataList);
            if (!isCurrentUserInMemberList(memberObjects)) return;

            groupList.add(convertDocumentToGroupObject(document, memberObjects));
        } catch (Exception error) {
            Log.e("FIREBASE", "Error processing group", error);
        }
    }

    // This function is responsible for turning raw database member data into User objects.
    // Input: List<Object> memberData.
    // Output: Vector<User> (list of users).
    private Vector<User> parseMemberData(List<Object> memberData) {
        Vector<User> users = new Vector<>();
        for (Object item : memberData) {
            if (!(item instanceof Map)) continue;

            Map<String, Object> data = (Map<String, Object>) item;
            users.add(new User(
                    (String) data.get("username"),
                    (String) data.get("password"),
                    (String) data.get("email")
            ));
        }
        return users;
    }

    // This function is responsible for checking if the current user belongs to a list of members.
    // Input: Vector<User> members.
    // Output: boolean (true if user is in list).
    private boolean isCurrentUserInMemberList(Vector<User> members) {
        for (User user : members) {
            if (user.getUsername().equals(currentUsername)) return true;
        }
        return false;
    }

    // This function is responsible for creating a Group object from a Firestore document.
    // Input: QueryDocumentSnapshot document, Vector<User> members.
    // Output: Group (the finalized object).
    private Group convertDocumentToGroupObject(QueryDocumentSnapshot document, Vector<User> members) {
        String ownerName = document.getString("owner");
        Group group = new Group(
                document.getLong("groupId").intValue(),
                document.getString("groupName"),
                ownerName,
                document.getString("description"),
                new Vector<>(),
                new User(ownerName, "", "")
        );
        group.setMembers(members);
        group.setImageBase64(document.getString("imageBase64"));
        return group;
    }

    // This function is responsible for updating the visibility of the group list and the empty message.
    // Input: None.
    // Output: None.
    private void refreshInterface() {
        groupAdapter.updateGroups(groupList);
        boolean listIsEmpty = groupList.isEmpty();
        
        emptyStateText.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
        groupsRecyclerView.setVisibility(listIsEmpty ? View.GONE : View.VISIBLE);
    }

    // This function is responsible for opening the chat screen when a group is clicked.
    // Input: Group selectedGroup.
    // Output: None.
    private void onGroupItemClicked(Group selectedGroup) {
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("groupId", selectedGroup.getGroupId());
        intent.putExtra("groupName", selectedGroup.getGroupName());
        startActivity(intent);
    }

    // This function is responsible for showing the settings menu with Profile and Settings options.
    // Input: View anchorView (the button that was clicked).
    // Output: None.
    private void showPopupMenu(View anchorView) {
        Context themeContext = new ContextThemeWrapper(this, R.style.CustomPopupMenu);
        PopupMenu menu = new PopupMenu(themeContext, anchorView);
        menu.getMenuInflater().inflate(R.menu.home_menu, menu.getMenu());

        try {
            Field fieldMenuPopup = menu.getClass().getDeclaredField("mPopup");
            fieldMenuPopup.setAccessible(true);
            Object menuPopupHelper = fieldMenuPopup.get(menu);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);
        } catch (Exception error) {
            Log.e("MENU", "Error displaying icons", error);
        }

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.menu_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            return true;
        });
        menu.show();
    }

    // This function is responsible for logging the user out and returning to the introduction activity.
    // Input: None.
    // Output: None.
    private void logoutUser() {
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("isLoggedIn", false).apply();
        startActivity(new Intent(this, IntroductionActivity.class));
        finish();
    }

    // This function is responsible for checking the login flag in shared preferences.
    // Input: Context context.
    // Output: boolean (true if logged in).
    public boolean checkLoginFlag(Context context) {
        return context.getSharedPreferences("UserPrefs", MODE_PRIVATE).getBoolean("isLoggedIn", false);
    }
}
