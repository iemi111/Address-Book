package com.com2027.contacts;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Emi on 14/05/2017.
 */

public class AdapterList extends ArrayAdapter<Contact> {

    private List<Contact> list;



    public AdapterList(Context context, List<Contact> list)  {
        super(context, R.layout.item_list_contacts,list);
        this.list = list;
    }


    @Override
    public View getView(int position,View convertView,ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.item_list_contacts,parent,false);

        TextView txt = (TextView) rowView.findViewById(R.id.txtField);

        txt.setText(list.get(position).getName());

        return rowView;
    }
}
