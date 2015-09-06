package am.zoom.mlauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private List<AppDetail> apps = new ArrayList<AppDetail>();
	private List<AppDetail> bookmarks = new ArrayList<AppDetail>();
	private ListView list; 
	private ArrayAdapter<AppDetail> adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		//---------------------adapter setup--------------------//
		adapter = new ArrayAdapter<AppDetail>(this, R.layout.list_item, apps) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            if(convertView == null){
	                convertView = getListItem();
	            }
	            
	            if(apps.size() > position){
		            TextView appLabel = (TextView)convertView.findViewById(R.id.item_app_label);
		            appLabel.setText(apps.get(position).label);
		            appLabel.setCompoundDrawablesWithIntrinsicBounds(null, null, apps.get(position).icon, null);
	            }
	             
	            return convertView;
	        }
	    };
		
		//-----------------------list setup--------------------//
		list = (ListView)findViewById(R.id.apps_list);
	    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
	        	launchApp(apps.get(pos).name.toString(), apps.get(pos).activity.toString());
	        }
	    });
	    list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
	    	@Override
	    	public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
	    		AppDetail anApp = apps.get(pos);
	    		
	    		//----search in bookmarks and remove if there----//
	    		for(int i=0; i<bookmarks.size(); i++) {
	    			AppDetail iterApp = bookmarks.get(i);
	    			
					if(iterApp.name.equals(anApp.name) && iterApp.activity.equals(anApp.activity)){
						bookmarks.remove(i);
						
	    				Toast.makeText(MainActivity.this, "Removed from bookmarks", Toast.LENGTH_SHORT).show();
	    				
	    				return true;
					}
	    		}
	    		
	    		//----add to bookmarks if not there----//
	    		bookmarks.add(anApp);
	    		
	    		Toast.makeText(MainActivity.this, "Added to bookmarks", Toast.LENGTH_SHORT).show();
	    		
	    		return true;
	    	}
		});
	    list.setAdapter(adapter);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
	    //------------------------apps-------------------------------//
		PackageManager manager = getPackageManager();
	    
	    List<ResolveInfo> availableActivities = manager.queryIntentActivities(new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0);
	    for(ResolveInfo anActivity:availableActivities){
	        AppDetail app = new AppDetail();
	        app.label = anActivity.loadLabel(manager);
	        app.name = anActivity.activityInfo.packageName;
	        app.activity = anActivity.activityInfo.name;
	        app.icon = anActivity.activityInfo.loadIcon(manager);
	        apps.add(app);
	    }
	    
	    Collections.sort(apps, new Comparator<AppDetail>() {
			@Override
			public int compare(AppDetail lhs, AppDetail rhs) {
				return lhs.label.toString().compareToIgnoreCase(rhs.label.toString());
			}
		});
	    
	    adapter.notifyDataSetChanged();
	    
	    //------------------------bookmarks-------------------------------//
	    SharedPreferences settings = getSharedPreferences("bookmarks", 0);
	    String bookmarksJson = settings.getString("bookmarks", "");
	    
	    JSONArray savedBookmarks = null;
	    try {
	    	savedBookmarks = new JSONArray(bookmarksJson);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    if(savedBookmarks!=null){
		    for(int iter=0; iter<savedBookmarks.length(); iter++){
		    	AppDetail bookmark = getBookmark(iter, savedBookmarks);
		    	
		    	for(int j=0; j<apps.size(); j++){
		    		AppDetail anApp = apps.get(j);
		    		
		    		if(anApp.name.equals(bookmark.name) && anApp.activity.equals(bookmark.activity)){
		    			bookmarks.add(anApp);
		    		}
		    	}
		    }
	    }
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		//----------------save bookmarks--------------------//
		JSONArray savedBookmarks = new JSONArray();
		for(int i=0; i<bookmarks.size(); i++){
			AppDetail bookmark = bookmarks.get(i);
			
			JSONObject saveBookmark = new JSONObject();
			
			try {
				saveBookmark.put("name", bookmark.name);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			try {
				saveBookmark.put("activity", bookmark.activity);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			savedBookmarks.put(saveBookmark);
		}
		
		SharedPreferences prefs = getSharedPreferences("bookmarks", Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString("bookmarks", savedBookmarks.toString());
		editor.commit();
		
		//----------------free memory--------------------//
		apps.clear();
		bookmarks.clear();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		
		int lastKey = bookmarks.size() - 1;
		
		for(int i=lastKey; i>=0; i--) {
			menu.add(Menu.NONE, i, Menu.NONE, bookmarks.get(i).label);
		}
		
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			openOptionsMenu();
			
			return true;
		}
		else
		{
			return super.onKeyDown(keyCode, event);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int bookmarkIndex = item.getItemId();
		
		if(bookmarkIndex < bookmarks.size()){
			launchApp(bookmarks.get(bookmarkIndex).name.toString(), bookmarks.get(bookmarkIndex).activity.toString());
		}
		
		return true;
	}
	
	protected void launchApp(String pkg, String cls){
    	Intent i = new Intent(Intent.ACTION_MAIN, null);
    	i.addCategory(Intent.CATEGORY_LAUNCHER);
		i.setComponent( new ComponentName(pkg, cls) );
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		startActivity(i);
	}
	
	protected AppDetail getBookmark(int atIndex, JSONArray savedBookmarks){
		AppDetail ret = new AppDetail();
		
		JSONObject bookmark = null;
		try {
			bookmark = savedBookmarks.getJSONObject(atIndex);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if(bookmark!=null){
			String name = null;
			try {
				name = bookmark.getString("name");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			String activity = null;
			try {
				activity = bookmark.getString("activity");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			if(name!=null && activity!=null){
				ret.name = name;
				ret.activity = activity;
			}
		}
		
		return ret;
	}
	
	@SuppressLint("InflateParams")
	protected View getListItem(){
		return getLayoutInflater().inflate(R.layout.list_item, null);
	}
}
