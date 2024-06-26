package com.example.calmshperef1;

import static com.example.calmshperef1.cords.FirebaseCords.MAIN_CHAT_DATABASE;
import static com.example.calmshperef1.cords.FirebaseCords.mAuth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calmshperef1.adapter.ChatAdapter;
import com.example.calmshperef1.fcm.SendPushNotification;
import com.example.calmshperef1.model.ChatModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
//import com.theartofdev.edmodo.cropper.CropImage;
//import com.theartofdev.edmodo.cropper.CropImageView;


//import static com.monstertechno.chatroom.cords.FirebaseCords.MAIN_CHAT_DATABASE;
//import static com.monstertechno.chatroom.cords.FirebaseCords.mAuth;

public class ChatMain extends AppCompatActivity {

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser==null){
            startActivity(new Intent(this,LoginPage.class));
            finish();
        }
        chatAdapter.startListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }


@Override
public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.logout) {
        mAuth.signOut();
        startActivity(new Intent(this, LoginPage.class));
        return true;
    } else {
        return super.onOptionsItemSelected(item);
    }
}


    EditText chat_box;
    RecyclerView chat_list;

    ChatAdapter chatAdapter;
    public TextView text2;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        FirebaseMessaging.getInstance().subscribeToTopic("global_chat");


        chat_box = findViewById(R.id.chat_box);
        chat_list = findViewById(R.id.chat_list);



        initChatList();

    }

    private void initChatList() {
        chat_list.setHasFixedSize(true);
        chat_list.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,true));


        Query query = MAIN_CHAT_DATABASE.orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<ChatModel> option  = new FirestoreRecyclerOptions.Builder<ChatModel>()
                .setQuery(query,ChatModel.class)
                .build();
        chatAdapter = new ChatAdapter(option);
        chat_list.setAdapter(chatAdapter);
        chatAdapter.startListening();

    }

    public void addMessage(View view) {
        String message = chat_box.getText().toString();
        FirebaseUser user = mAuth.getCurrentUser();
        if(!TextUtils.isEmpty(message)){

            /*Generate messageID using the current date. */
            Date today = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String messageID = format.format(today);

            /*Getting user image from Google account*/
            String user_image_url = "";
            Uri photoUrl = user.getPhotoUrl();
            String originalUrl = "s96-c/photo.jpg";
            String resizeImageUrl = "s400-c/photo.jpg";
            if(photoUrl!=null){
                String photoPath = photoUrl.toString();
                user_image_url = photoPath.replace(originalUrl,resizeImageUrl);
            }

            HashMap<String,Object> messageObj = new HashMap<>();
            messageObj.put("message",message);
            messageObj.put("user_name",user.getDisplayName());
            messageObj.put("timestamp", new Date().getTime());
            messageObj.put("messageID",messageID);
            messageObj.put("chat_image","");
            messageObj.put("user_image_url",user_image_url);


            MAIN_CHAT_DATABASE.document(messageID).set(messageObj).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("global_chat");
                        SendPushNotification sendPushNotification = new SendPushNotification(ChatMain.this);
                        sendPushNotification.startPush(user.getDisplayName(),message,"global_chat");
                        chat_box.setText("");
                    }else {
                        Toast.makeText(ChatMain.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public void OpenExplorer(View view) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
//            ChoseImage();
        }else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},20);
            }else {
                Toast.makeText(this, "Storage permission needed", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},20);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 20){
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
//                ChoseImage();
            }else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void ChoseImage() {
//        CropImage.activity()
//                .setGuidelines(CropImageView.Guidelines.ON)
//                .start(ChatMain.this);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//            Log.d("TAG", "onActivityResult: "+result);
//            if(resultCode == RESULT_OK){
//                imageUri = result.getUri();
//                Log.d("TAG", "onActivityResult: "+imageUri);
//                startActivity(new Intent(ChatMain.this,ImageUploadPreview.class)
//                        .putExtra("image_uri",imageUri.toString()));
//            }else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
//                Toast.makeText(this, result.getError().getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    public void Finish(View view) {
        long currentTime  = new Date().getTime();
        new SaveState(this).setClickTime(currentTime);
        finish();
    }
}
