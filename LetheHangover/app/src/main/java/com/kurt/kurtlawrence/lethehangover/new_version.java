package com.kurt.kurtlawrence.lethehangover;

import android.app.Activity;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.android.kurtlawrence.lethehangover.R;

/**
 * Created by Kurt on 26/09/2015.
 */
public class new_version extends Activity {
    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.new_version);
        TextView newVerTextView = (TextView)findViewById(R.id.newVerSpiel);
        newVerTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    public void onNewVersionButtonClick(View view) {
        Intent goBackToChecklist = new Intent(this, Check_list.class);
        startActivity(goBackToChecklist);
    }       // Display the normal screen when the button click has happened
}
