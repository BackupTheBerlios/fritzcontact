/* FritzContact - Imports contacts from AVM-Fritz!Box-XML files 
 * Copyright (C) 2011 Dominik Köppl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.devwork.fritzcontact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;



public class ProcessActivity extends Activity {
    	
	
	
	private ProcessAdapter adapter;
	private ListView listview;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.process_activity);
    	Bundle bundle = getIntent().getExtras();
    	ArrayList<PhoneNumber> list = bundle.getParcelableArrayList("list");
        listview = (ListView) findViewById(R.id.process_listview);
		adapter = new ProcessAdapter(this, list);
		listview.setAdapter(adapter);
        listview.setClickable(true);
        listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listview.setOnItemClickListener(adapter);
        
		Button apply = (Button) findViewById(R.id.process_apply);

		apply.setOnClickListener(new OnClickListener() {
			
			private ProgressDialog progressDialog;
			private int applied = 0;
			private int all = 0;
			@Override
			public void onClick(View v) {
				progressDialog = new ProgressDialog(ProcessActivity.this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setMessage(getResources().getString(R.string.processing));
				progressDialog.setMax(adapter.getCount());
				progressDialog.show();
				new Task().execute();
			}

			class Task extends AsyncTask<Integer, Integer, Integer> {

				@Override
				protected Integer doInBackground(Integer... arg0) {
					for(int i = 0; i < adapter.getCount(); ++i)
					{
						progressDialog.setProgress(i);
						if(adapter.check_states[i] == false) continue;
						++all;
						{
							Uri lookupUri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(adapter.list.get(i).number ));
							Cursor cursor = getContentResolver().query(lookupUri, new String[]{Phone.CONTACT_ID}, null, null, null);
							boolean exists = cursor.getCount() > 0;
							cursor.close();
							if(exists) continue;
						}
						/*
						Uri lookupUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(adapter.list.get(i).name ));
						Cursor cursor = getContentResolver().query(lookupUri, new String[]{Contacts.LOOKUP_KEY}, null, null, null);
						boolean exists = cursor.getCount() > 0;
						if(exists)
						{
							cursor.moveToFirst();
						}
						cursor.close();
						*/
						
						
						ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
						ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
				                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
				                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
								.build());
				        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
				                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
				                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, adapter.list.get(i).name)
				                .build());
				        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
				                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
				                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, adapter.list.get(i).number)
				                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,adapter.list.get(i).type )
				                .build());
				        try {
				        	getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
				        	++applied;
				        } catch(Throwable e)
				        {
				    		AlertDialog.Builder builder = new AlertDialog.Builder(ProcessActivity.this);
				    		builder.setMessage(getResources().getString(R.string.process_error, e.toString()))
				    		       .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
				    		           public void onClick(DialogInterface dialog, int id) {
				    		                dialog.dismiss();
				    		           }
				    		       })
				    		       ;
				    		AlertDialog alert = builder.create();
				    		alert.show();
				    		return null;
				        }
					}
					return null;
				}
				@Override
			    protected void onPostExecute(Integer result) {
					progressDialog.dismiss();
					String str = getResources().getQuantityString(R.plurals.process_finished_inserted, applied, applied) + 
					"\n" + 
					getResources().getQuantityString(R.plurals.process_finished_omitted, all - applied, all - applied);
					Toast toast = Toast.makeText(ProcessActivity.this, str, Toast.LENGTH_LONG);
					toast.show();
			    }
			};
		});
	}
    class ProcessAdapter extends BaseAdapter implements OnItemClickListener {

    	private final ArrayList<PhoneNumber> list;
    	private final boolean[] check_states;
    	private final LayoutInflater layoutinflater; 
		ProcessAdapter(Context context, ArrayList<PhoneNumber> list)
    	{
    		this.list = list;
    		this.layoutinflater = LayoutInflater.from(context);
    		check_states = new boolean[list.size()];
    	}
    	
		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int index) {
			return list.get(index);
		}

		@Override
		public long getItemId(int position) {
			return list.get(position).id;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = layoutinflater.inflate(R.layout.process_child, null);
			}
			final CheckBox check = (CheckBox) convertView.findViewById(R.id.process_child_check);
				
				OnClickListener listener = new OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean state = !check_states[position];
						check_states[position] = state;
						check.setChecked(state);
					}
				};
				//convertView.setOnClickListener(listener);
				check.setOnClickListener(listener);
			
			TextView name = (TextView)convertView.findViewById(R.id.process_child_name);
			TextView number = (TextView)convertView.findViewById(R.id.process_child_number);
			TextView type = (TextView)convertView.findViewById(R.id.process_child_type);
			check.setFocusable(false);
			check.setChecked(check_states[position]);
			name.setText(list.get(position).name);
			number.setText(list.get(position).number);
			type.setText(list.get(position).getTypeName(ProcessActivity.this));
			return convertView;
		}
		
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final CheckBox checkbox = (CheckBox) view.findViewById(R.id.process_child_check);
			if(checkbox == null) return;
    		boolean state = ! check_states[position];
        	check_states[position] = state;
        	checkbox.setChecked(state);
		}
    	
    }
    

 

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.process, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_about:
        	try
	        {
        		Intent intent = new Intent("org.openintents.action.SHOW_ABOUT_DIALOG");
	    		//Intent intent = new Intent(AboutIntents.ACTION_SHOW_ABOUT_DIALOG);
	    		startActivityForResult(intent, 1);
	        }
        	catch(ActivityNotFoundException e)
        	{
        		try
        		{
	        		BufferedReader bf = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.license_short)));
	        		StringBuilder sb = new StringBuilder();
	        		while(true)
	        		{
	        			String s = bf.readLine();
	        			if(s == null) break;
	        			sb.append(s);
	        			sb.append("\n");
	        		}
        		
	        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        		builder.setMessage(sb.toString())
	        		       .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
	        		           public void onClick(DialogInterface dialog, int id) {
	        		                dialog.dismiss();
	        		           }
	        		       })
	        		       ;
	        		AlertDialog alert = builder.create();
	        		alert.show();
        		}
        		catch(IOException io)
        		{
        			
        		}
        	}
        	return true;
        case R.id.menu_select_all:
        	changeCheckStates(true);
        	return true;
        case R.id.menu_select_clear:
        	changeCheckStates(false);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void changeCheckStates(boolean checked)
    {
    	for(int i = 0; i < adapter.check_states.length; ++i)
    	{
    		listview.setItemChecked(i, true);
    		View view = listview.getChildAt(i);
    		if(view != null)
    		{        	
    			final CheckBox checkbox = (CheckBox) view.findViewById(R.id.process_child_check);
    			checkbox.setChecked(checked);
    		}
    		adapter.check_states[i] = checked;
    	}
    }
	
	
}
