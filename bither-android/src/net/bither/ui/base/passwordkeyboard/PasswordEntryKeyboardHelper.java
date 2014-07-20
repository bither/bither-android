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
import android.content.res.Resources;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;

import net.bither.R;
import net.bither.util.LogUtil;

import java.lang.reflect.Method;

/**
 * Created by songchenwen on 14-7-18.
 */
public class PasswordEntryKeyboardHelper implements KeyboardView.OnKeyboardActionListener {

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
    private long[] mVibratePattern;
    private boolean mEnableHaptics = false;

    private static final int NUMERIC = 0;
    private static final int QWERTY = 1;
    private static final int QWERTY_SHIFTED = 2;
    private static final int SYMBOLS = 3;
    private static final int SYMBOLS_SHIFTED = 4;

    private Object viewRootImpl;

    int mLayouts[] = new int[]{R.xml.password_keyboard_number, R.xml.password_keyboard_letter,
            R.xml.password_keyboard_letter};

    private boolean mUsingScreenWidth;

    public PasswordEntryKeyboardHelper(Context context, KeyboardView keyboardView,
                                       View targetView) {
        this(context, keyboardView, targetView, true, null);
    }

    public PasswordEntryKeyboardHelper(Context context, KeyboardView keyboardView,
                                       View targetView, boolean useFullScreenWidth) {
        this(context, keyboardView, targetView, useFullScreenWidth, null);
    }

    public PasswordEntryKeyboardHelper(Context context, KeyboardView keyboardView,
                                       View targetView, boolean useFullScreenWidth, int layouts[]) {
        mContext = context;
        mTargetView = targetView;
        mKeyboardView = keyboardView;
        mKeyboardView.setOnKeyboardActionListener(this);
        mUsingScreenWidth = useFullScreenWidth;
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
        getViewRootImpl();
        createKeyboards();
    }

    public void createKeyboards() {
        createKeyboardsWithDefaultWidth();
    }

    public void setEnableHaptics(boolean enabled) {
        mEnableHaptics = enabled;
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
                final boolean visiblePassword = Settings.System.getInt(mContext
                        .getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD, 1) != 0;
                final boolean enablePreview = false; // TODO: grab from configuration
                mKeyboardView.setPreviewEnabled(visiblePassword && enablePreview);
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

    /**
     * Sets and enables vibrate pattern.  If id is 0 (or can't be loaded), vibrate is disabled.
     *
     * @param id resource id for array containing vibrate pattern.
     */
    public void setVibratePattern(int id) {
        int[] tmpArray = null;
        try {
            tmpArray = mContext.getResources().getIntArray(id);
        } catch (Resources.NotFoundException e) {
            if (id != 0) {
                Log.e(TAG, "Vibrate pattern missing", e);
            }
        }
        if (tmpArray == null) {
            mVibratePattern = null;
            return;
        }
        mVibratePattern = new long[tmpArray.length];
        for (int i = 0;
             i < tmpArray.length;
             i++) {
            mVibratePattern[i] = tmpArray[i];
        }
    }

    private void handleModeChange() {
        final Keyboard current = mKeyboardView.getKeyboard();
        Keyboard next = null;
        if (current == mQwertyKeyboard || current == mQwertyKeyboardShifted) {
            next = mNumericKeyboard;
        } else if (current == mNumericKeyboard) {
            next = mQwertyKeyboard;
        }
        if (next != null) {
            mKeyboardView.setKeyboard(next);
            mKeyboardState = KEYBOARD_STATE_NORMAL;
        }
    }

    public void handleBackspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        performHapticFeedback();
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

    public void onPress(int primaryCode) {
        performHapticFeedback();
    }

    private void performHapticFeedback() {
        if (mEnableHaptics) {
            mKeyboardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants
                            .FLAG_IGNORE_GLOBAL_SETTING
            );
        }
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
}
