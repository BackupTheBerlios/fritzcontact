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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;

public class PhoneNumber implements Parcelable
{
	public static final Parcelable.Creator<PhoneNumber> CREATOR = new Parcelable.Creator<PhoneNumber>() {

		@Override
		public PhoneNumber createFromParcel(Parcel source) {
			return new PhoneNumber(source.readInt(), source.readString(), source.readInt(), source.readString());
		}

		@Override
		public PhoneNumber[] newArray(int size) {
			return new PhoneNumber[size];
		}
	};
	
	private PhoneNumber(int id, String name, int type, String number) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.number = number;
	}
	
	public PhoneNumber(int id, String name, String type, String number) {
		this.id = id;
		this.name = name;
		this.number = number;
		type = type.toLowerCase();
		Log.e("type", type);
		if(type.equals("home"))
			this.type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
		else if(type.equals("work"))
			this.type = ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
		else if(type.equals("mobile"))
			this.type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
		else 
			this.type = ContactsContract.CommonDataKinds.Phone.TYPE_MAIN;
	}
	final int id;
	final String name;
	final int type;
	final String number;
	
	public String getTypeName(Context context)
	{
		switch(type)
		{
			case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
				return context.getResources().getString(R.string.type_home);
			case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
				return context.getResources().getString(R.string.type_work);
			case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
				return context.getResources().getString(R.string.type_mobile);
			default:
				return context.getResources().getString(R.string.type_main);
				
		}
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(name);
		dest.writeInt(type);
		dest.writeString(number);
	}
	
}