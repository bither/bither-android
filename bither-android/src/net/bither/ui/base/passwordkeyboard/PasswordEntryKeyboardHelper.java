/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bither.ui.base.passwordkeyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.text.InputType;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import net.bither.R;
import net.bither.util.LogUtil;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;

import java.lang.reflect.Method;

/**
 * Created by songchenwen on 14-7-18.
 */
public class PasswordEntryKeyboardHelper implements KeyboardView.OnKeyboardActionListener,
        View.OnFocusChangeListener, View.OnClickListener, View.OnTouchListener {

    public static final int AnimDuration = 400;

    public static final int KEYBOARD_MODE_ALPHA = 0;
    public static final int KEYBOARD_MODE_NUMERIC = 1;
    private static final int KEYBOARD_STATE_NORMAL = 0;
    private static final int KEYBOARD_STATE_SHIFTED = 1;
    private static final int KEYBOARD_STATE_CAPSLOCK = 2;
    private static final String TAG = "PasswordEntryKeyboardHelper";
    private int mKeyboardMode = KEYBOARD_MODE_ALPHA;
    private int mKeyboardState = KEYBOARD_STATE_NORMAL;
    private PasswordEntryKeyboard mQwertyKeyboard;
    private PasswordEntryKeyboard mQwertyKeyboardShifted;
    private PasswordEntryKeyboard mNumericKeyboard;
    private final Context mContext;
    private final View mTargetView;
    private final KeyboardView mKeyboardView;
    private final InputMethodManager imm;

    private static final int NUMERIC = 0;
    private static final int QWERTY = 1;
    private static final int QWERTY_SHIFTED = 2;

    private Object viewRootImpl;

    int mLayouts[] = new int[]{R.xml.password_keyboard_number, R.xml.password_keyboard_letter,
            R.xml.password_keyboard_letter};

    public PasswordEntryKeyboardHelper(Context context, KeyboardView keyboardView,
                                       View targetView) {
        this(context, keyboardView, targetView, null);
    }

    public PasswordEntryKeyboardHelper(Context context, KeyboardView keyboardView,
                                       View targetView, int layouts[]) {
        mContext = context;
        mTargetView = targetView;
        mKeyboardView = keyboardView;
        mKeyboardView.setOnKeyboardActionListener(this);
        if (layouts != null) {
            if (layouts.length != mLayouts.length) {
                throw new RuntimeException("Wrong number of layouts");
            }
            for (int i = 0;
                 i < mLayouts.length;
                 i++) {
                mLayouts[i] = layouts[i];
            }
        }
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        getViewRootImpl();
        createKeyboards();
        setKeyboardMode(KEYBOARD_MODE_NUMERIC);
    }

    public void createKeyboards() {
        createKeyboardsWithDefaultWidth();
    }

    public boolean isAlpha() {
        return mKeyboardMode == KEYBOARD_MODE_ALPHA;
    }

    private void createKeyboardsWithDefaultWidth() {
        mNumericKeyboard = new PasswordEntryKeyboard(mContext, mLayouts[NUMERIC]);
        mQwertyKeyboard = new PasswordEntryKeyboard(mContext, mLayouts[QWERTY], 0);
        mQwertyKeyboard.enableShiftLock();

        mQwertyKeyboardShifted = new PasswordEntryKeyboard(mContext, mLayouts[QWERTY_SHIFTED], 0);
        mQwertyKeyboardShifted.enableShiftLock();
        mQwertyKeyboardShifted.setShifted(true); // always shifted.
    }

    public void setKeyboardMode(int mode) {
        switch (mode) {
            case KEYBOARD_MODE_ALPHA:
                mKeyboardView.setKeyboard(mQwertyKeyboard);
                mKeyboardState = KEYBOARD_STATE_NORMAL;
                mKeyboardView.setPreviewEnabled(false);
                break;
            case KEYBOARD_MODE_NUMERIC:
                mKeyboardView.setKeyboard(mNumericKeyboard);
                mKeyboardState = KEYBOARD_STATE_NORMAL;
                mKeyboardView.setPreviewEnabled(false); // never show popup for numeric keypad
                break;
        }
        mKeyboardMode = mode;
    }

    private void sendKeyEventsToTarget(int character) {
        if (viewRootImpl == null) {
            getViewRootImpl();
        }
        KeyEvent[] events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(new
                char[]{(char) character});
        try {
            Method method = viewRootImpl.getClass().getDeclaredMethod("dispatchKeyFromIme",
                    KeyEvent.class);
            method.setAccessible(true);
            if (events != null) {
                final int N = events.length;
                for (int i = 0;
                     i < N;
                     i++) {
                    KeyEvent event = events[i];
                    event = KeyEvent.changeFlags(event, event.getFlags() | KeyEvent
                            .FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
                    method.invoke(viewRootImpl, event);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "can not dispatch input event");
        }
    }

    public void sendDownUpKeyEvents(int keyEventCode) {
        if (viewRootImpl == null) {
            getViewRootImpl();
        }
        long eventTime = SystemClock.uptimeMillis();
        try {
            Method method = viewRootImpl.getClass().getDeclaredMethod("dispatchKeyFromIme",
                    KeyEvent.class);
            method.setAccessible(true);
            method.invoke(viewRootImpl, new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN,
                    keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
            method.invoke(viewRootImpl, new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP,
                    keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "can not dispatch key from ime");
        }
    }

    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE && mKeyboardView != null) {
            handleModeChange();
        } else if (primaryCode == PasswordEntryKeyboard.KEYCODE_ENTER) {
            handleEnter();
        } else {
            handleCharacter(primaryCode, keyCodes);
            // Switch back to old keyboard if we're not in capslock mode
            if (mKeyboardState == KEYBOARD_STATE_SHIFTED) {
                // skip to the unlocked state
                mKeyboardState = KEYBOARD_STATE_CAPSLOCK;
                handleShift();
            }
        }
    }

    private void handleEnter() {
        View currentFocusView = mKeyboardView.getRootView().findFocus();
        if (currentFocusView == null) {
            return;
        }
        if (currentFocusView instanceof EditText) {
            EditText currentFocusEt = (EditText) currentFocusView;
            if (currentFocusEt.getImeActionId() > 0) {
                currentFocusEt.onEditorAction(currentFocusEt.getImeActionId());
            } else {
                View nextFocusView = currentFocusEt.focusSearch(View.FOCUS_DOWN);
                if(nextFocusView != null){
                    nextFocusView.requestFocus(View.FOCUS_DOWN);
                    return;
                }else{
                    if(imm.isActive(currentFocusEt)) {
                        imm.hideSoftInputFromWindow(currentFocusEt.getWindowToken(), 0);
                    }
                    hideKeyboard();
                    return;
                }
            }
        }
    }

    private void handleModeChange() {
        final Keyboard current = mKeyboardView.getKeyboard();
        Keyboard next = null;
        if (current == mQwertyKeyboard || current == mQwertyKeyboardShifted) {
            next = mNumericKeyboard;
            mKeyboardMode = KEYBOARD_MODE_NUMERIC;
        } else if (current == mNumericKeyboard) {
            next = mQwertyKeyboard;
            mKeyboardMode = KEYBOARD_MODE_ALPHA;
        }
        if (next != null) {
            mKeyboardView.setKeyboard(next);
            mKeyboardState = KEYBOARD_STATE_NORMAL;
        }
    }

    public void handleBackspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    }

    private void handleShift() {
        if (mKeyboardView == null) {
            return;
        }
        Keyboard current = mKeyboardView.getKeyboard();
        PasswordEntryKeyboard next = null;
        final boolean isAlphaMode = current == mQwertyKeyboard || current == mQwertyKeyboardShifted;
        if (mKeyboardState == KEYBOARD_STATE_NORMAL) {
            mKeyboardState = isAlphaMode ? KEYBOARD_STATE_SHIFTED : KEYBOARD_STATE_CAPSLOCK;
            next = mQwertyKeyboardShifted;
        } else if (mKeyboardState == KEYBOARD_STATE_SHIFTED) {
            mKeyboardState = KEYBOARD_STATE_CAPSLOCK;
            next = mQwertyKeyboardShifted;
        } else if (mKeyboardState == KEYBOARD_STATE_CAPSLOCK) {
            mKeyboardState = KEYBOARD_STATE_NORMAL;
            next = mQwertyKeyboard;
        }
        if (next != null) {
            if (next != current) {
                mKeyboardView.setKeyboard(next);
            }
            next.setShiftLocked(mKeyboardState == KEYBOARD_STATE_CAPSLOCK);
            mKeyboardView.setShifted(mKeyboardState != KEYBOARD_STATE_NORMAL);
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        // Maybe turn off shift if not in capslock mode.
        if (mKeyboardView.isShifted() && primaryCode != ' ' && primaryCode != '\n') {
            primaryCode = Character.toUpperCase(primaryCode);
        }
        sendKeyEventsToTarget(primaryCode);
    }

    private void handleClose() {
        hideKeyboard();
    }

    private void getViewRootImpl() {
        if (mTargetView != null) {
            try {
                Method method = View.class.getDeclaredMethod("getViewRootImpl");
                method.setAccessible(true);
                viewRootImpl = method.invoke(mTargetView);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e(TAG, "can not get view root imp");
            }
        } else {
            viewRootImpl = null;
        }
    }

    public int getKeyboardHeight() {
        Keyboard keyboard = mKeyboardView.getKeyboard();
        if (keyboard == null) {
            return 0;
        }
        return keyboard.getHeight();
    }

    @Override
    public void onPress(int primaryCode) {

    }

    public void onRelease(int primaryCode) {

    }

    public void onText(CharSequence text) {

    }

    public void swipeDown() {

    }

    public void swipeLeft() {

    }

    public void swipeRight() {

    }

    public void swipeUp() {

    }

    public boolean isKeyboardShown() {
        if (mKeyboardView == null) {
            return false;
        }
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    public void showKeyboard() {
        if (mKeyboardView == null) {
            return;
        }
        if(mKeyboardView.getKeyboard() == mQwertyKeyboardShifted){
            mKeyboardView.setKeyboard(mQwertyKeyboard);
            mKeyboardState = KEYBOARD_STATE_NORMAL;
        }
        mKeyboardView.removeCallbacks(showKeyboardRunnable);
        configureDoneButton();
        if (isKeyboardShown()) {
            return;
        }
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean result = imm.hideSoftInputFromWindow(mKeyboardView.getWindowToken(), 0,
                        new ResultReceiver(null) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (resultCode == InputMethodManager.RESULT_HIDDEN || resultCode
                                        == InputMethodManager.RESULT_UNCHANGED_HIDDEN) {
                                    mKeyboardView.postDelayed(showKeyboardRunnable, 100);
                                }
                                super.onReceiveResult(resultCode, resultData);
                            }
                        }
                );
                if (!result) {
                    mKeyboardView.setVisibility(View.VISIBLE);
                    mKeyboardView.setEnabled(true);
                }
            }
        });
    }

    private Runnable showKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            if (mKeyboardView != null) {
                mKeyboardView.setVisibility(View.VISIBLE);
                mKeyboardView.setEnabled(true);
            }
        }
    };

    public void hideKeyboard() {
        if (mKeyboardView == null) {
            return;
        }
        mKeyboardView.removeCallbacks(showKeyboardRunnable);
        if (!isKeyboardShown()) {
            return;
        }
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                imm.hideSoftInputFromWindow(mKeyboardView.getWindowToken(), 0, null);
                mKeyboardView.setVisibility(View.GONE);
                mKeyboardView.setEnabled(false);
            }
        });
    }

    public PasswordEntryKeyboardHelper registerEditText(EditText... ets) {
        for (EditText et : ets) {
            et.setOnFocusChangeListener(this);
            et.setOnClickListener(this);
            et.setOnTouchListener(this);
        }
        return this;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            showKeyboard();
        } else {
            hideKeyboard();
        }
    }

    @Override
    public void onClick(View v) {
        showKeyboard();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v instanceof EditText) {
            EditText edittext = (EditText) v;
            int inType = edittext.getInputType();       // Backup the input type
            edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
            edittext.onTouchEvent(event);               // Call native handler
            edittext.setInputType(inType);              // Restore input type
            edittext.setSelection(edittext.getText().length());
            return true;
        }
        return false;
    }

    private void configureDoneButton() {
        if (mKeyboardView == null) {
            return;
        }
        View currentFocusView = mKeyboardView.getRootView().findFocus();
        if (currentFocusView == null) {
            return;
        }
        if (currentFocusView instanceof EditText) {
            EditText currentFocusEt = (EditText) currentFocusView;
            if (!StringUtil.isEmpty(currentFocusEt.getImeActionLabel() == null ? null :
                    currentFocusEt.getImeActionLabel().toString())) {
                mNumericKeyboard.setEnterKeyText(currentFocusEt.getImeActionLabel());
                mQwertyKeyboard.setEnterKeyText(currentFocusEt.getImeActionLabel());
                mQwertyKeyboardShifted.setEnterKeyText(currentFocusEt.getImeActionLabel());
                return;
            }
            if (currentFocusEt.getImeActionId() > 0) {
                switch (currentFocusEt.getImeActionId()) {
                    case EditorInfo.IME_ACTION_DONE:
                    case EditorInfo.IME_ACTION_GO:
                    case EditorInfo.IME_ACTION_SEND:
                        mNumericKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                                R.string.password_keyboard_done);
                        mQwertyKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                                R.string.password_keyboard_done);
                        mQwertyKeyboardShifted.setEnterKeyResources(mContext.getResources(), 0, 0,
                                R.string.password_keyboard_done);
                        return;
                    case EditorInfo.IME_ACTION_NEXT:
                        mNumericKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                                R.string.password_keyboard_next);
                        mQwertyKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                                R.string.password_keyboard_next);
                        mQwertyKeyboardShifted.setEnterKeyResources(mContext.getResources(), 0, 0,
                                R.string.password_keyboard_next);
                        return;
                    default:
                        break;
                }
            }
            View nextFocusView = currentFocusEt.focusSearch(View.FOCUS_DOWN);
            if (nextFocusView != null) {
                mNumericKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                        R.string.password_keyboard_next);
                mQwertyKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                        R.string.password_keyboard_next);
                mQwertyKeyboardShifted.setEnterKeyResources(mContext.getResources(), 0, 0,
                        R.string.password_keyboard_next);
            } else {
                mNumericKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                        R.string.password_keyboard_done);
                mQwertyKeyboard.setEnterKeyResources(mContext.getResources(), 0, 0,
                        R.string.password_keyboard_done);
                mQwertyKeyboardShifted.setEnterKeyResources(mContext.getResources(), 0, 0,
                        R.string.password_keyboard_done);
            }
            PasswordEntryKeyboard keyboard = (PasswordEntryKeyboard) mKeyboardView.getKeyboard();
            mKeyboardView.invalidateKey(keyboard.getEnterKeyIndex());
        }
    }
}
