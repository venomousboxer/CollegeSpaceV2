package com.ap.collegespacev2;

import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ap.collegespacev2.Adapter.PostsListAdapter;
import com.ap.collegespacev2.Helper.ConnectionDetector;
import com.ap.collegespacev2.Helper.DBHelper;
import com.ap.collegespacev2.Helper.UpdatesItem;
import com.ap.collegespacev2.Helper.jsonParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Home extends BaseActivity
{
    DBHelper mDBHelper;
    ConnectionDetector cd;
    SwipeRefreshLayout swipeLayout;
    PostsListAdapter mUpdatesAdapter;
    ListView mUpdatesListView;
    AssetManager mAssetManager;
    Resources mResources;

    boolean InvalidateUpdatesList;
    Integer CurrentPage = 0;
    static final String plugin_url = "http://updates.collegespace.in/wp-json/posts/";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home, 0);

        getSupportActionBar().setTitle(R.string.updatesTitle);
        mDBHelper = new DBHelper(this);
        mUpdatesListView = (ListView)findViewById(R.id.posts_list_updates);
        mAssetManager = getAssets();
        mResources = getResources();

        /* SwipeRefreshLayout Settings */
        swipeLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeLayout.setColorSchemeResources(
                R.color.swipe_c1,
                R.color.swipe_c2,
                R.color.swipe_c3,
                R.color.swipe_c4);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                if(isCon())
                {
                    InvalidateUpdatesList = true;
                    new DownloadPostsTask().execute(plugin_url);
                }
            }
        });

        mUpdatesListView.setOnScrollListener(new AbsListView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) { }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                if(firstVisibleItem == 0)
                    swipeLayout.setEnabled(true);
                else
                    swipeLayout.setEnabled(false);
            }
        });

        UpdateListPosts();
        if (isCon())
        {
            swipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(true);
                    new DownloadPostsTask().execute(plugin_url);
                }
            });
        }
    }

    private Boolean isCon()
    {
        cd = new ConnectionDetector(this);
        return cd.isConnectingToInternet();
    }

    private void UpdateListPosts()
    {
        mUpdatesAdapter = new PostsListAdapter(this, mDBHelper.UpdatesGetAllItemsID());
        mUpdatesListView.setVisibility(View.VISIBLE);
        mUpdatesListView.setAdapter(mUpdatesAdapter);

        mUpdatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id)
            {
                UpdatesItem post = mUpdatesAdapter.getItem(position);
                Intent intent = new Intent(Home.this, PostDetails.class);
                intent.putExtra("post_title_bar", "Updates");
                intent.putExtra("post_id", Integer.toString(post.getID()));
                intent.putExtra("post_title", post.getTitle());
                intent.putExtra("post_content", post.getContent());
                intent.putExtra("post_url", post.getLink());
                intent.putExtra("post_date", post.getDate());
                startActivity(intent);
            }
        });
    }

    protected class DownloadPostsTask extends AsyncTask<String, Integer, Void>
    {
        @Override
        protected void onPreExecute()
        {
            swipeLayout.setEnabled(true);
            swipeLayout.setRefreshing(true);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if (InvalidateUpdatesList)
                UpdateListPosts();
            swipeLayout.setRefreshing(false);
        }

        @Override
        protected Void doInBackground(String... params)
        {
            String download_url = params[0];
            try
            {
                jsonParser jParser = new jsonParser();
                JSONArray json = jParser.getJSONFromUrl(download_url);
                JSONObject feedObj;
                UpdatesItem aItem;

                String content = getStringFromInputStream(mAssetManager.open("post_template.html"));
                for (int index = 0; index < json.length(); index++)
                {
                    feedObj = json.getJSONObject(index);
                    aItem = new UpdatesItem(
                            feedObj.getInt(mResources.getString(R.string.wp_plugin_id)),
                            feedObj.getString(mResources.getString(R.string.wp_plugin_title)),
                            content + feedObj.getString(mResources.getString(R.string.wp_plugin_content)),
                            feedObj.getString(mResources.getString(R.string.wp_plugin_link)),
                            feedObj.getString(mResources.getString(R.string.wp_plugin_date)),
                            feedObj.getString(mResources.getString(R.string.wp_plugin_modified))
                    );
                    mDBHelper.UpdatesAddItemIfNotExist(aItem);
                }
            }
            catch (Exception ex)
            { Log.e("@json_exception", ex.getMessage()); }
            return null;
        }
    }

    private static String getStringFromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
}
