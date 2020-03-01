package com.example.skinupdateutils.skin;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import java.io.File;
import java.lang.reflect.Method;

import androidx.core.content.ContextCompat;

public class SkinEngine {
    private Context mContext;
    private String mOutPkgName; //TODO: 外部资源包的packageName
    private Resources mOutResource; //TODO: 外部资源
    private static SkinEngine instance = null;

    private SkinEngine() {
    }

    public static SkinEngine getInstance() {
        if (instance == null) {
            synchronized (SkinEngine.class) {
                if (instance == null) {
                    instance = new SkinEngine();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
    }

    public void load() {
        String path = Environment.getExternalStorageDirectory() + File.separator + "test.skin";
        load(path);
    }
    /**
     * 加载外部资源包
     * @param path
     */
    public void load(final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(path);
                if (!file.exists()) {
                    return;
                }

                PackageManager pm = mContext.getPackageManager();
                PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
                mOutPkgName = info.packageName;
                AssetManager assetManager = null;
                try {
                    //TODO: 关键点3 通过反射获取AssetManager 用来加载外部的资源包
                    assetManager = AssetManager.class.newInstance();
                    Method addAssetPath = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
                    addAssetPath.invoke(assetManager, path);

                    mOutResource = new Resources(assetManager, mContext.getResources().getDisplayMetrics(), mContext.getResources().getConfiguration());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }



    public Drawable getDrawable(int resId) {
        if (mOutResource == null) {
            return ContextCompat.getDrawable(mContext, resId);
        }

        String resName = mOutResource.getResourceEntryName(resId);
        int outResId = mOutResource.getIdentifier(resName, "drawable", mOutPkgName);
        if (outResId == 0) {
            return ContextCompat.getDrawable(mContext, outResId);
        }

        return mOutResource.getDrawable(outResId);
    }

    public int getColor(int resId) {
        if (mOutResource == null) {
            return ContextCompat.getColor(mContext, resId);
        }

        String resName = mOutResource.getResourceEntryName(resId);
        int outResId = mOutResource.getIdentifier(resName, "color", mOutPkgName);
        if (outResId == 0) {
            return resId;
        }

        return mOutResource.getColor(outResId);
    }
}
