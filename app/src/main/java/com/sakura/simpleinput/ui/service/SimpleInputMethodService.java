package com.sakura.simpleinput.ui.service;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.sakura.simpleinput.R;
import com.sakura.simpleinput.utils.ClipboardUtil;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * @author zhangzheng & xianfei
 * @Date 2020-05-20
 * @ClassName SimpleInputMethodService
 * <p>
 * Desc :
 */
public class SimpleInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    private String TAG = "zz";
    /**
     * 自定义的KeyboardView
     */
    private KeyboardView keyboardView;
    /**
     * 自定义的Keyboard
     */
    private Keyboard keyboard;
    private View view;
    private TextView mTitleTv;
    private TextView mContentTv;
//    private Typeface typeFace;
    private int index;
    private String results;
    /**
     * use this method to split chinese.
     */
    private String []str;
    private boolean isInputing = false;
    private StringBuilder stringBuilder = new StringBuilder();
    private Disposable mDisposable;


    /**
     * 当软键盘隐藏时停止输入
     *
     * @version
     * @date 2019-07-08 16:43
     * @author zhangzheng
     */
    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        Log.d(TAG, "onWindowHidden()");
        destroyTime();
    }


    /**
     * 键盘显示时获取当前粘贴板内容
     *
     * @version
     * @date 2019-07-16 11:00
     * @author zhangzheng
     */
    @Override
    public void onWindowShown() {
        super.onWindowShown();
        results = ClipboardUtil.getClipContent(this);
        mContentTv.setText(results);
        str=results.split("");
    }

    /**
     * 键盘首次创建时候调用
     *
     * @return
     */
    @Override
    public View onCreateInputView() {
        view = getLayoutInflater().inflate(R.layout.layout_keyboard_view, null);
        initKeyView(view);
        initView(view);
        return view;
    }

    /**
     * 初始化软键盘相关view
     *
     * @param view
     * @version
     * @date 2019-07-08 16:46
     * @author zhangzheng
     */
    private void initKeyView(View view) {
        // keyboard被创建后，将调用onCreateInputView函数
        keyboardView = view.findViewById(R.id.keyboard);
        keyboard = new Keyboard(this, R.xml.qwerty);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
    }

    /**
     * 初始化标题、内容view
     *
     * @param view
     * @version
     * @date 2019-07-08 16:46
     * @author zhangzheng
     */
    private void initView(View view) {
//        typeFace = Typeface.createFromAsset(getAssets(), "font.ttf");
        mTitleTv = view.findViewById(R.id.input_title_tv);
//        mTitleTv.setTypeface(typeFace);
        mContentTv = view.findViewById(R.id.input_content_tv);
        mContentTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        results = ClipboardUtil.getClipContent(this);
        mContentTv.setText(results);
        str=results.split("");
    }

    /**
     * 删除当前文字
     *
     * @version
     * @date 2019-07-16 11:01
     * @author zhangzheng
     */
    public void deleteText() {
        getCurrentInputConnection().deleteSurroundingText(1, 0);
    }

    /**
     * 停止计时器
     *
     * @version
     * @date 2019-07-16 11:03
     * @author zhangzheng
     */
    public void destroyTime() {
        if (mDisposable != null)
            mDisposable.dispose();
    }


    public void inputCommitText() {
        this.index = 0;
        startTask();
    }


    /**
     * @param codePoint character
     * @return is emoji?
     */
    private static boolean isEmojiCharacter(char codePoint) {
        return !((codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA) || (codePoint == 0xD)
                || ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
                || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF)));

    }

    /**
     * method of input chars automatically.
     *
     * @author xianfei
     */
    public void startTask() {
        // add random delay for input.
        mDisposable = (Disposable) Flowable.interval(new Random().nextInt(50)+5, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        InputConnection inputConnection = SimpleInputMethodService.this.getCurrentInputConnection();
                        if (index >= str.length) {
                            mDisposable.dispose();
                            inputConnection.commitText(" ", 1); // input a blank at the end
                            return;
                        }
                        // add input random numbers of char.
                        for (int i = 0; i < new Random().nextInt(3)+1; i++) {
                            stringBuilder.append(str[index]);
                            stringBuilder.append("");
                            index++;
                            if (index >= str.length)break;
                            if(isEmojiCharacter(str[index].charAt(0))){stringBuilder.append(str[index]);index++;}
                        }
                        inputConnection.commitText(stringBuilder.toString(), 1);
                        stringBuilder.delete(0, stringBuilder.length());
                    }
                }).doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {

                    }
                }).subscribe();

    }


    @SuppressLint("WrongConstant")
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        switch (primaryCode) {
            case 102:
                if (isInputing) {
                    destroyTime();
                } else {
                    inputCommitText();
                }
                break;
            case 101:
                ((InputMethodManager) getSystemService("input_method")).showInputMethodPicker();
                break;
            case 100:
                deleteText();
                break;
            case 103:
                mContentTv.setText("");
                ClipboardUtil.clearClip(this);
                break;
            default:
                break;
        }

    }

    @Override
    public void onText(CharSequence text) {
        Log.e(TAG, "onText:" + text);
    }

    @Override
    public void onPress(int primaryCode) {
        Log.e(TAG, "onPress:" + primaryCode);
    }

    @Override
    public void onRelease(int primaryCode) {
        Log.e(TAG, "onRelease:" + primaryCode);
    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}
