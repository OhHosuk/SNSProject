package com.example.sns_project;

import static com.example.sns_project.Action.START_LOCATION_SERVICE;
import static com.google.firebase.messaging.Constants.MessagePayloadKeys.SENDER_ID;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sns_project.CameraGallerys.GalleryAdapter;
import com.example.sns_project.Posts.PostInfo;
import com.example.sns_project.Posts.WritePostActivity;
import com.example.sns_project.R;
import com.example.sns_project.SignLogins.LoginActivity;
import com.example.sns_project.SignLogins.MemberInitActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BasicActivity {
    private static final String TAG = "MainActivity";
    private static TextView CurrentEmail, CurrentName, CurrentPhone, childTextview;
    private static DatabaseReference mDatabase;
    private static String ChildName, ChildEmail;
    private LinearLayout ButtonLayout;

    private static final int REQUEST_CODE = 1;
    Button Startbutton;
    Button Stopbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 회원정보가 존재하는지 확인
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document != null){
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            CurrentEmail = (TextView)findViewById(R.id.EmailText);
                            CurrentEmail.setText("접속한 이메일 : "+user.getEmail());
                            CurrentName = (TextView) findViewById(R.id.NameText);
                            CurrentName.setText("접속한 이름 : " + document.getData().get("name"));
                            CurrentPhone = (TextView) findViewById(R.id.PNumText);
                            CurrentPhone.setText("접속한 이름 : " + document.getData().get("phoneNumber"));
                            mDatabase = FirebaseDatabase.getInstance().getReference();
                            mDatabase.child("users").child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    if (!task.isSuccessful()) {
                                        Log.e("firebase", "Error getting data", task.getException());
                                    }
                                    else {
                                        Log.d("firebase", String.valueOf(task.getResult().getValue()));
                                        childTextview = (TextView)findViewById(R.id.childTextview);
                                        childTextview.setText(String.valueOf(task.getResult().getValue()));
                                    }
                                }
                            });

                        } else {
                            Log.d(TAG, "No such document");
                            finish();
                            myStartActivity(MemberInitActivity.class);
                        }
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        //게시글 화면에 보여주기
        db.collection("posts")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<PostInfo> postList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                postList.add(new PostInfo(
                                        document.getData().get("title").toString(),
                                        (ArrayList<String>) document.getData().get("contents"),
                                        document.getData().get("publisher").toString(),
                                        new Date(document.getDate("createdAt").getTime())));
                            }

                            RecyclerView recyclerView = findViewById(R.id.recyclerView);
                            recyclerView.setHasFixedSize(true);
                            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

                            RecyclerView.Adapter mAdapter = new MainAdapter(MainActivity.this, postList);
                            recyclerView.setAdapter(mAdapter);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });


        // 버튼 리스너
        findViewById(R.id.logoutButton).setOnClickListener(onClickListener);
        findViewById(R.id.floatingActionButton).setOnClickListener(onClickListener);
        findViewById(R.id.SendSignalButton).setOnClickListener(onClickListener);
        findViewById(R.id.SendtoParent).setOnClickListener(onClickListener);

        //자식 아이디 일때만 버튼 보이기
        if(user.getUid().equals("be8dflcIjBg0e88Jg7JtkPd1z693")){
            ButtonLayout = findViewById(R.id.ButtonLayout);
            ButtonLayout.setVisibility(View.GONE);
        }
        Startbutton = findViewById(R.id.Startbutton);
        Startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                //앱에 권한이 없는 경우 이 메서드는 PackageManager.PERMISSION_DENIED를 반환하고,
                //앱이 사용자에게 명시적으로 권한을 요청해야 합니다. 즉, IF문을 통해 이를 비교하여 권한을 받았는지 못받았는지를 가려낼 수 있습니다.
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) { //위치 권한 확인
                    //위치 권한 요청
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                } else {
                    startLocationService();
                }
            }
        });


        Stopbutton = findViewById(R.id.Stopbutton);
        Stopbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationService();
            }
        });
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.logoutButton:
                    FirebaseAuth.getInstance().signOut();
                    myStartActivity(LoginActivity.class);
                    break;
                case R.id.floatingActionButton:
                    myStartActivity(WritePostActivity.class);
                    break;
                case R.id.SendSignalButton:
                    //데이터 보내고 받기
                    EditText ParentId = (EditText)findViewById(R.id.ParentId);
                    SendData(ParentId.getText().toString());
                    break;
                case R.id.SendtoParent:
                    //데이터 보내고 받기
                    SendData("be8dflcIjBg0e88Jg7JtkPd1z693");
                    break;
            }
        }
    };

    private void startLocationService() {
        Intent startIntent = new Intent(getApplicationContext(), LocationService.class);
        startIntent.setAction(Action.START_LOCATION_SERVICE);
        startService(startIntent);
        Toast.makeText(this, "위치 업데이트 실행", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationService() {
        Intent stopIntent = new Intent(getApplicationContext(), LocationService.class);
        stopIntent.setAction(Action.STOP_LOCATION_SERVICE);
        startService(stopIntent);
        Toast.makeText(this, "위치 업데이트 중지", Toast.LENGTH_SHORT).show();
    }

    private void nullAction(){

    }

    //grantResults는 요청에 OK를 했을 때의 정보를 갖습니다.
    //grantResults[0]에는 PackageManager.PERMISSION_GRANTED 값을 갖습니다.
    //만약 요청이 거절되었다면 grantResults에는 아무런 데이터도 존재하지 않게됩니다.
    //그렇게 IF문은 권한에 대해 OK를 받았을 때 수행할 작업이 들어가며 ELSE에는 권한에 대해 CANCEL을 받았을 때의 코드를 넣어줍니다.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        startLocationService();
                    }
                } else {
                    Toast.makeText(this, "Permission  denied", Toast.LENGTH_SHORT).show();
                }
        }
        Toast.makeText(this, "asdafdgfdfsdsfdf", Toast.LENGTH_LONG).show();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void myStartActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }


    public class User {
        String name;
        String email;
        String message;
        Date date;

        public User(String name, String email, String message, Date date) {
            this.name = name;
            this.email = email;
            this.message = message;
            this.date = date;
        }

        public String getMessage(){
            return message;
        }
        public Date getDate(){return date;}
    }
    public void writeNewUser(String userId, String name, String email, String message) {
        User user = new User(name, email, message, new Date());

        mDatabase.child("users").child(userId).child(name).child(user.getDate().toString()).setValue(message);
    }

    private void SendData(String ParrentId){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRefParrent = db.collection("users").document(ParrentId);
        DocumentReference docRefChild = db.collection("users").document(user.getUid());

        docRefChild.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document != null){
                        if (document.exists()) {
                            ChildName = (String) document.getData().get("name");
                            ChildEmail = user.getEmail();
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        docRefParrent.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document != null){
                        if (document.exists()) {
                            Log.d("부모 이름",document.getData().get("name").toString());
                            Log.d("부모 번호",document.getData().get("phoneNumber").toString());
                            writeNewUser(ParrentId,ChildName,ChildEmail,"테스트메세지입니다1");

                        } else {
                            Log.d(TAG, "No such document");
                        }
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }

    private void RealtimeData(String userId,String name, String email){
        // 파이어베이스 실시간 데이터 다루기
        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        //DatabaseReference myRef = database.getReference("message");
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").child(userId).child(name).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Log.d("데이터",dataSnapshot.getValue().toString());
                //notificationText = (TextView) findViewById(R.id.notificationText);
                //notificationText.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

}
