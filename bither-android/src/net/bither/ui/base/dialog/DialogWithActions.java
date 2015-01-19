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

package net.bither.ui.base.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;

import java.util.List;

/**
 * Created by songchenwen on 15/1/8.
 */
public abstract class DialogWithActions extends CenterDialog implements DialogInterface
        .OnDismissListener, View.OnClickListener {
    private static final int ActionTagIndex = R.id.dialog_with_actions_action;
    private View clickedView;

    public DialogWithActions(Context context) {
        super(context);
        setContentView(R.layout.dialog_with_actions);
        setOnDismissListener(this);
    }

    private void initView() {
        findViewById(R.id.tv_close).setOnClickListener(this);
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_action_container);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        List<Action> actions = getActions();
        for (Action a : actions) {
            View v = inflater.inflate(R.layout.dialog_with_actions_list_item, ll, false);
            TextView tvName = (TextView) v.findViewById(R.id.tv_name);
            tvName.setText(a.getName());
            tvName.setTag(ActionTagIndex, a);
            tvName.setOnClickListener(this);
            ll.addView(v);
        }
    }

    @Override
    public void show() {
        clickedView = null;
        initView();
        super.show();
    }

    @Override
    public void onClick(View v) {
        clickedView = v;
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (clickedView != null) {
            Object t = clickedView.getTag(ActionTagIndex);
            if (t != null && t instanceof Action) {
                Runnable r = ((Action) t).getAction();
                if(r != null){
                    r.run();
                }
            }
        }
    }

    protected abstract List<Action> getActions();

    public static final class Action {
        private String name;
        private Runnable action;

        public Action(String name, Runnable action) {
            setName(name);
            setAction(action);
        }

        public Action(int name, Runnable action) {
            setName(BitherApplication.mContext.getString(name));
            setAction(action);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Runnable getAction() {
            return action;
        }

        public void setAction(Runnable action) {
            this.action = action;
        }

    }

    public static abstract class DialogWithActionsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            new DialogWithActions(v.getContext()) {
                @Override
                protected List<Action> getActions() {
                    return DialogWithActionsClickListener.this.getActions();
                }
            }.show();
        }

        protected abstract List<Action> getActions();
    }
}
