package com.vr_object.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vr_object.fixed.xnzrw24b.data.NamesBLE;

import java.util.HashMap;

/**
 * Created by Michael Lukin on 13.12.2017.
 */

public class DatabaseProxy {
    public static final String TAG = "DatabaseProxy";
    private DatabaseReference beaconsRef;

    public void CreateConnection() {
        ConnectReference();

        beaconsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object val = dataSnapshot.getValue();
                if (val instanceof HashMap) {
                    HashMap<String, String> hv = (HashMap<String, String>)val;
                    NamesBLE.setData(hv);
//                    Log.d(TAG, String.format("onDataChange: key: %s val: %s: ", dataSnapshot.getKey(), dataSnapshot.getValue().toString));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void PushData(String key, String val) {
        ConnectReference();

        beaconsRef.child(key).setValue(val);
    }

    private void ConnectReference() {
        if (beaconsRef != null) {
            return;
        }
        beaconsRef = FirebaseDatabase.getInstance().getReference().child("beacons");
    }
}
