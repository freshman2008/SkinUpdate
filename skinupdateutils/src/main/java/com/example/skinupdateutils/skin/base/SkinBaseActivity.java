package com.example.skinupdateutils.skin.base;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;

import com.example.skinupdateutils.skin.SkinEngine;
import com.example.skinupdateutils.skin.SkinFactory;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class SkinBaseActivity extends AppCompatActivity {
    private SkinFactory mSkinFactory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        /*// 通过设置一个Factory进去，来代替系统创建View
        LayoutInflater.from(this).setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                Log.i("hello", "onCreateView 01 -> name:" + name);
                if (name.equals("TextView")) {
                    Button button =  new Button(context, attrs);
                    button.setText("我是被替换的TextView");
                    return button;
                }
                return null;
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                Log.i("hello", "onCreateView 02 -> name:" + name);
                return null;
            }
        });*/

        //TODO: 关键点1： hook系统创建view的过程
        mSkinFactory = new SkinFactory();
        mSkinFactory.setDelegate(getDelegate());
        LayoutInflater.from(this).setFactory2(mSkinFactory);
        super.onCreate(savedInstanceState);
    }

    protected void changeSkin(String path) {
        if (true) {
            File skinFile = new File(Environment.getExternalStorageDirectory(), path);
            SkinEngine.getInstance().load(skinFile.getAbsolutePath());
            mSkinFactory.changeSkin();
        }
    }
}
