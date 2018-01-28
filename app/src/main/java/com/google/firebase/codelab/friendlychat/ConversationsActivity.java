package com.google.firebase.codelab.friendlychat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationsActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener
//        GoogleApiClient.ConnectionCallbacks,
//        LocationListener
{

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        TextView placeTextView;
        CircleImageView placeImageView;

        public ConversationViewHolder(View v) {
            super(v);
            placeTextView = (TextView) itemView.findViewById(R.id.placeTextView);
//            placeImageView = (CircleImageView) itemView.findViewById(R.id.placeImageView);
        }
    }

    private static final String TAG = "ConversationsActivity";
    public static final String CONVERSATIONS_CHILD = "conversations";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    public static final String ANONYMOUS = "anonymous";
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    private String mUsername;
    private String mPhotoUrl;

    private SharedPreferences mSharedPreferences;
    private ProgressBar mProgressBar;

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<FriendlyConversation, ConversationsActivity.ConversationViewHolder> mFirebaseAdapter;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    //    private AdView mAdView;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private GoogleApiClient mGoogleApiClient;

    // Geo Location
    private DatabaseReference peopleRef;
    private DatabaseReference locationsRef;
    private DatabaseReference peopleAccessRef;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    private String mLastUpdateTime;

    private AccessRight mAccessRight;

    // Heatmap
    Map<String, LocationDataPoint> locationMap = new HashMap<String, LocationDataPoint>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        setTitle("Conversations");


        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        // Demo
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();


        mUsername = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
//                .addApi(LocationServices.API)
//                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.conversationsRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
//        mLinearLayoutManager.setStackFromEnd(true);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        SnapshotParser<FriendlyConversation> parser = new SnapshotParser<FriendlyConversation>() {
            @Override
            public FriendlyConversation parseSnapshot(DataSnapshot dataSnapshot) {
                FriendlyConversation friendlyConversation = dataSnapshot.getValue(FriendlyConversation.class);
                if (friendlyConversation != null) {
                    friendlyConversation.setId(dataSnapshot.getKey());
                }
                return friendlyConversation;
            }
        };

        DatabaseReference messagesRef = mFirebaseDatabaseReference.child(CONVERSATIONS_CHILD);

        FirebaseRecyclerOptions<FriendlyConversation> options =
                new FirebaseRecyclerOptions.Builder<FriendlyConversation>()
                        .setQuery(messagesRef, parser)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyConversation, ConversationsActivity.ConversationViewHolder>(options) {

            @Override
            public ConversationsActivity.ConversationViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new ConversationsActivity.ConversationViewHolder(inflater.inflate(R.layout.item_conversation, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final ConversationsActivity.ConversationViewHolder viewHolder,
                                            int position,
                                            final FriendlyConversation friendlyConversation) {

//                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyConversation.getName() != null) {
                    viewHolder.placeTextView.setText(friendlyConversation.getName());
                    viewHolder.placeTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.placeTextView.setClickable(true);
                    viewHolder.placeTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
//                            Toast.makeText(getBaseContext(), friendlyConversation.getId(), Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(ConversationsActivity.this, MainActivity.class);
                            intent.putExtra(Intent.EXTRA_TEXT, friendlyConversation.getId());
                            String access = "";
                            if( mAccessRight != null) {
                                access = mAccessRight.getAccessRight();
                            }
                            intent.putExtra("accessRight", access);
                            intent.putExtra("conversationTitle", friendlyConversation.getName());

                            startActivity(intent);
                        }
                    });

//                    viewHolder.placeImageView.setVisibility(ImageView.GONE);
                }
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyConversationCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
//                if (lastVisiblePosition == -1 ||
//                        (positionStart >= (friendlyConversationCount - 1) && lastVisiblePosition == (positionStart - 1))) {
//                    mMessageRecyclerView.scrollToPosition(positionStart);
//                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        // Initialize and request AdMob ad.
//        mAdView = (AdView) findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);

        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize Firebase Remote Config.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", 10L);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        peopleRef = mFirebaseDatabaseReference.child("people");
        mFirebaseAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mFirebaseUser == null) {
                    mFirebaseAdapter.stopListening();
                    stopLocationUpdates();
                } else {

                }
            }
        });
        peopleRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());
                if(!dataSnapshot.getValue().toString().equals("accessRight")) {
                    String latitude = dataSnapshot.getValue(PeopleContract.class).getLatitude();
                    String longitude = dataSnapshot.getValue(PeopleContract.class).getLongitude();
                    String name = dataSnapshot.getValue(PeopleContract.class).getPerson();
                    locationMap.put(name, new LocationDataPoint(latitude, longitude));
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());
                if(!dataSnapshot.getValue().equals("accessRight")) {
                    String latitude = dataSnapshot.getValue(PeopleContract.class).getLatitude();
                    String longitude = dataSnapshot.getValue(PeopleContract.class).getLongitude();
                    String name = dataSnapshot.getValue(PeopleContract.class).getPerson();
                    locationMap.put(name, new LocationDataPoint(latitude, longitude));
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());
                if(!dataSnapshot.getValue().equals("accessRight")) {
                    String name = dataSnapshot.getValue(PeopleContract.class).getPerson();
                    locationMap.remove(name);
                }

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
//                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                Toast.makeText(ConversationsActivity.this, "Failed to load comments.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        peopleAccessRef = mFirebaseDatabaseReference.child("people").child(mFirebaseUser.getDisplayName()).child("access");

        peopleAccessRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded: AccessRight " + dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged: AccessRight " + dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
//                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                Toast.makeText(ConversationsActivity.this, "Failed to load comments.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        locationsRef = mFirebaseDatabaseReference.child("locations");

        locationsRef.child("HackED").setValue(new LocationContract("HackEd", "53.528106", "-113.529119","22"));
        locationsRef.child("SouthSide").setValue(new LocationContract("SouthSide", "53.53712766", "-113.3679195","21"));
        locationsRef.child("RiverSide").setValue(new LocationContract("RiverSide", "53.52726929", "-113.4959407","13"));

        // Fetch remote config.
//        fetchConfig();

//        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
//        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
//                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
//        mMessageEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (charSequence.toString().trim().length() > 0) {
//                    mSendButton.setEnabled(true);
//                } else {
//                    mSendButton.setEnabled(false);
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//            }
//        });
//
//        mAddMessageImageView = (ImageView) findViewById(R.id.addMessageImageView);
//        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType("image/*");
//                startActivityForResult(intent, REQUEST_IMAGE);
//            }
//        });
//
//        mSendButton = (Button) findViewById(R.id.sendButton);
//        mSendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername,
//                        mPhotoUrl, null);
//                mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(friendlyMessage);
//                mMessageEditText.setText("");
//                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
//            }
//        });

        peopleRef.child("Kristen Whitaker").setValue(new PeopleContract(String.valueOf(53.52692547), String.valueOf(-113.5306006), "Kristen Whitaker"));
        peopleRef.child("Anthony Rocha").setValue(new PeopleContract(String.valueOf(53.52710565), String.valueOf(-113.5294103), "Anthony Rocha"));
        peopleRef.child("Eugenia Fleming").setValue(new PeopleContract(String.valueOf(53.52657355), String.valueOf(-113.5290497), "Eugenia Fleming"));
        peopleRef.child("Amanda Blanchard").setValue(new PeopleContract(String.valueOf(53.52728209), String.valueOf(-113.5302487), "Amanda Blanchard"));
        peopleRef.child("Devin Reid").setValue(new PeopleContract(String.valueOf(53.52647147), String.valueOf(-113.5286872), "Devin Reid"));
        peopleRef.child("Allen Shields").setValue(new PeopleContract(String.valueOf(53.52488384), String.valueOf(-113.5293381), "Allen Shields"));
        peopleRef.child("Alexis Garrison").setValue(new PeopleContract(String.valueOf(53.52543962), String.valueOf(-113.5283742), "Alexis Garrison"));
        peopleRef.child("Jonah Rowland").setValue(new PeopleContract(String.valueOf(53.5258327), String.valueOf(-113.5295105), "Jonah Rowland"));
        peopleRef.child("Burke Brooks").setValue(new PeopleContract(String.valueOf(53.52524303), String.valueOf(-113.5307861), "Burke Brooks"));
        peopleRef.child("Clio Floyd").setValue(new PeopleContract(String.valueOf(53.52585316), String.valueOf(-113.53085), "Clio Floyd"));
        peopleRef.child("Leslie Orr").setValue(new PeopleContract(String.valueOf(53.52712263), String.valueOf(-113.5301731), "Leslie Orr"));
        peopleRef.child("Lars Phelps").setValue(new PeopleContract(String.valueOf(53.52687966), String.valueOf(-113.5304511), "Lars Phelps"));
        peopleRef.child("Callum Barrera").setValue(new PeopleContract(String.valueOf(53.52660654), String.valueOf(-113.5295541), "Callum Barrera"));
        peopleRef.child("Yvette Henry").setValue(new PeopleContract(String.valueOf(53.52614314), String.valueOf(-113.52908), "Yvette Henry"));
        peopleRef.child("Kathleen Stafford").setValue(new PeopleContract(String.valueOf(53.52584947), String.valueOf(-113.5285902), "Kathleen Stafford"));
        peopleRef.child("Elizabeth Mcdaniel").setValue(new PeopleContract(String.valueOf(53.52630283), String.valueOf(-113.529117), "Elizabeth Mcdaniel"));
        peopleRef.child("Plato Randolph").setValue(new PeopleContract(String.valueOf(53.52579016), String.valueOf(-113.5300731), "Plato Randolph"));
        peopleRef.child("Alika Weber").setValue(new PeopleContract(String.valueOf(53.52642727), String.valueOf(-113.5291429), "Alika Weber"));
        peopleRef.child("George Snyder").setValue(new PeopleContract(String.valueOf(53.52624139), String.valueOf(-113.5298465), "George Snyder"));
        peopleRef.child("Vincent Merrill").setValue(new PeopleContract(String.valueOf(53.52585321), String.valueOf(-113.5303515), "Vincent Merrill"));
        peopleRef.child("Bert Sparks").setValue(new PeopleContract(String.valueOf(53.52644538), String.valueOf(-113.5287856), "Bert Sparks"));
        peopleRef.child("Baker Yates").setValue(new PeopleContract(String.valueOf(53.5267126), String.valueOf(-113.4901315), "Baker Yates"));
        peopleRef.child("Brendan Joyner").setValue(new PeopleContract(String.valueOf(53.52488669), String.valueOf(-113.4960012), "Brendan Joyner"));
        peopleRef.child("Idona Casey").setValue(new PeopleContract(String.valueOf(53.52708301), String.valueOf(-113.4963967), "Idona Casey"));
        peopleRef.child("Carson Neal").setValue(new PeopleContract(String.valueOf(53.52579627), String.valueOf(-113.4909967), "Carson Neal"));
        peopleRef.child("Deacon Black").setValue(new PeopleContract(String.valueOf(53.52701089), String.valueOf(-113.4937844), "Deacon Black"));
        peopleRef.child("Colton Wade").setValue(new PeopleContract(String.valueOf(53.52581971), String.valueOf(-113.4901064), "Colton Wade"));
        peopleRef.child("Ivy Sellers").setValue(new PeopleContract(String.valueOf(53.52728837), String.valueOf(-113.4917536), "Ivy Sellers"));
        peopleRef.child("Vance Rowe").setValue(new PeopleContract(String.valueOf(53.52726929), String.valueOf(-113.4959407), "Vance Rowe"));
        peopleRef.child("Phoebe Duffy").setValue(new PeopleContract(String.valueOf(53.52540234), String.valueOf(-113.4941087), "Phoebe Duffy"));
        peopleRef.child("Rina Mason").setValue(new PeopleContract(String.valueOf(53.52581299), String.valueOf(-113.4963677), "Rina Mason"));
        peopleRef.child("Alexandra Roberson").setValue(new PeopleContract(String.valueOf(53.52664528), String.valueOf(-113.4939421), "Alexandra Roberson"));
        peopleRef.child("Martina Oliver").setValue(new PeopleContract(String.valueOf(53.52631157), String.valueOf(-113.4928197), "Martina Oliver"));
        peopleRef.child("Kasper Trevino").setValue(new PeopleContract(String.valueOf(53.5250771), String.valueOf(-113.4940697), "Kasper Trevino"));
        peopleRef.child("Curran Trujillo").setValue(new PeopleContract(String.valueOf(53.53819347), String.valueOf(-113.3689149), "Curran Trujillo"));
        peopleRef.child("Ivan Mccullough").setValue(new PeopleContract(String.valueOf(53.54514557), String.valueOf(-113.371161), "Ivan Mccullough"));
        peopleRef.child("Mark Wright").setValue(new PeopleContract(String.valueOf(53.54259618), String.valueOf(-113.3741042), "Mark Wright"));
        peopleRef.child("Maisie Best").setValue(new PeopleContract(String.valueOf(53.53975244), String.valueOf(-113.389527), "Maisie Best"));
        peopleRef.child("Kelly Sawyer").setValue(new PeopleContract(String.valueOf(53.54099508), String.valueOf(-113.383665), "Kelly Sawyer"));
        peopleRef.child("Wyoming Cannon").setValue(new PeopleContract(String.valueOf(53.54488971), String.valueOf(-113.3657872), "Wyoming Cannon"));
        peopleRef.child("Joel Sloan").setValue(new PeopleContract(String.valueOf(53.53521367), String.valueOf(-113.3849438), "Joel Sloan"));
        peopleRef.child("Diana Crosby").setValue(new PeopleContract(String.valueOf(53.53197414), String.valueOf(-113.3658684), "Diana Crosby"));
        peopleRef.child("Zena Clayton").setValue(new PeopleContract(String.valueOf(53.53710857), String.valueOf(-113.385264), "Zena Clayton"));
        peopleRef.child("Cade Terry").setValue(new PeopleContract(String.valueOf(53.53940341), String.valueOf(-113.3596979), "Cade Terry"));
        peopleRef.child("Fiona Arnold").setValue(new PeopleContract(String.valueOf(53.53712766), String.valueOf(-113.3679195), "Fiona Arnold"));
        peopleRef.child("Beck Cameron").setValue(new PeopleContract(String.valueOf(53.53136291), String.valueOf(-113.389389), "Beck Cameron"));
        peopleRef.child("Zeus Lyons").setValue(new PeopleContract(String.valueOf(53.53270094), String.valueOf(-113.3721102), "Zeus Lyons"));
        peopleRef.child("Jena Singleton").setValue(new PeopleContract(String.valueOf(53.53999), String.valueOf(-113.3851244), "Jena Singleton"));
        peopleRef.child("Abel Jordan").setValue(new PeopleContract(String.valueOf(53.54171884), String.valueOf(-113.3862147), "Abel Jordan"));
        peopleRef.child("Jamal Gregory").setValue(new PeopleContract(String.valueOf(53.53767375), String.valueOf(-113.3640804), "Jamal Gregory"));
        peopleRef.child("Victor Houston").setValue(new PeopleContract(String.valueOf(53.5344625), String.valueOf(-113.3802169), "Victor Houston"));
        peopleRef.child("Echo Bentley").setValue(new PeopleContract(String.valueOf(53.54217665), String.valueOf(-113.3847535), "Echo Bentley"));
        peopleRef.child("Xenos Gilliam").setValue(new PeopleContract(String.valueOf(53.53530355), String.valueOf(-113.3736759), "Xenos Gilliam"));
        peopleRef.child("Tad Howard").setValue(new PeopleContract(String.valueOf(53.53238956), String.valueOf(-113.373122), "Tad Howard"));
        peopleRef.child("Wyoming Vega").setValue(new PeopleContract(String.valueOf(53.53209083), String.valueOf(-113.3798783), "Wyoming Vega"));
        peopleRef.child("Odessa Koch").setValue(new PeopleContract(String.valueOf(53.53301015), String.valueOf(-113.3753975), "Odessa Koch"));
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }
            updateUI();
        }
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                Toast.makeText(ConversationsActivity.this, String.format(Locale.ENGLISH, "%s: %f\n%s: %f", "Latitude",
                        mCurrentLocation.getLatitude(), "Longitude", mCurrentLocation.getLongitude()), Toast.LENGTH_SHORT).show();

                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationOnFirebase();
                updateLocationUI();
            }
        };
    }

    private void updateLocationOnFirebase() {
        if(mFirebaseUser == null) return;
        peopleRef.child(mFirebaseUser.getDisplayName()).child("latitude").setValue(String.valueOf(mCurrentLocation.getLatitude()));
        peopleRef.child(mFirebaseUser.getDisplayName()).child("longitude").setValue(String.valueOf(mCurrentLocation.getLongitude()));
        Location loc = new Location("");
        loc.setLatitude(53.528106);
        loc.setLongitude(-113.529119);
        System.out.println(checkDistanceBetween(mCurrentLocation, loc));
        if(checkDistanceBetween(mCurrentLocation, loc) < 100) {
            mAccessRight = new AccessRight("HackEd");
            peopleRef.child(mFirebaseUser.getDisplayName()).child("access").setValue(new AccessRight("HackEd"));
        }

        loc.setLatitude(53.52726929);
        loc.setLongitude(-113.4959407);
        System.out.println(checkDistanceBetween(mCurrentLocation, loc));
        if(checkDistanceBetween(mCurrentLocation, loc) < 100)
            peopleRef.child(mFirebaseUser.getDisplayName()).child("access").push().setValue(new AccessRight("RiverSide"));

        loc.setLatitude(53.53712766);
        loc.setLongitude(-113.3679195);
        System.out.println(checkDistanceBetween(mCurrentLocation, loc));
        if(checkDistanceBetween(mCurrentLocation, loc) < 100)
            peopleRef.child(mFirebaseUser.getDisplayName()).child("access").push().setValue(new AccessRight("SouthSide"));



//        peopleRef.push().setValue(new PeopleContract(String.valueOf(mCurrentLocation.getLatitude()), String.valueOf(mCurrentLocation.getLongitude()), mFirebaseUser.getDisplayName()));
    }

    private double checkDistanceBetween(Location locOne, Location locTwo){
        locOne.setLatitude(mCurrentLocation.getLatitude());
        locOne.setLongitude(mCurrentLocation.getLongitude());
        return locOne.distanceTo(locTwo);
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");


                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(ConversationsActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(ConversationsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateUI();
                    }
                });
    }

    /**
     * Updates all UI fields.
     */
    private void updateUI() {
        setButtonsEnabledState();
        updateLocationUI();
    }

    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
//            mStartUpdatesButton.setEnabled(false);
//            mStopUpdatesButton.setEnabled(true);
        } else {
//            mStartUpdatesButton.setEnabled(true);
//            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */
    private void updateLocationUI() {
//        if (mCurrentLocation != null) {
//            Toast.makeText(this, String.format(Locale.ENGLISH, "%s: %f\n%s: %f", "Latitude",
//                    mCurrentLocation.getLatitude(), "Longitude", mCurrentLocation.getLongitude()), Toast.LENGTH_SHORT).show();
//        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                        setButtonsEnabledState();
                    }
                });
    }





    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationUpdates();
    }

    @Override
    protected void onStop(){
        mFirebaseAdapter.stopListening();

        stopLocationUpdates();
        super.onStop();
    }

    @Override
    public void onPause() {
//        if (mAdView != null) {
//            mAdView.pause();
//        }
        mFirebaseAdapter.stopListening();
        // Remove location updates to save battery.
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (mAdView != null) {
//            mAdView.resume();
//        }
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        updateUI();
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
//        if (mAdView != null) {
//            mAdView.destroy();
//        }
//        mFirebaseAdapter.stopListening();
//        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public String peopleToString(){
        System.out.println(locationMap.size());
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for(LocationDataPoint locationDataPoint: locationMap.values()){
            sb.append("{");
            sb.append("\"lat\" : ");
            sb.append(locationDataPoint.getLatitude());
            sb.append(", \"lng\" : ");
            sb.append(locationDataPoint.getLongitude());
            sb.append("} ,\n");
        }
        sb.delete(sb.lastIndexOf(","), sb.length());
        sb.append("\n");
        sb.append("]");
        System.out.println(sb.toString());

        return sb.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.heat_map_menu:
                Intent intent = new Intent(this, HeatmapsActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, peopleToString());
                startActivity(intent);
                return true;
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.crash_menu:
                FirebaseCrash.logcat(Log.ERROR, TAG, "crash caused");
                causeCrash();
                return true;
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mFirebaseUser = null;
                mUsername = ANONYMOUS;
                mPhotoUrl = null;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            case R.id.fresh_config_menu:
//                fetchConfig();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void causeCrash() {
        throw new NullPointerException("Fake null pointer exception");
    }

    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

//    // Fetch the config to determine the allowed length of messages.
//    public void fetchConfig() {
//        long cacheExpiration = 3600; // 1 hour in seconds
//        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
//        // server. This should not be used in release builds.
//        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
//            cacheExpiration = 0;
//        }
//        mFirebaseRemoteConfig.fetch(cacheExpiration)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
//                        mFirebaseRemoteConfig.activateFetched();
//                        applyRetrievedLengthLimit();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // There has been an error fetching the config
//                        Log.w(TAG, "Error fetching config", e);
//                        applyRetrievedLengthLimit();
//                    }
//                });
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    FriendlyConversation tempConversation = new FriendlyConversation(null, mUsername, mPhotoUrl,
                            LOADING_IMAGE_URL);
                    mFirebaseDatabaseReference.child(CONVERSATIONS_CHILD).push()
                            .setValue(tempConversation, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError,
                                                       DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        String key = databaseReference.getKey();
                                        StorageReference storageReference =
                                                FirebaseStorage.getInstance()
                                                        .getReference(mFirebaseUser.getUid())
                                                        .child(key)
                                                        .child(uri.getLastPathSegment());

                                        putImageInStorage(storageReference, uri, key);
                                    } else {
                                        Log.w(TAG, "Unable to write message to database.",
                                                databaseError.toException());
                                    }
                                }
                            });
                }
            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
//                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        } else if (requestCode == REQUEST_CHECK_SETTINGS){
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.i(TAG, "User agreed to make required location settings changes.");
                    // Nothing to do. startLocationupdates() gets called in onResume again.
                    break;
                case Activity.RESULT_CANCELED:
                    Log.i(TAG, "User chose not to make required location settings changes.");
                    mRequestingLocationUpdates = false;
                    updateUI();
                    break;
            }
        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(ConversationsActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            FriendlyMessage friendlyMessage =
                                    new FriendlyMessage(null, mUsername, mPhotoUrl,
                                            task.getResult().getDownloadUrl()
                                                    .toString());
                            mFirebaseDatabaseReference.child(CONVERSATIONS_CHILD).child(key)
                                    .setValue(friendlyMessage);
                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
//    private void applyRetrievedLengthLimit() {
//        Long friendly_msg_length = mFirebaseRemoteConfig.getLong("friendly_msg_length");
//        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
//        Log.d(TAG, "FML is: " + friendly_msg_length);
//    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(ConversationsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(ConversationsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }
}
