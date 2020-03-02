package com.example.skinupdateutils.skin;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.skinupdateutils.R;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SkinFactory implements LayoutInflater.Factory2 {
    //预定义一个委托类，它负责按照系统的原有逻辑来创建view
    private AppCompatDelegate delegate;
    //缓存所有可以换肤的View对象
    private List<SkinView> cacheSkinView = new ArrayList<>();

    private static final Class<?>[] mConstructorSignature = new Class[] {Context.class, AttributeSet.class};
    private final Object[] mConstructorArgs = new Object[2];//View的构造函数的2个"实"参对象
    private static final HashMap<String, Constructor<? extends View>> sConstructorMap = new HashMap();
    private static final String[] prefixs = new String[] {//Android里面控件的包名，就这么3种,这个变量是为了下面代码里，反射创建类的class而预备的
            "android.widget.",
            "android.view.",
            "android.webkit."
    };

    /**
     * Factory2 是继承Factory的，所以，我们这次是主要重写Factory的onCreateView逻辑，就不必理会Factory的重写方法了
     *
     * @param name
     * @param context
     * @param attrs
     * @return
     */
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        Log.i("hello", "onCreateView 2 -> name:" + name);
//        return onCreateView(null, name, context, attrs);
        return null;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        //TODO: 关键点1
        Log.i("hello", "onCreateView 1 -> name:" + name);
        View view = delegate.createView(parent, name, context, attrs);
        if (view == null) {
            mConstructorArgs[0] = context;
            try {//替代系统来创建View
                if (-1 == name.indexOf('.')) {
                    //如果View的name中不包含'.'则说明是系统控件，会在接下来的调用链在name前面加上包名
                    view = createView(context, name, prefixs, attrs);
                } else {
                    //如果View的name中包含'.'(则说明是自定义View或者v4,v7等扩展包中的view)则直接调用createView方法， onCreateView后续也是调用了createView
                    view = createView(context, name, null, attrs);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        //TODO: 关键点2 收集需要换肤的View
        parseView(context, attrs, view);

        return view;
    }

    public void setDelegate(AppCompatDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * 分析View是不是支持换肤，如果支持加入到换肤View集合中保存
     *
     * @param context
     * @param attrs
     * @param view
     */
    private void parseView(Context context, AttributeSet attrs, View view) {
        Log.i("hello", "parseView");
        if (view == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Skinable);
        boolean isSupport = typedArray.getBoolean(R.styleable.Skinable_isSupport, false);
        if (isSupport) {
            final int count = attrs.getAttributeCount();
            HashMap<String, String> attrmap = new HashMap<>();
            for (int i=0; i<count; i++) {
                String attrName = attrs.getAttributeName(i);
                String attrValue = attrs.getAttributeValue(i);
                Log.i("hello", "attrName: " + attrName + "attrValue； " + attrValue);
                attrmap.put(attrName, attrValue);
            }
            SkinView skinView = new SkinView();
            skinView.view = view;
            skinView.attrmap = attrmap;
            cacheSkinView.add(skinView);
            skinView.apply();
        }
    }

    /**
     * 所谓hook，要懂源码，懂了之后再劫持系统逻辑，加入自己的逻辑。
     * 那么，既然懂了，系统的有些代码，直接拿过来用，也无可厚非。
     */
    //*******************************下面一大片，都是从源码里面抄过来的，并不是我自主设计******************************
    // 你问我抄的哪里的？到 AppCompatViewInflater类源码里面去搜索：view = createViewFromTag(context, name, attrs);
    public final View createView(Context context, String name, String[] prefixs, AttributeSet attrs) {
        Constructor<? extends View> constructor = sConstructorMap.get(name);
        Class<? extends View> clazz = null;

        if (constructor == null) {
            try {
                if (prefixs != null && prefixs.length >0) {
                    for (String prefix:prefixs) {
                        try {
                            clazz = context.getClassLoader().loadClass(
                                    prefix != null ? (prefix + name) : name).asSubclass(View.class);
                            if (clazz != null) break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (clazz == null) {
                        clazz = context.getClassLoader().loadClass(name).asSubclass(View.class);
                    }
                }

                if (clazz == null) {
                    return null;
                }

                constructor = clazz.getConstructor(mConstructorSignature);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            constructor.setAccessible(true);
            sConstructorMap.put(name, constructor);
        }

        Object[] args = mConstructorArgs;
        args[1] = attrs;

        try {
            //通过反射创建View对象
            final View view = constructor.newInstance(args);
            return view;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 换肤接口，外部调用
     */
    public void changeSkin() {
        for (SkinView skinView:cacheSkinView) {
            skinView.apply();
        }
    }

    public static class SkinView {
        public View view;
        public HashMap<String, String> attrmap;

        /**
         * TODO: 应用换肤
         */
        public void apply() {
            if (!TextUtils.isEmpty(attrmap.get("background"))) {
                int id = Integer.parseInt(attrmap.get("background").substring(1));
                String attrType = view.getResources().getResourceTypeName(id);
                if (TextUtils.equals(attrType, "drawable")) {
                    view.setBackgroundDrawable(SkinEngine.getInstance().getDrawable(id));
                } else if (TextUtils.equals(attrType, "color")) {
                    view.setBackgroundColor(SkinEngine.getInstance().getColor(id));
                }
            }

            if (view instanceof ImageView) {
                if (!TextUtils.isEmpty(attrmap.get("src"))) {
                    int id = Integer.parseInt(attrmap.get("src").substring(1));
                    String attrType = view.getResources().getResourceTypeName(id);
                    if (TextUtils.equals(attrType, "drawable")) {
                        ((ImageView)view).setImageDrawable(SkinEngine.getInstance().getDrawable(id));
                    } else if (TextUtils.equals(attrType, "color")) {
                        view.setBackgroundColor(SkinEngine.getInstance().getColor(id));
                    }
                }
            }

            if (view instanceof TextView) {
                if (!TextUtils.isEmpty(attrmap.get("textColor"))) {
                    int id = Integer.parseInt(attrmap.get("textColor").substring(1));
                    String attrType = view.getResources().getResourceTypeName(id);
                    if (TextUtils.equals(attrType, "color")) {
                        ((TextView)view).setTextColor(SkinEngine.getInstance().getColor(id));
                    }
                }
            }

            //那么如果是自定义组件呢
            if (view instanceof MyView) {
                //那么这样一个对象，要换肤，就要写针对性的方法了，每一个控件需要用什么样的方式去换，尤其是那种，自定义的属性，怎么去set，
                // 这就对开发人员要求比较高了，而且这个换肤接口还要暴露给 自定义View的开发人员,他们去定义
                // ....
            }
        }
    }
}
