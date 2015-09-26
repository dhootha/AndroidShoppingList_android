package com.kurt.kurtlawrence.lethehangover;


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.os.Handler;
import java.util.ArrayList;
import java.util.List;
import android.view.View;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import com.android.kurtlawrence.lethehangover.R;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.google.android.vending.licensing.AESObfuscator;


public class Check_list extends ListActivity {
    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;
    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAll06s57WCqWB+OXW4IQAajcQk7uWdxOawg4im56YxaxZbqZ5egSwl+vj8Uzv+fJSfkDK4nyKKjPk7mXg2P9EU+nNAboOzawP0prxbCQZ/VpnDb5l+fJh9A/ylLubUG0dqOu5dStXoCPHYvfrK1cHZjzFh0v6bHCxGqgC5JvfffCZGGqYENwitnTF9vRdUMoNV5cOhCBXq9/d2gnZUObYiIcLA58YLBs9rNL6qEXu/NJmTnIO5undiK8u/Js3rAYkIIeibNCch122aZvmediVPWWoPabOg+X8K6MvBVZldJ/6NRaf4yYTKpOzm0nvEVZiFNPd6P9qaUs2iodjUMlnmQIDAQAB";
    // Generate 20 random bytes, and put them here.
    private static final byte[] SALT = new byte[] {
            54, 65, 43, -128, -103, -57, 74, -64, 51, 102, -95,
            -45, 77, -45, -36, 45, -11, 32, 12, 89};
    private PebbleDictionary listForPebble;
    private PebbleKit.PebbleDataReceiver pebbleListHandler;
    private PebbleKit.PebbleAckReceiver pebbleAckHandler;
    private PebbleKit.PebbleNackReceiver pebbleNackHandler;
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("35f60a69-3b11-4568-853d-2d4f878dad89");
    private StringBuilder listString;
    private List<String> itemList;
    private List<Integer> statusList;
    private boolean hasCheckedItems;
    private int lastCheckedItem;
    private boolean acceptWatchData;
    private static final int MAX_NUMBER_OF_CHAR_FOR_ITEM = 14;
    private static final int UPDATE_COMM_DELAY_IN_SEC = 20;      // if nothing changes in three seconds, update the watch app
    private static final int CHAR_TO_TRANSFER_PER_COMM = 80;       // Arbitrary maximal characters that the bluetooth comm can carry.
    private static final int BUILD_NUMBER = 2;      // Set the constant build number
    private static final int DATA_RETRY_COUNTS = 3;     // Number of times to retry bluetooth sending when a nack happens
    private boolean changeHasOccurred;
    private int secondsSinceChange;
    private int numberOfCommTransfersReq;
    private int commTransferNumber;
    private int nacksReceived;
    private Handler mHandler;
    private StringBuilder pebbleConnectionMessage;


    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int reason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // Should allow user access.
            displayResult(getString(R.string.allow));
        }

        public void dontAllow(int reason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            displayResult(getString(R.string.dont_allow));

            if (reason == Policy.RETRY) {
                // If the reason received from the policy is RETRY, it was probably
                // due to a loss of connection with the service, so we should give the
                // user a chance to retry. So show a dialog to retry.
                //showDialog(DIALOG_RETRY);
            } else {
                // Otherwise, the user is not licensed to use this app.
                // Your response should always inform the user that the application
                // is not licensed, but your behavior at that point can vary. You might
                // provide the user a limited access version of your app or you can
                // take them to Google Play to purchase the app.
                //showDialog(DIALOG_GOTOMARKET);
            }
        }

        @Override
        public void applicationError(int reason) {
            dontAllow(reason);      // Just call don't allow
        }
    }
    private void doCheck() {
        //mCheckLicenseButton.setEnabled(false);
        setProgressBarIndeterminateVisibility(true);
        //mStatusText.setText(R.string.checking_license);
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    void redistributeChecklistItems(int fromVal, int toValInclusive) {
        // Function moves the from val item to the end of the display array and moves the other values forwards. Generally toVal is num of items
        String tempName = itemList.get(fromVal);		//Store the value that was sitting in the from value place
        int tempStatus = statusList.get(fromVal);
        if (fromVal == toValInclusive) {
            //APP_LOG(APP_LOG_LEVEL_ERROR, "Unusual situation in the redistribution, not sure how this plays out");
            // It plays out like this, do nothing and the end and from values just replace each other on the ends of if statements
        } else if (fromVal < toValInclusive) {
            // This is the typical send to back of array version
            for (int i = fromVal; i < toValInclusive; i++) {		// Less than to val, to avoid overflowing the array with the i+1 array index
                itemList.set(i, itemList.get(i + 1));
                statusList.set(i, statusList.get(i + 1));
            }
        } else {
            // This is the not so typical, place back into unchecked situation
            for (int i = fromVal; i > toValInclusive; i--) {		// Much like the increment for loop, avoid overflow but not getting to toVal
                itemList.set(i, itemList.get(i - 1));
                statusList.set(i, statusList.get(i - 1));
            }
        }
        itemList.set(toValInclusive, tempName);     // Set the from val value to the specified place in array
        statusList.set(toValInclusive, tempStatus);
    }
    int sumStatusArr() {
        // Return the sum of the status array, if greater than zero then this will affect has checked
        int temp = 0;
        for (int i = 0; i < statusList.size(); i++) {
            temp += statusList.get(i);
        }
        return temp;
    }
    void init() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String defaultValue = "Add,Items";
        String defaultStatus = "0,0";
        listString = new StringBuilder(sharedPref.getString(getString(R.string.stored_list_items), defaultValue));     // saved string
        String listStatus = sharedPref.getString(getString(R.string.stored_list_status), defaultStatus);       // saved status
        acceptWatchData = (sharedPref.getInt(getString(R.string.stored_commStatus), 0) == 1);       // saved comm status
        String[] tempArr = listString.toString().split(",");
        String[] tempStatus = listStatus.toString().split(",");
        itemList = new ArrayList<String>();
        statusList = new ArrayList<Integer>();
        if (listString.toString().equals("")) {
            // The list is empty, set a basic single line asking to add item
            itemList.add("Add item to begin");
            statusList.add(0);
        } else {
            for (int i = 0; i < tempArr.length; i++) {
                //Log.i("Running log", "In init, attempting to add " + tempArr[i] + " status " + tempStatus[i] + " to array lists");
                itemList.add(tempArr[i]);
                statusList.add(Integer.parseInt(tempStatus[i]));
            }
            int defaultLastCheckedValue = itemList.size() - 1;
            lastCheckedItem = sharedPref.getInt(getString(R.string.stored_lastCheckedItem), defaultLastCheckedValue);       // saved last checked value
        }

        if (sumStatusArr() == 0) {
            hasCheckedItems = false;
            lastCheckedItem = statusList.size() - 1;
        } else {
            hasCheckedItems = true;
        }

        EditText editText = (EditText) findViewById(R.id.add_list_item);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    Log.i("Running log", "Enter pressed");
                    addItem();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }
    void updatePebbleConnectionInfo(final String toThis) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.pebbleConnectionInfoTextView);
                textView.setText(toThis);
            }
        });
    }

    void sendDataToPebble(int transferNumber) {
        updatePebbleConnectionInfo("Sending data to pebble");
        Log.i("Running log", "Entered send to peb, transferNumber = " + transferNumber);
        // Time to recreate the dictionary, upon resuming this app
        listForPebble = new PebbleDictionary();
        if (transferNumber == numberOfCommTransfersReq) {
            Log.i("Running log", listString.substring(transferNumber * CHAR_TO_TRANSFER_PER_COMM, listString.length()));
            listForPebble.addString(2, listString.substring(transferNumber * CHAR_TO_TRANSFER_PER_COMM, listString.length()));
        } else if (transferNumber < numberOfCommTransfersReq){
            Log.i("Running log", listString.substring(transferNumber * CHAR_TO_TRANSFER_PER_COMM, (transferNumber + 1) * CHAR_TO_TRANSFER_PER_COMM));
            listForPebble.addString(2, listString.substring(transferNumber * CHAR_TO_TRANSFER_PER_COMM, (transferNumber + 1) * CHAR_TO_TRANSFER_PER_COMM));
        }
        byte[] temp = new byte[statusList.size()];      // Create the status list into a byte array
        for (int i = 0; i < statusList.size(); i++) {
            temp[i] = (byte)(statusList.get(i) & 0xFF);
        }

        if (transferNumber == 0) {
            listForPebble.addUint8(1, (byte)0);       // First one
        } else if (transferNumber <= numberOfCommTransfersReq) {
            listForPebble.addUint8(1, (byte)1);       // More to come
        } else {
            listForPebble.addUint8(1, (byte)2);       // Last one, add in the other data for the last
            listForPebble.addBytes(3, temp);
            listForPebble.addUint8(4, (byte) lastCheckedItem);
        }

        /*if (transferNumber == 0 && numberOfCommTransfersReq == 0) {
            // Special case where there is only one transfer, gotta transfer everything and send special 3 phase over
            listForPebble.addUint8(1, (byte)3);       // Last one, add in the other data for the last
            listForPebble.addBytes(3, temp);
            listForPebble.addUint8(4, (byte) lastCheckedItem);
        }*/

        // Send this test to pebble
        // Check the watch is connected
        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i("Running log", "Pebble is " + (connected ? "connected" : "not connected"));
        changeHasOccurred = false;       // The watch has been updated
        commTransferNumber = transferNumber;        // This transfer number has just been completed
        // Send the data
        PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, listForPebble);
    }
    void initialSendToPebble() {
        nacksReceived = 0;      // Reset the nack count
        sendDataToPebble(0);
    }       // Used for the first send of data to pebble, not counting retries and double multi transfers
    void markForSendToPebble() {
        // The database has been modified, time to update the key-value dictionary for pebble usage.
        // When database has changed, the data is right to be sent to pebble, after countdown that is
        updatePebbleConnectionInfo("Change has occurred, sending data in " + (UPDATE_COMM_DELAY_IN_SEC) + "s. Tap to force update now.");
        updateAncillaryListsWithListChg();
        acceptWatchData = false;
        changeHasOccurred = true;
        secondsSinceChange = 0;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_list);

        init();       // shouldnt have to call this
        // use the SimpleCursorAdapter to show the elements in a ListView
        //ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.single_line_layout, R.id.item_name, itemList);
        //ListView checklistView = (ListView)findViewById(R.id.ChecklistWithButtons);
        MyListAdapter adapter = new MyListAdapter(this, R.layout.single_line_layout, itemList, statusList);
        //checklistView.setAdapter(adapter);
        setListAdapter(adapter);

        // Setting up an ad to pay for my yum yums
       /* AdView myAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("YOUR_DEVICE_HASH")
                .build();
        myAdView.loadAd(adRequest);*/

        mHandler = new Handler();
        // Construct the LicenseCheckerCallback. The library calls this when done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a Policy.
        mChecker = new LicenseChecker(this, new ServerManagedPolicy(this, new AESObfuscator(SALT, getPackageName(), "2-GETDS_3")), BASE64_PUBLIC_KEY);

        doCheck();      // Check the license

        new Timer().scheduleAtFixedRate(incrementSecondsSinceChange, 3000, 1000);      // Give myself 3 seconds to update

        //Log.i("Running test", "Reached here");
    }
    private void displayResult(final String result) {
        mHandler.post(new Runnable() {
            public void run() {
                //mStatusText.setText(result);
                setProgressBarIndeterminateVisibility(false);
                //mCheckLicenseButton.setEnabled(true);
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_check_list, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateAncillaryListsWithListChg() {
        // Recompiles the list string, and the display adapter list, should be called when itemlist changes
        //Log.i("Running log", "Updating lists with change in item list");
        MyListAdapter adapter = (MyListAdapter) getListAdapter();
        adapter.clear();    // Clear out the display adapter
        adapter.addAll(itemList, statusList);       // Adds all the items in the item list. In order of item list
        adapter.notifyDataSetChanged();

        listString.setLength(0);        // Clears the string builder
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).length() > MAX_NUMBER_OF_CHAR_FOR_ITEM) {
                // String is greater than the max length for the watch, add an ellipsis for clarity
                listString.append(itemList.get(i).substring(0, MAX_NUMBER_OF_CHAR_FOR_ITEM - 2));
                listString.append("..");
            } else {
                listString.append(itemList.get(i));
            }
            if ((i + 1) < itemList.size()) {
                listString.append(",");
            }
        }
        // List string has been built, time to get how many of the things I am going to have to send over the comm
        numberOfCommTransfersReq = (listString.length() / CHAR_TO_TRANSFER_PER_COMM);
        Log.i("Running log", "Number of comm transfers (start 0): " + numberOfCommTransfersReq);


        // Saved the changed values to the persistent storage
        StringBuilder listStatus = new StringBuilder("");
        for (int i = 0; i < statusList.size(); i++) {
            listStatus.append(statusList.get(i));
            if ((i + 1) < statusList.size()) {
                listStatus.append(",");
            }
        }
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.stored_list_items), listString.toString());
        editor.putString(getString(R.string.stored_list_status), listStatus.toString());
        editor.putInt(getString(R.string.stored_lastCheckedItem), lastCheckedItem);
        editor.apply();
    }

    public void onClickOfAddItemButton(View view) {
        addItem();  // Goto add item
    }
    public void addItem() {
        EditText editText = (EditText) findViewById(R.id.add_list_item);
        String itemName = editText.getText().toString();    // Collected item name add
        editText.getText().clear();       // Reset after item add
        Log.d("Running log", "Item name is " + itemName);

        // Need to display a warning if an item with more than 15 characters
        if (itemName.length() > MAX_NUMBER_OF_CHAR_FOR_ITEM) {
            Toast tempToast = Toast.makeText(getApplicationContext(), "The item just entered is greater than " + MAX_NUMBER_OF_CHAR_FOR_ITEM + " characters. The item will be truncated on your watch", Toast.LENGTH_LONG);
            TextView v = (TextView) tempToast.getView().findViewById(android.R.id.message);
            if( v != null) v.setGravity(Gravity.CENTER);
            tempToast.show();
        }

        // Need to display a warning if adding more than 50 items
        if (itemList.size() >= 50) {
            Toast tempToast = Toast.makeText(getApplicationContext(), "You have more than 50 items. There may be problems when transferring to watch", Toast.LENGTH_LONG);
            TextView v = (TextView) tempToast.getView().findViewById(android.R.id.message);
            if( v != null) v.setGravity(Gravity.CENTER);
            tempToast.show();
        }

        // Add item will add to top of pile, have to move everything down first
        if (listString.toString().equals("")) {
            // We have to handle the lack of data differently...
            itemList.add(itemName);
            statusList.add(0);
            lastCheckedItem = 0;        // Reset everything to beginnings
        } else {
            itemList.add(itemList.get(itemList.size() - 1));        // Add an item, the last item move down
            statusList.add(statusList.get(statusList.size() - 1));
            for (int i = (itemList.size() - 1); i > 0; i--) {
                itemList.set(i, itemList.get(i - 1));
                statusList.set(i, statusList.get(i - 1));
            }
            itemList.set(0, itemName);      // Add the item to the top of the pile
            statusList.set(0, 0);           // Set checked status to false

            lastCheckedItem++;      // Increment the last checked item to align with the pile shift
        }



        Log.d("Running log", "Current list string: " + listString.toString());
        markForSendToPebble();
    }   // Called when the user clicks the add item button in a check list
    public void clearCheckedItems(View view) {
        // Step through the statuses and remove any that have been checked
        for (int i = 0; i < statusList.size(); i++) {
            if (statusList.get(i) == 1) {
                // Remove
                statusList.remove(i);
                itemList.remove(i);
                i--;        // Decrement i as it will be increment on the end of the for loop
            }
        }

        markForSendToPebble();

        Log.d("Running log", "Current list string: " + listString.toString());
    }
    public void clearAllItems(View view) {
        // Step through the statuses and remove any that have been checked
        statusList.clear();
        itemList.clear();

        markForSendToPebble();
    }
    public void uncheckAllItems(View view) {
        // Step through the statuses and change any that have been checked to unchecked
        for (int i = 0; i < statusList.size(); i++) {
            if (statusList.get(i) == 1) {
                // Change to unchecked
                statusList.set(i, 0);
            }
        }

        markForSendToPebble();
    }
    public void removeItemHandler(View view) {
        Log.i("Running test", "Reached removeItemHandler");
        int listNumber = (int)view.getTag();
        Log.i("Running test", "The removed tag number is " + listNumber);

        itemList.remove(listNumber);
        statusList.remove(listNumber);
        Log.i("Running test", "Retrieved the item number correctly");
        markForSendToPebble();
    }
    public void onCheckboxClickedHandler(View view) {
        //Log.i("Running log", "Reached onCheckboxClickedHandler");
        int listNumber = (int) view.getTag();
        //Log.i("Running log", "The checked tag number is " + listNumber);

        // Change the status of checkbox
        if (((CheckBox) view).isChecked()) {
            // Checkbox is true (state has changed to true)
            statusList.set(listNumber, 1);      // Set the status to true
            if (hasCheckedItems) {
                redistributeChecklistItems(lastCheckedItem, (itemList.size() - 1));        // Alter the display order array. When checked, pass the dirty last checked item and the total array length
            } else {
                hasCheckedItems = true;
            }
            if (lastCheckedItem < listNumber) {
                lastCheckedItem = listNumber - 1;		// As the selected number has moved up, need to decrement the dirty item
            } else {
                lastCheckedItem = listNumber;		// Set this value as the last checked item, all movements happened below this
            }
        } else {
            // Checkbox is false (state has changed to false)
            statusList.set(listNumber, 0);      // Set the status to false
            if (hasCheckedItems) {
                if (listNumber <= lastCheckedItem) {
                    // Passing these values through the usual routine will make the unchecked layer at the bottom of the pile.
                    // To combat this pass the top of list value through, shooting the item to the top of the list
                    redistributeChecklistItems(listNumber, 0);
                    lastCheckedItem = itemList.size() - 1;        // Set the last checked item to the last value in the pile if the user is unchecked the last item checked
                } else {
                    redistributeChecklistItems(listNumber, lastCheckedItem);        // Alter the display order array. When checked, pass the selected display num and last dirty check
                    lastCheckedItem = lastCheckedItem + 1;        // As the redistribution puts the unchecked value above the dirty item, move the last checked down
                }
                // Need to add code that will handle if that was the only checked item (set hasCheckedItems to false)
            }
        }

        markForSendToPebble();
    }
    public void onConnectionInfoClick(View view) {
        initialSendToPebble();
    }

    // Send a broadcast to launch the specified application on the connected Pebble
    public void startWatchApp() {
        Log.i("Running log", "Starting watch app");
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }
    // Send a broadcast to close the specified application on the connected Pebble
    public void stopWatchApp() {
        PebbleKit.closeAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }

    TimerTask incrementSecondsSinceChange = new TimerTask() {
        @Override
        public void run() {
            secondsSinceChange++;
            if (changeHasOccurred) {
                updatePebbleConnectionInfo("Change has occurred, sending data in " + (UPDATE_COMM_DELAY_IN_SEC - secondsSinceChange) + "s. Tap to force update now.");
            } else {
                if (PebbleKit.isWatchConnected(getApplicationContext())) {
                    /*if (acceptWatchData) {
                        updatePebbleConnectionInfo("Will accept watch change.\nTap to send data to watch.");
                    } else {
                        updatePebbleConnectionInfo("Won't accept changes from watch.\nTap to override data on watch");
                    }*/
                } else {
                    if (acceptWatchData) {
                        updatePebbleConnectionInfo("Pebble disconnected. Will accept watch data on watch app start");
                    } else {
                        updatePebbleConnectionInfo("Pebble disconnected. Will override data on watch when next started");
                    }
                }
            }
            if (secondsSinceChange >= UPDATE_COMM_DELAY_IN_SEC && changeHasOccurred) {
                // No change has occurred since last change, send the data to pebble
                Log.i ("Running log", "Change hasn't occurred for a while, send data to pebble");
                initialSendToPebble();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();       // Get it up and running

        updatePebbleConnectionInfo("Initialising");

        // Display the new version window first up if need be
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        int defaultBuildNumber = 0;
        if (sharedPref.getInt(getString(R.string.stored_version), defaultBuildNumber) != BUILD_NUMBER) {
            // The stored version number does not match the current build number, display the update screen
            Log.i("Running log", "reached a new version update.");
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.stored_version), BUILD_NUMBER);
            editor.commit();
            Intent newVer_intent = new Intent(this, new_version.class);
            startActivity(newVer_intent);
        }

        // On resume, make sure pebble is running the app. Hopefully this means that when the app is started, the pebble app will open and force an update to the phone
        final Handler handler = new Handler();
        init();

        // To receive data back from the watch app, Android applications must register a "DataReceiver" to operate on the dictionaries received from the watch.
        pebbleListHandler = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                Log.i("Running log", "About to get data from watch");

                if (acceptWatchData) {
                    // I am going to accept the watch changes
                    updatePebbleConnectionInfo("Accepted data from watch.");

                    // Time to parse the passed string and byte data and update the running lists
                    String[] tempArr = data.getString(2).split("\n");
                    byte[] tempStatusArr = data.getBytes(3);
                    itemList.clear();
                    statusList.clear();
                    for (int i = 0; i < tempArr.length; i++) {
                        itemList.add(tempArr[i]);
                        statusList.add(((int) tempStatusArr[i]));
                        //Log.d("Running log", "Parsed watch data into: " + itemList.get(i) + " Status " + statusList.get(i));
                    }

                    // Set up the lastchecked item
                    lastCheckedItem = data.getUnsignedIntegerAsLong(4).intValue();

                    PebbleKit.sendAckToPebble(context, transactionId);
                } else {
                    // Disregard the data from watch
                    updatePebbleConnectionInfo("Overwriting data on watch");
                    initialSendToPebble();     // Send my own list updates to the watch. HAHAHA
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateAncillaryListsWithListChg();
                    }
                });
            }
        };
        PebbleKit.registerReceivedDataHandler(this, pebbleListHandler);

        // I also need to register the acknowledgements handlers
        pebbleAckHandler = new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                //Log.i("Running log", "Received ack for transaction " + transactionId);
                nacksReceived = 0;      // Reset the number of nacks, as received a positive acknowledgement
                if (commTransferNumber <= numberOfCommTransfersReq) {
                    // Have to undertake another transfer
                    commTransferNumber++;
                    Log.i("Running log", "Received ACK. Have to run another send, trans number " + commTransferNumber);
                    sendDataToPebble(commTransferNumber);
                } else {
                    Log.i("Running log", "Received ACK. Time to accept the data from the watch from now on");
                    updatePebbleConnectionInfo("Data successfully sent to watch.\nAccepting watch changes.");
                    acceptWatchData = true;
                }
            }
        };
        PebbleKit.registerReceivedAckHandler(this, pebbleAckHandler);
        pebbleNackHandler = new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                // This will repeat until watch accepts!
                // Have to undertake another transfer
                Log.i("Running log", "Received NACK. Have to run another send, trans number " + commTransferNumber);
                if (nacksReceived++ <= DATA_RETRY_COUNTS) {
                    updatePebbleConnectionInfo("Data send failed, trying again");
                    acceptWatchData = false;        // This should already be false but just in case
                    startWatchApp();        // Make sure watch app is running
                    sendDataToPebble(commTransferNumber);
                } else {
                    Log.i("Running log", "Received NACK. Still won't accept watch data until watch acks");
                    updatePebbleConnectionInfo("Data send failed.\nWill not accept changes from watch");
                    acceptWatchData = false;
                    changeHasOccurred = false;
                }
            }
        };
        PebbleKit.registerReceivedNackHandler(this, pebbleNackHandler);

        // Setup a timer that will call sendDataToPebble if a change has occurred
        changeHasOccurred = false;       // First set the default value to false, so no unnecessary comm occurs
        secondsSinceChange = 0;



        TimerTask delayOpenApp = new TimerTask() {
            @Override
            public void run() {
                updatePebbleConnectionInfo("Ensure both devices are synced. Tap to override watch data");
                startWatchApp();     // Open watch app after the android app has sorted itself out. This stops multiple calls to start watch
                if (acceptWatchData == false) {
                    initialSendToPebble();
                }
            }
        };
        new Timer().schedule(delayOpenApp, 500);    // Give it a little less than a second
    }       // Loads up the the checklist from storage, then checks what the watch has done, if possible
    @Override
    protected void onPause() {
        // Create a toast alerting the user that a final bluetooth push is occurring
        if (changeHasOccurred) {
            acceptWatchData = false;
            Toast tempToast = Toast.makeText(getApplicationContext(), "Pushing final changes to watch\nPLEASE REOPEN APP", Toast.LENGTH_SHORT);
            TextView v = (TextView) tempToast.getView().findViewById(android.R.id.message);
            if( v != null) v.setGravity(Gravity.CENTER);
            tempToast.show();
        }

        // Deregister the broadcast receivers for the watch first so it won't trigger calls
        if (pebbleListHandler != null) {
            unregisterReceiver(pebbleListHandler);
            unregisterReceiver(pebbleNackHandler);
            unregisterReceiver(pebbleAckHandler);
            pebbleListHandler = null;
            pebbleNackHandler = null;
            pebbleAckHandler = null;
        }

        // Save the list data for now
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (acceptWatchData) {
            editor.putInt(getString(R.string.stored_commStatus), 1);        // save comm status as true
        } else {
            editor.putInt(getString(R.string.stored_commStatus), 0);        // save comm status as false
        }
        editor.apply();



        Log.i("Running log", "Pausing app. Will I accept data changes? " + acceptWatchData);

        super.onPause();
    }       // Close down the data source on app pause
    @Override
    protected void onDestroy() {
        // Deregister the timer services (seems to be stacking)
        incrementSecondsSinceChange.cancel();
        mChecker.onDestroy();
        super.onDestroy();
    }
}