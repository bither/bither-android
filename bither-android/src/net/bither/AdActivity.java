package net.bither;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;

public class AdActivity extends Activity {

    private ImageView ivAd;
    private Button btnGo;
    private Button btnSkip;
    private String url;
    private final int TIME = 1000;
    private int countdown = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);

        initView();
        handler.postDelayed(runnable, TIME);
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            setBtnSkipText(--countdown);
            if (countdown > 1) {
                handler.postDelayed(this, TIME);
            } else {
                adActivityFinish();
            }
        }
    };

    private void initView() {
        ivAd = (ImageView) findViewById(R.id.iv_ad);
        btnGo = (Button) findViewById(R.id.btn_go);
        btnSkip = (Button) findViewById(R.id.btn_skip);
        setIvAd();
        setBtnSkipText(countdown);
        setAdUrl();
    }

    private void setBtnSkipText(int countdown) {
        String skipText = Integer.toString(countdown) + " " + getString(R.string.ad_skip);
        btnSkip.setText(skipText);
        Spannable wordtoSpan = new SpannableString(btnSkip.getText());
        wordtoSpan.setSpan(new AbsoluteSizeSpan(40), 0, 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        wordtoSpan.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        btnSkip.setText(wordtoSpan);
    }

    private void setIvAd() {
        File imageFolder = ImageFileUtil.getAdImageFolder(getString(R.string.ad_image_name));
        File imageFiles[] = imageFolder.listFiles();
        File imageFile = null;
        for (File file: imageFiles) {
            if (file.getAbsolutePath().contains(getString(R.string.ad_image_name))) {
                if (!file.getAbsolutePath().contains(".nomedia")) {
                    imageFile = file;
                    break;
                }
            }
        }

        if (imageFile != null) {
            Bitmap bitmap = getBitmapFromFile(imageFile);
            if (bitmap != null) {
                ivAd.setImageBitmap(bitmap);
            } else {
                adActivityFinish();
            }
        } else {
            adActivityFinish();
        }

    }

    private void setAdUrl() {
        File file = FileUtil.getAdFile();
        String adStr = (String) FileUtil.deserialize(file);
        try {
            JSONObject jsonObject = new JSONObject(adStr);
            url = jsonObject.getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void btnGoOnClick(View v) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void btnSkipOnClick(View v) {
        adActivityFinish();
    }

    private void adActivityFinish() {
        setResult(20);
        AdActivity.this.finish();
    }

    private Bitmap getBitmapFromFile(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
