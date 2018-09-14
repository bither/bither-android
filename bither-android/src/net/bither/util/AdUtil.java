package net.bither.util;

import net.bither.R;
import net.bither.bitherj.api.DownloadFile;
import net.bither.bitherj.api.GetAdApi;
import net.bither.runnable.BaseRunnable;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;

/**
 * Created by Hzz on 2016/10/27.
 */

public class AdUtil {
    private final String imgEn = "img_en";
    private final String imgZhCN = "img_zh_CN";
    private final String imgZhTW = "img_zh_TW";
    private static String timestamp = "timestamp";
    private int downloadImageNum;
    private JSONObject jsonObject;

    public void getAd() {
        BaseRunnable baseRunnable = new BaseRunnable() {

            @Override
            public void run() {
                try {
                    GetAdApi getAdApi = new GetAdApi();
                    getAdApi.handleHttpGet();
                    jsonObject = new JSONObject(getAdApi
                            .getResult());
                    if (isDownloadImage(jsonObject)) {
                        downloadImageNum = 0;
                        String imgEnPath = jsonObject.getString(imgEn);
                        String imgZhCNPath = jsonObject.getString(imgZhCN);
                        String imgZhTWPath = jsonObject.getString(imgZhTW);
                        downloadImage(imgEn, imgEnPath);
                        downloadImage(imgZhCN, imgZhCNPath);
                        downloadImage(imgZhTW, imgZhTWPath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(baseRunnable).start();
    }

    public boolean isDownloadImage(JSONObject newJsonObject) {
        JSONObject cacheAdJsonObject = getCacheAdJSON();
        if (cacheAdJsonObject != null) {
            try {
                if (cacheAdJsonObject.getString(timestamp).equalsIgnoreCase(newJsonObject.getString(timestamp))) {
                    return false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return true;
            }
        }
        return true;
    }

    public static JSONObject getCacheAdJSON() {
        try {
            File file = FileUtil.getAdFile();
            String adStr = (String) FileUtil.deserialize(file);
            JSONObject jsonObject = new JSONObject(adStr);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void downloadImage(String fileName, String filePath) {
        try {
            DownloadFile downloadFile = new DownloadFile();
            File file = ImageFileUtil.getAdImageFile(fileName+getTime()+".png");
            downloadFile.downloadFile(filePath, file);
            downloadImageNum += 1;
            if (downloadImageNum == 3) {
                try {
                    File adfile = FileUtil.getAdFile();
                    FileUtil.serializeObject(adfile, jsonObject.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTime(){
        long time = System.currentTimeMillis()/1000;
        String str = String.valueOf(time);
        return str;
    }

}
