package org.lt.thinkinuihack2.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;


public class MainActivity extends Activity {

    private final static String IMAGE_SAVE_FOLDER = "/sdcard/android_shotSnap/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RelativeLayout contain = new RelativeLayout(this);
        contain.setBackgroundColor(Color.WHITE);
        this.setContentView(contain);


        //add dialog
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("ADBVV").show();
        }

        WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
//        System.out.println("LETME windowManager:" + windowManager.getClass());


        //add window
//        {
//            RelativeLayout mockWin = new RelativeLayout(this);
//            mockWin.setBackgroundColor(Color.argb(100, 0, 0, 250));
//            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            windowManager.addView(mockWin, lp);
//        }


        Field globalField = null;
        for (Field field : windowManager.getClass().getDeclaredFields()) {
//            System.out.println("LETME declaredField : " + field.getName());
            if ("mGlobal".equals(field.getName())) {
                globalField = field;
                break;
            }
        }

        if (null == globalField) {
            throw new RuntimeException("globalField is null.");
        }

        Object global = null;

        try {

            globalField.setAccessible(true);
            global = globalField.get(windowManager);
//            System.out.println("LETME global : " + global);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (null == global) {
            throw new RuntimeException("global is null.");
        }


        Object views = null;
        for (Field field : global.getClass().getDeclaredFields()) {
//            System.out.println("LETME field:" + field.getName());
            if ("mViews".equals(field.getName())) {
                field.setAccessible(true);
                try {
                    views = field.get(global);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        final View[] viewsArray = (View[]) views;

        new Handler().post(new Runnable() {
            @Override
            public void run() {

                for (View window : viewsArray) {

                    System.out.println("LETME Window " + window);

                    saveViewTreeToFile(window);
                }

                System.out.println("LETME end=======");

            }
        });


    }

    public void saveViewTreeToFile(View aView) {

        if (0 >= aView.getWidth() || 0 >= aView.getHeight()) {
            System.out.println("LETME skip a view saveViewTreeToFile.");
            return;
        }

        saveViewToFile(aView);

        if (aView instanceof ViewGroup) {

            ViewGroup vGroup = (ViewGroup) aView;

            for (int i = 0, len = vGroup.getChildCount(); i < len; i++) {
                View childView = vGroup.getChildAt(i);
                saveViewTreeToFile(childView);
            }
        }
    }

    public void saveViewToFile(View aView) {
        Bitmap bitmap = null;
        try {
            bitmap = loadBitmapFromView(aView);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null == bitmap) {
            return;
        }

        saveBitmapToFile(bitmap,
                aView.getLeft() + "_" + aView.getTop() + "_" + aView.getWidth() + "_" + aView.getHeight() + "_"
                        + System.currentTimeMillis()
        );
    }

    public Bitmap shotSnap(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }

    public Bitmap loadBitmapFromView(View v) throws Exception {
        if ((v instanceof View) && !(v instanceof ViewGroup)) {
            return shotSnap(v);
        } else {

            Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            // draw background
            Drawable backgroundDrawable = v.getBackground();
            if (null != backgroundDrawable) {
                backgroundDrawable.draw(c);
            }

            // draw content
            Class<? extends View> viewClass = v.getClass();

            System.out.println("LETME viewClass : " + viewClass.getName());

            Constructor<?>[] constructorArray = viewClass.getDeclaredConstructors();

            if (null == constructorArray) {
                return null;
            }

            Constructor<?> defaultConstructor = null;

            if ("com.android.internal.policy.impl.PhoneWindow$DecorView".equals(viewClass.getName())) {

                if (1 == constructorArray.length) {
                    defaultConstructor = constructorArray[0];
                }

            } else {

                for (Constructor<?> constructor : constructorArray) {

                    System.out.println("LETME constructor: " + constructor);

                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    if (1 == parameterTypes.length && parameterTypes[0].equals(Context.class)) {
                        defaultConstructor = constructor;
                        break;
                    }
                }

            }


//            System.out.println("LETME : default constructor:" + defaultConstructor);

            if (null == defaultConstructor) {
                return null;
            }

            Object newInstance = null;

            if ("com.android.internal.policy.impl.PhoneWindow$DecorView".equals(viewClass.getName())) {
                newInstance = defaultConstructor.newInstance(createPhoneWindow(), MainActivity.this, -1);
            } else {
                newInstance =
                        defaultConstructor.newInstance(MainActivity.this);
            }

            for (Field field : viewClass.getDeclaredFields()) {
                boolean originAccessible = field.isAccessible();
                field.setAccessible(true);
                // System.out.println("LETME Field[" + field.getName() + "," +
                // field.get(v) + "]");
                field.set(newInstance, field.get(v));
                // System.out.println("LETME Field[" + field.getName() + "," +
                // field.get(newInstance) + "]");
                field.setAccessible(originAccessible);
            }

            if (newInstance instanceof ViewGroup) {
                ViewGroup view = (ViewGroup) newInstance;
                view.removeAllViews();
            }

            if (newInstance instanceof View) {
                View view = (View) newInstance;

                view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                view.invalidate();

                view.draw(c);
            }

            return b;

        }
    }

    private Window createPhoneWindow() {
        Window ret = null;

        try {
            Class phoneWindowClass =
                    Class.forName("com.android.internal.policy.impl.PhoneWindow");

            Constructor<?>[] constructors = phoneWindowClass.getDeclaredConstructors();

            Constructor defaultConstructor = null;

            for (Constructor c : constructors) {
                if (1 == c.getParameterTypes().length) {
                    defaultConstructor = c;
                    break;
                }
            }

            if (null != defaultConstructor) {
                try {
                    ret = (Window) defaultConstructor.newInstance(MainActivity.this);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public void saveBitmapToFile(Bitmap bitmap, String aFileName) {

        if (null == bitmap) {
            return;
        }

        try {
            FileOutputStream out = new FileOutputStream(IMAGE_SAVE_FOLDER + aFileName + ".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
