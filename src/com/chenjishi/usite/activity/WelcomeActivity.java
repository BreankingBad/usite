package com.chenjishi.usite.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.chenjishi.usite.R;
import com.chenjishi.usite.base.App;
import com.chenjishi.usite.base.BaseActivity;
import com.chenjishi.usite.entity.FeedItem;
import com.chenjishi.usite.parser.FeedItemParser;
import com.chenjishi.usite.util.ApiUtils;
import com.chenjishi.usite.util.CommonUtil;
import com.chenjishi.usite.util.FileUtils;
import com.chenjishi.usite.util.UsiteConfig;

import java.util.ArrayList;

public class WelcomeActivity extends BaseActivity {
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        context = this;

        new LoadFirstPageTask().execute(ApiUtils.BASE_URL + "/list/1.html");
    }

    class LoadFirstPageTask extends AsyncTask<String, Void, Boolean> {
        String filePath;

        @Override
        protected Boolean doInBackground(String... strings) {
            if (!CommonUtil.didNetworkConnected(context)) {
                return false;
            }

            filePath = App.instance().getCacheDirectory() + ApiUtils.CACHED_FILE_NAME;

            ArrayList<FeedItem> feedItems = (ArrayList<FeedItem>) FileUtils.unserializeObject(filePath);

            if (null != feedItems && feedItems.size() > 0) {
                return true;
            } else {

                feedItems = FeedItemParser.getMainList(strings[0]);

                if (null != feedItems) {
                    FileUtils.serializeObject(filePath, feedItems);
                    UsiteConfig.saveUpdateTime(context, System.currentTimeMillis());
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
            if (aBoolean) {
                Intent intent = new Intent(context, HomeActivity.class);
                intent.putExtra("file_path", filePath);
                startActivity(intent);
                finish();
            } else {
                CommonUtil.showToast(context, getString(R.string.net_error));
            }
        }
    }
}
