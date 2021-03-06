package com.chenjishi.u148.easter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.chenjishi.u148.R;
import com.chenjishi.u148.util.HttpUtils;
import com.chenjishi.u148.volley.toolbox.ImageLoader;
import com.chenjishi.u148.volley.toolbox.NetworkImageView;

/**
 * Created by chenjishi on 15/1/26.
 */
public class PhotoDialog extends Dialog implements View.OnClickListener {
    private NetworkImageView mImageView;
    private TextView mTextView;

    private OnDialogDismissCallback mCallback;

    private ImageLoader mImageLoader;

    public PhotoDialog(Context context, OnDialogDismissCallback callback) {
        super(context, R.style.FullHeightDialog);
        setCanceledOnTouchOutside(false);
        setCancelable(false);

        mCallback = callback;
        mImageLoader = HttpUtils.getImageLoader();

        View view = LayoutInflater.from(context).inflate(R.layout.photo_dialog, null);
        setContentView(view);

        mImageView = (NetworkImageView) view.findViewById(R.id.photo_view);
        mImageView.setOnClickListener(this);
        mTextView = (TextView) view.findViewById(R.id.photo_text);
        view.findViewById(R.id.btn_exit).setOnClickListener(this);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int imageWidth = (int) (metrics.widthPixels * 0.8f - 2 * metrics.density * 16);
        int imageHeight = (int) (imageWidth * 600.f / 400);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mImageView.getLayoutParams();
        lp.width = imageWidth;
        lp.height = imageHeight;
        mImageView.setLayoutParams(lp);
    }

    private int mIndex;

    public void setPhotoItem(PhotoItem item, int index) {
        if (null != item) {
            mImageView.setImageUrl(item.image, mImageLoader);
            mTextView.setText(item.title);
        } else {
            mImageView.setImageResource(R.drawable.avatar);
            mTextView.setText("Written By Jishi Chen, 2014/08/12");
        }
        mIndex = index;
    }

    @Override
    public void show() {
        WindowManager windowManager = getWindow().getWindowManager();
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = (int) (metrics.widthPixels * 0.8f);
        getWindow().setAttributes(layoutParams);

        super.show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.photo_view) {
            if (mIndex == 3) {
                Context context = getContext();
                context.startActivity(new Intent(context, FireworksActivity.class));
            }
        } else {
            dismiss();
            if (null != mCallback) {
                mCallback.onDismiss();
            }
        }
    }

    public interface OnDialogDismissCallback {

        public void onDismiss();
    }
}
