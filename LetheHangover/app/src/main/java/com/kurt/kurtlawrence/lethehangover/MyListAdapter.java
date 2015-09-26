package com.kurt.kurtlawrence.lethehangover;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.android.kurtlawrence.lethehangover.R;

import java.util.List;

public class MyListAdapter extends ArrayAdapter<String> {
    private List<String> items;
    private int layoutResourceId;
    private Context context;
    private List<Integer> statuses;

    public MyListAdapter(Context context, int layoutResourceId, List<String> items, List<Integer> statuses) {
        super(context, -1, items);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.items = items;
        this.statuses = statuses;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.single_line_layout, parent, false);
        CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.status_checkbox);
        ImageButton imageButton = (ImageButton) rowView.findViewById(R.id.item_remove_button);

        boolean currentStatus = false;
        if (statuses.get(position) == 1) {
            currentStatus = true;
        }
        checkBox.setText(items.get(position));
        checkBox.setTag(position);
        checkBox.setChecked(currentStatus);
        imageButton.setTag(position);       // Trial at setting a tag as the item number for when deletion is chosen

        return rowView;
    }

    @Override
    public void clear() {
        items.clear();
        statuses.clear();
    }

    //@Override
    public void addAll(List<String> itemListInput, List<Integer> statusListInput) {
        items.addAll(itemListInput);
        statuses.addAll(statusListInput);
    }
}
