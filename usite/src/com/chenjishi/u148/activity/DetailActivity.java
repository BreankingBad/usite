package com.chenjishi.u148.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.*;
import com.chenjishi.u148.R;
import com.chenjishi.u148.entity.Comment;
import com.chenjishi.u148.service.MusicPlayListener;
import com.chenjishi.u148.service.MusicService;
import com.chenjishi.u148.util.ApiUtils;
import com.chenjishi.u148.util.StringUtil;
import com.chenjishi.u148.util.UIUtil;
import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: chenjishi
 * Date: 12-11-4
 * Time: 下午7:56
 * To change this template use File | Settings | File Templates.
 */
public class DetailActivity extends BaseActivity implements MusicPlayListener {
    private WebView mWebView;
    private JavascriptBridge mJsBridge;
    private String mUrl;

    private MusicService mMusicService;

    private String mTitle;
    private String mContent;
    private ArrayList<String> mImageUrls = new ArrayList<String>();
    private ArrayList<Comment> commentList = new ArrayList<Comment>();

    private RelativeLayout mMusicPanel;
    private TextView mSongText;
    private TextView mArtistText;
    private ProgressBar mMusicProgress;
    private ImageButton mPlayBtn;

    private boolean mBounded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdView adView = new AdView(this, AdSize.FIT_SCREEN);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.ad_layout);
        adLayout.addView(adView);
        setMenuIcon2Visibility(true);

        Bundle bundle = getIntent().getExtras();
        mUrl = ApiUtils.BASE_URL + bundle.getString("link");
        mTitle = bundle.getString("title");

        mSongText = (TextView) findViewById(R.id.tv_song_title);
        mArtistText = (TextView) findViewById(R.id.tv_artist);
        mMusicProgress = (ProgressBar) findViewById(R.id.pb_music_loading);
        mPlayBtn = (ImageButton) findViewById(R.id.btn_play);
        mMusicPanel = (RelativeLayout) findViewById(R.id.panel_music);

        mWebView = (WebView) findViewById(R.id.webview_content);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        mJsBridge = new JavascriptBridge(this);

        mWebView.addJavascriptInterface(mJsBridge, "U148");

        //for debug javascript only
//        mWebView.setWebChromeClient(new MyWebChromeClient());

        loadData();
    }


    private void initMusicPanel() {
        mSongText.setText("正在加载...");
        mArtistText.setVisibility(View.GONE);
        mMusicProgress.setVisibility(View.VISIBLE);
        mPlayBtn.setVisibility(View.GONE);
        mMusicPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMusicStartParse() {
        initMusicPanel();
    }

    @Override
    public void onMusicPrepared(String song, String artist) {
        mSongText.setText(song);
        mArtistText.setText(artist);
        mArtistText.setVisibility(View.VISIBLE);

        mMusicProgress.setVisibility(View.GONE);
        mPlayBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMusicCompleted() {
        mMusicPanel.setVisibility(View.GONE);
    }

    @Override
    public void onMusicParseError() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBounded) {
            bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMusicService = ((MusicService.MusicBinder) service).getService();
            mMusicService.registerListener(DetailActivity.this);
            mBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMusicService.unRegisterListener();
            mMusicService = null;
            mBounded = false;
        }
    };

    public void onButtonClicked(View view) {

        switch (view.getId()) {
            case R.id.icon_menu2:
                Intent intent = new Intent(this, CommentActivity.class);
                intent.putParcelableArrayListExtra("comments", commentList);
                startActivity(intent);
                break;
            case R.id.btn_play:
                if (mMusicService != null) {
                    mPlayBtn.setImageResource(mMusicService.isPlaying()
                            ? R.drawable.ic_play : R.drawable.ic_pause);
                    mMusicService.togglePlayer();
                }

                break;

        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.detail;
    }

    @Override
    protected void backIconClicked() {
        finish();
    }

    private void loadData() {
        if (StringUtil.isEmpty(mUrl)) return;

        Runnable action = new Runnable() {
            @Override
            public void run() {
                try {
                    Document doc = Jsoup.connect(mUrl).get();
                    Elements u148main = doc.getElementsByClass("u148main");
                    if (u148main.size() > 0) {
                        Elements content = u148main.get(0).getElementsByClass("u148content");
                        if (content.size() > 0) {
                            Elements mainContent = content.get(0).getElementsByClass("content");
                            if (mainContent.size() > 0) {
                                Element article = mainContent.get(0);

                                Elements images = article.select("img");
                                for (Element image : images) {
                                    mImageUrls.add(image.attr("src"));
                                }

                                Elements videos = article.select("embed");
                                for (Element video : videos) {
                                    String videoUrl = video.attr("src");
                                    video.parent().html("<img src=\"file:///android_asset/video.png\" title=\"" + videoUrl + "\" />");
                                }

                                mContent = article.html();
                                mJsBridge.setImageUrls(mImageUrls);
                            }

                            Element floors = content.get(0).getElementById("floors");
                            Elements items = floors.select("ul");
                            for (Element e : items) {
                                Comment comment = new Comment();
                                Elements el = e.select("li");
                                if (el.size() > 0) {
                                    Element _el = el.get(0);

                                    Element imgEl = _el.getElementsByClass("uhead").get(0);
                                    comment.avatar = imgEl.attr("src");

                                    Element userEl = _el.getElementsByClass("reply").get(0);
                                    Element userInfo = userEl.getElementsByClass("uinfo").get(0);
                                    comment.userName = userInfo.select("a").get(0).text();
                                    comment.time = userInfo.select("span").get(0).text();

                                    comment.content = userInfo.nextElementSibling().text();
                                }

                                commentList.add(comment);
                            }
                        }
                    }
                } catch (IOException e) {
                }
            }
        };

        Runnable postAction = new Runnable() {
            @Override
            public void run() {
                renderPage();
            }
        };

        UIUtil.runWithoutMessage(action, postAction);
    }

    private void renderPage() {
        String template = readFromAssets(DetailActivity.this, "usite.html");

        if (null != mTitle) {
            template = template.replace("{TITLE}", mTitle);
        }

        if (null != mContent) {
            template = template.replace("{CONTENT}", mContent);
        }

        mWebView.loadDataWithBaseURL(null, template, "text/html", "UTF-8", null);
        mWebView.setVisibility(View.VISIBLE);
    }

    private String readFromAssets(Context context, String fileName) {
        InputStream is;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            is = context.getAssets().open(fileName);
            byte buf[] = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            baos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toString();
    }

    class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Toast.makeText(DetailActivity.this, message, Toast.LENGTH_SHORT).show();
            result.cancel();
            return true;
        }
    }

    private class JavascriptBridge {
        private Context mContext;
        private ArrayList<String> mImageUrls = new ArrayList<String>();

        public JavascriptBridge(Context context) {
            mContext = context;
        }

        public void setImageUrls(ArrayList<String> imageUrls) {
            mImageUrls = imageUrls;
        }

        public void onImageClick(String src) {
            Intent intent = new Intent(mContext, PhotoViewActivity.class);
            intent.putExtra("imgsrc", src);
            intent.putStringArrayListExtra("images", mImageUrls);
            mContext.startActivity(intent);
        }

        public void onVideoClick(String src) {
            Intent intent = new Intent();
            if (src.contains("www.xiami.com")) {
                intent.setClass(mContext, MusicService.class);
                intent.putExtra("url", src);
                startService(intent);
            } else {
                intent.setClass(mContext, VideoPlayerActivity.class);
                intent.putExtra("url", src);
                mContext.startActivity(intent);
            }
        }
    }
}
