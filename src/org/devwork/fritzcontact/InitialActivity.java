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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class InitialActivity extends Activity {
	
	private EditText edit;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_activity);
        
        final TextView textview = (TextView) findViewById(R.id.initial_text);
        final Button button = (Button) findViewById(R.id.initial_next);
        final Button openfile = (Button) findViewById(R.id.initial_openfile);
        edit = (EditText) findViewById(R.id.initial_edit);
        
		try
		{
    		BufferedReader bf = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.intro)));
    		StringBuilder sb = new StringBuilder();
    		while(true)
    		{
    			String s = bf.readLine();
    			if(s == null) break;
    			sb.append(s);
    			sb.append("\n");
    		}
    		bf.close();
    		textview.setText(sb.toString());
		}
		catch(IOException io)
		{
			
		}
		
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String filename = edit.getText().toString();

		        try {
		        	ArrayList<PhoneNumber> list = read(filename);
					Intent intent = new Intent(InitialActivity.this, ProcessActivity.class);
	                Bundle bundle = new Bundle();
	                bundle.putParcelableArrayList("list", list);
					intent.putExtras(bundle);
					InitialActivity.this.startActivity(intent);
				} catch (Exception e) {
		    		AlertDialog.Builder builder = new AlertDialog.Builder(InitialActivity.this);
		    		builder.setMessage(getResources().getString(R.string.xml_error, filename, e.toString() + ", " + e.getStackTrace().toString() ))
		    		       .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
		    		           public void onClick(DialogInterface dialog, int id) {
		    		                dialog.dismiss();
		    		           }
		    		       })
		    		       ;
		    		AlertDialog alert = builder.create();
		    		alert.show();
		    		return;
				}
			}
			
		});
		
		openfile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	        	try
		        {
					Intent intent = new Intent("org.openintents.action.PICK_FILE");
					intent.setData(Uri.parse("file://" + edit.getText().toString()));
					intent.putExtra("org.openintents.extra.TITLE", getResources().getString(R.string.filedialog_openxml));
					intent.putExtra("org.openintents.extra.BUTTON_TEXT", getResources().getString(R.string.filedialog_button));
					startActivityForResult(intent, 1);
		        }
	        	catch(ActivityNotFoundException e)
	        	{
	        		AlertDialog.Builder builder = new AlertDialog.Builder(InitialActivity.this);
	        		builder.setMessage(getResources().getString(R.string.filedialog_error))
	        		       .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
	        		           public void onClick(DialogInterface dialog, int id) {
	        		                dialog.dismiss();
	        		           }
	        		       })
	        		       ;
	        		AlertDialog alert = builder.create();
	        		alert.show();
	        	}
			}
		});

		
        
        
    }
    
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
    	if(requestCode == 1)
    	{
    		if(data == null || data.getData() == null) return;
    		edit.setText(data.getData().getPath());
    	}
    }
    
	private ArrayList<PhoneNumber> read(String filename) throws SAXException, IOException, ParserConfigurationException
	{
		File f = new File(filename);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
		doc.getDocumentElement().normalize();
		ArrayList<PhoneNumber> phonenumbers = new ArrayList<PhoneNumber>();
		
		NodeList contacts = doc.getElementsByTagName("contact");
		for(int i = 0; i < contacts.getLength(); ++i)
		{
			Node contactNode = contacts.item(i);
			if(contactNode.getNodeType() != Node.ELEMENT_NODE) continue;
			Element contact = (Element) contactNode;
			NodeList realNameList = contact.getElementsByTagName("realName");
			if(realNameList.getLength() == 0) continue;
			NodeList realNameListChildNodes = realNameList.item(0).getChildNodes();
			if(realNameListChildNodes.getLength() == 0) continue;
			String realName = realNameListChildNodes.item(0).getNodeValue();
			if(realName.contains("~AVM-")) continue;
			
			NodeList numbers = contact.getElementsByTagName("number");
			for(int n = 0; n < numbers.getLength(); ++n)
			{
				Node numberNode = numbers.item(n);
				if(numberNode.getNodeType() != Node.ELEMENT_NODE) continue;
				Element number = (Element) numberNode;
				String type = number.getAttribute("type");
				NodeList numberList = number.getChildNodes();
				if(numberList.getLength() == 0) continue;
				String snumber = numberList.item(0).getNodeValue();
				phonenumbers.add(new PhoneNumber(phonenumbers.size(), realName, type, snumber));
			}
		}
		return phonenumbers;
	}
    
    
}
