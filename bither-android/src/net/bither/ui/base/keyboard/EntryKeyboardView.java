/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.ui.base.keyboard;

import android.content.Context;
import android.graphics.Paint;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.util.LogUtil;
import net.bither.util.StringUtil;
import net.bither.util.ThreadUtil;
import net.bither.util.UIUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class EntryKeyboardView extends KeyboardView implements KeyboardView
        .OnKeyboardActionListener, View.OnFocusChangeListener, View.OnClickListener,
        View.OnTouchListener, EntryKeyboard.DrawKeyListener {
    public static interface EntryKeyboardViewListener {
        public void onEntryKeyboardHide(EntryKeyboardView v);

        public void onEntryKeyboardShow(EntryKeyboardView v);
    }

    private static final String TAG = "EntryKeyboardHelper";

    public static final int KEYBOARD_MODE_ALPHA = 0;
    public static final int KEYBOARD_MODE_NUMERIC = 1;
    private static final int KEYBOARD_STATE_NORMAL = 0;
    private static final int KEYBOARD_STATE_SHIFTED = 1;
    private static final int KEYBOARD_STATE_CAPSLOCK = 2;

    public static final int[] ActionKeyCodes = new int[]{Keyboard.KEYCODE_DELETE,
            Keyboard.KEYCODE_CANCEL, Keyboard.KEYCODE_MODE_CHANGE, EntryKeyboard.KEYCODE_SHIFT,
            EntryKeyboard.KEYCODE_ENTER};

    static {
        Arrays.sort(ActionKeyCodes);
    }

    private static final float NormalKeyShadowRadius = 3;
    private static final float ActionKeyShadowRadius = 2;

    private static float QwertyKeyWidth = BitherApplication.mContext.getResources().getFraction(R
            .fraction.password_keyboard_letter_key_width, 1, UIUtil.getScreenWidth());
    private static float QwertyKeyHorizontalGap = BitherApplication.mContext.getResources()
            .getFraction(R.fraction.password_keyboard_letter_key_horizontal_gap, 1,
                    UIUtil.getScreenWidth());

    private static float NumberKeyWidth = BitherApplication.mContext.getResources().getFraction(R
            .fraction.password_keyboard_number_key_width, 1, UIUtil.getScreenWidth());
    private static float NumberKeyHorizontalGap = BitherApplication.mContext.getResources()
            .getFraction(R.fraction.password_keyboard_number_key_horizontal_gap, 1,
                    UIUtil.getScreenWidth());

    private int mKeyboardMode = KEYBOARD_MODE_ALPHA;
    private int mKeyboardState = KEYBOARD_STATE_NORMAL;

    private EntryKeyboard mQwertyKeyboard;
    private EntryKeyboard mQwertyKeyboardShifted;
    private EntryKeyboard mNumericKeyboard;

    private InputMethodManager imm;

    private Object viewRootImpl;

    private EntryKeyboardViewListener listener;

    private boolean canGetViewRootImpl;

    public EntryKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EntryKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        setOnKeyboardActionListener(EntryKeyboardView.this);
        createKeyboards();
        setKeyboardMode(getDefaultKeyboardMode());
    }

    public void createKeyboards() {
        createKeyboardsWithDefaultWidth();
    }

    public boolean isAlpha() {
        return mKeyboardMode == KEYBOARD_MODE_ALPHA;
    }

    private void createKeyboardsWithDefaultWidth() {
        if (getNumericKeyboard() != 0) {
            mNumericKeyboard = new EntryKeyboard(getContext(), getNumericKeyboard());
        }
        if (getAlphaKeyboard() != 0) {
            mQwertyKeyboard = new EntryKeyboard(getContext(), getAlphaKeyboard(), 0);
            mQwertyKeyboard.enableShiftLock();

            mQwertyKeyboardShifted = new EntryKeyboard(getContext(), getAlphaKeyboard(), 0);
            mQwertyKeyboardShifted.enableShiftLock();
            mQwertyKeyboardShifted.setShifted(true); // always shifted.

            mNumericKeyboard.setDrawKeyListener(this);
            mQwertyKeyboard.setDrawKeyListener(this);
            mQwertyKeyboardShifted.setDrawKeyListener(this);
        }
    }

    protected abstract int getAlphaKeyboard();

    protected abstract int getNumericKeyboard();

    protected abstract int getDefaultKeyboardMode();

    public void setKeyboardMode(int mode) {
        switch (mode) {
            case KEYBOARD_MODE_ALPHA:
                setKeyboard(mQwertyKeyboard);
                mKeyboardState = KEYBOARD_STATE_NORMAL;
                break;
            case KEYBOARD_MODE_NUMERIC:
                setKeyboard(mNumericKeyboard);
                mKeyboardState = KEYBOARD_STATE_NORMAL;
                break;
        }
        mKeyboardMode = mode;
    }

    private void sendKeyEventsToTarget(int character) {
        if (viewRootImpl == null && canGetViewRootImpl) {
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
        if (viewRootImpl == null && canGetViewRootImpl) {
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
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            handleModeChange();
        } else if (primaryCode == EntryKeyboard.KEYCODE_ENTER) {
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

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        // Maybe turn off shift if not in capslock mode.
        if (isShifted() && primaryCode != ' ' && primaryCode != '\n') {
            primaryCode = Character.toUpperCase(primaryCode);
        }
        if (canGetViewRootImpl) {
            sendKeyEventsToTarget(primaryCode);
        } else {
            EditText editText = getCurrentInputView();
            int start = editText.getSelectionStart();
            Editable editable = editText.getText();
            editable.insert(start, Character.toString((char) primaryCode));
        }
    }

    public void handleBackspace() {
        if (canGetViewRootImpl) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        } else {
            EditText editText = getCurrentInputView();
            int start = editText.getSelectionStart();
            Editable editable = editText.getText();
            if (editable != null && start > 0) {
                editable.delete(start - 1, start);
            }
        }
    }

    private void handleEnter() {
        View currentFocusView = getRootView().findFocus();
        if (currentFocusView == null) {
            return;
        }
        if (currentFocusView instanceof EditText) {
            EditText currentFocusEt = (EditText) currentFocusView;
            if (currentFocusEt.getImeActionId() > 0) {
                currentFocusEt.onEditorAction(currentFocusEt.getImeActionId());
            } else {
                View nextFocusView = currentFocusEt.focusSearch(View.FOCUS_DOWN);
                if (nextFocusView != null) {
                    nextFocusView.requestFocus(View.FOCUS_DOWN);
                    return;
                } else {
                    if (imm.isActive(currentFocusEt)) {
                        imm.hideSoftInputFromWindow(currentFocusEt.getWindowToken(), 0);
                    }
                    hideKeyboard();
                    return;
                }
            }
        }
    }

    private void handleModeChange() {
        final Keyboard current = getKeyboard();
        Keyboard next = null;
        if (current == mQwertyKeyboard || current == mQwertyKeyboardShifted) {
            next = mNumericKeyboard;
            if (next != null) {
                mKeyboardMode = KEYBOARD_MODE_NUMERIC;
            }
        } else if (current == mNumericKeyboard) {
            next = mQwertyKeyboard;
            if (next != null) {
                mKeyboardMode = KEYBOARD_MODE_ALPHA;
            }
        }
        if (next != null) {
            setKeyboard(next);
            mKeyboardState = KEYBOARD_STATE_NORMAL;
        }
    }

    private void handleShift() {
        if (mQwertyKeyboard == null || mQwertyKeyboardShifted == null) {
            return;
        }
        Keyboard current = getKeyboard();
        EntryKeyboard next = null;
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
                setKeyboard(next);
            }
            next.setShiftLocked(mKeyboardState == KEYBOARD_STATE_CAPSLOCK);
            setShifted(mKeyboardState != KEYBOARD_STATE_NORMAL);
        }
    }

    private void handleClose() {
        hideKeyboard();
    }

    private void getViewRootImpl() {
        try {
            Method method = View.class.getDeclaredMethod("getViewRootImpl");
            method.setAccessible(true);
            viewRootImpl = method.invoke(this);
            if (viewRootImpl != null) {
                viewRootImpl.getClass().getDeclaredMethod("dispatchKeyFromIme", KeyEvent.class);
            }
            canGetViewRootImpl = true;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "can not get view root imp");
            canGetViewRootImpl = false;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewRootImpl();
    }

    public int getKeyboardHeight() {
        Keyboard keyboard = getKeyboard();
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
        return getVisibility() == View.VISIBLE;
    }

    public void showKeyboard() {
        if (getKeyboard() == mQwertyKeyboardShifted) {
            setKeyboard(mQwertyKeyboard);
            mKeyboardState = KEYBOARD_STATE_NORMAL;
        }
        removeCallbacks(showKeyboardRunnable);
        configureDoneButton();
        if (isKeyboardShown()) {
            return;
        }
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean result = imm.hideSoftInputFromWindow(getWindowToken(), 0,
                        new ResultReceiver(null) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (resultCode == InputMethodManager.RESULT_HIDDEN || resultCode
                                        == InputMethodManager.RESULT_UNCHANGED_HIDDEN) {
                                    postDelayed(showKeyboardRunnable, 100);
                                }
                                super.onReceiveResult(resultCode, resultData);
                            }
                        });
                if (!result) {
                    showKeyboardRunnable.run();
                }
            }
        });
    }

    private Runnable showKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            setVisibility(View.VISIBLE);
            setEnabled(true);
        }
    };

    public void hideKeyboard() {
        removeCallbacks(showKeyboardRunnable);
        if (!isKeyboardShown()) {
            return;
        }
        ThreadUtil.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                imm.hideSoftInputFromWindow(getWindowToken(), 0, null);
                setVisibility(View.GONE);
                setEnabled(false);
            }
        });
    }

    @Override
    public boolean handleBack() {
        if (super.handleBack()) {
            return true;
        } else if (isKeyboardShown()) {
            hideKeyboard();
            return true;
        }
        return false;
    }

    public EntryKeyboardView registerEditText(EditText... ets) {
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
        View currentFocusView = getRootView().findFocus();
        if (currentFocusView == null) {
            return;
        }
        if (currentFocusView instanceof EditText) {
            EditText currentFocusEt = (EditText) currentFocusView;
            if (!StringUtil.isEmpty(currentFocusEt.getImeActionLabel() == null ? null :
                    currentFocusEt.getImeActionLabel().toString())) {
                setEnterKeyText(currentFocusEt.getImeActionLabel());
            } else if (currentFocusEt.getImeActionId() > 0) {
                switch (currentFocusEt.getImeActionId()) {
                    case EditorInfo.IME_ACTION_DONE:
                    case EditorInfo.IME_ACTION_GO:
                    case EditorInfo.IME_ACTION_SEND:
                        setEnterKeyText(getResources().getString(R.string.password_keyboard_done));
                        break;
                    case EditorInfo.IME_ACTION_NEXT:
                        setEnterKeyText(getResources().getString(R.string.password_keyboard_next));
                        break;
                    default:
                        break;
                }
            } else {
                View nextFocusView = currentFocusEt.focusSearch(View.FOCUS_DOWN);
                if (nextFocusView != null) {
                    setEnterKeyText(getResources().getString(R.string.password_keyboard_next));
                } else {
                    setEnterKeyText(getResources().getString(R.string.password_keyboard_done));
                }
            }
            EntryKeyboard keyboard = (EntryKeyboard) getKeyboard();
            invalidateKey(keyboard.getEnterKeyIndex());
        }
    }

    private void setEnterKeyText(CharSequence text) {
        if (mNumericKeyboard != null) {
            mNumericKeyboard.setEnterKeyText(text);
        }
        if (mQwertyKeyboard != null) {
            mQwertyKeyboard.setEnterKeyText(text);
        }
        if (mQwertyKeyboardShifted != null) {
            mQwertyKeyboardShifted.setEnterKeyText(text);
        }
    }

    public EntryKeyboardViewListener getListener() {
        return listener;
    }

    public EntryKeyboardView setListener(EntryKeyboardViewListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (listener != null) {
            if (visibility == View.VISIBLE) {
                listener.onEntryKeyboardShow(this);
            } else {
                listener.onEntryKeyboardHide(this);
            }
        }
    }

    private EditText getCurrentInputView() {
        View v = getRootView().findFocus();
        if (v != null && v instanceof EditText) {
            return (EditText) v;
        }
        return null;
    }

    @Override
    public int[] beforeDrawKeyWithStates(Keyboard.Key key, int[] states) {
        boolean isActionKey = Arrays.binarySearch(ActionKeyCodes, key.codes[0]) > -1;
        if (isActionKey) {
            states = Arrays.copyOf(states, states.length + 1);
            states[states.length - 1] = android.R.attr.state_active;
        }

        try {
            Paint paint = getPaint();
            if (isActionKey) {
                paint.setColor(getContext().getResources().getColor(R.color
                        .password_keyboard_action_key_text));
                setShadowColor(getContext().getResources().getColor(R.color
                        .password_keyboard_action_key_shadow));
                setShadowRadius(ActionKeyShadowRadius);
            } else {
                paint.setColor(getContext().getResources().getColor(R.color
                        .password_keyboard_normal_key_text));
                setShadowColor(getContext().getResources().getColor(R.color
                        .password_keyboard_normal_key_shadow));
                setShadowRadius(NormalKeyShadowRadius);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "can not use different text color");
        }

        setPreviewEnabled(!isActionKey);
        if (isActionKey) {
            try {
                getPreviewText().setVisibility(INVISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e(TAG, "can not hide preview");
            }
        }
        return states;
    }

    @Override
    public void getLocationInWindow(int[] location) {
        // override to make preview centered
        super.getLocationInWindow(location);
        try {
            TextView previewText = getPreviewText();
            float keyWidth = getKeyWidth();
            float keyGap = getKeyHorizontalGap();
            previewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int previewWidth = previewText.getMeasuredWidth();
            if (previewWidth > keyWidth) {
                location[0] = (int) (location[0] + keyWidth / 2 - previewWidth / 2 + keyGap * 2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "can not center preview");
        }
    }

    public float getKeyWidth() {
        if (getKeyboard() == null) {
            return 0;
        }
        if (getKeyboard() == mNumericKeyboard) {
            return NumberKeyWidth;
        }
        return QwertyKeyWidth;
    }

    public float getKeyHorizontalGap() {
        if (getKeyboard() == null) {
            return 0;
        }
        if (getKeyboard() == mNumericKeyboard) {
            return NumberKeyHorizontalGap;
        }
        return QwertyKeyHorizontalGap;
    }

    private Paint getPaint() throws Exception {
        Field field = KeyboardView.class.getDeclaredField("mPaint");
        field.setAccessible(true);
        return (Paint) field.get(this);
    }

    private int getKeyTextColor() throws Exception {
        Field field = KeyboardView.class.getDeclaredField("mKeyTextColor");
        field.setAccessible(true);
        return field.getInt(this);
    }

    private void setShadowColor(int color) throws Exception {
        Field field = KeyboardView.class.getDeclaredField("mShadowColor");
        field.setAccessible(true);
        field.setInt(this, color);
    }

    private void setShadowRadius(float radius) throws Exception {
        Field field = KeyboardView.class.getDeclaredField("mShadowRadius");
        field.setAccessible(true);
        field.setFloat(this, radius);
    }

    private TextView getPreviewText() throws Exception {
        Field field = KeyboardView.class.getDeclaredField("mPreviewText");
        field.setAccessible(true);
        return (TextView) field.get(this);
    }
}
