package net.bither.util;
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
    private static String imgEn = "img_en";
    private static String imgZhCN = "img_zh_CN";
    private static String imgZhTW = "img_zh_TW";

    public void getAd() {
        BaseRunnable baseRunnable = new BaseRunnable() {

            @Override
            public void run() {
                try {
                    GetAdApi getAdApi = new GetAdApi();
                    getAdApi.handleHttpGet();
                    JSONObject jsonObject = new JSONObject(getAdApi
                            .getResult());
                    File file = FileUtil.getAdFile();
                    FileUtil.serializeObject(file, jsonObject.toString());
                    String imgEnPath = jsonObject.getString(imgEn);
                    String imgZhCNPath = jsonObject.getString(imgZhCN);
                    String imgZhTWPath = jsonObject.getString(imgZhTW);
                    downloadImage(imgEn, imgEnPath);
                    downloadImage(imgZhCN, imgZhCNPath);
                    downloadImage(imgZhTW, imgZhTWPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(baseRunnable).start();
    }

    private void downloadImage(String fileName, String filePath) {
        try {
            DownloadFile downloadFile = new DownloadFile();
            File file = ImageFileUtil.getAdImageFile(fileName+getTime()+".png");
            downloadFile.downloadFile(filePath, file);
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
