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

package net.bither.fragment.cold;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import net.bither.R;
import net.bither.model.BitherAddressWithPrivateKey;
import net.bither.model.Check;
import net.bither.model.Check.CheckListener;
import net.bither.ui.base.CheckHeaderView;
import net.bither.ui.base.CheckHeaderView.CheckHeaderViewListener;
import net.bither.ui.base.DialogAddressFull;
import net.bither.ui.base.WrapLayoutParamsForAnimator;
import net.bither.util.CheckUtil;
import net.bither.util.StringUtil;
import net.bither.util.WalletUtils;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ObjectAnimator;

public class CheckFragment extends Fragment implements CheckHeaderViewListener {

	private static final int PrivateKeyCheckThreadCount = 1;
	private static final int ListExpandAnimDuration = 500;

	private FrameLayout flContainer;
	private CheckHeaderView vCheckHeader;
	private FrameLayout fl;
	private ListView lv;

	private int checkCount;
	private int checkFinishedCount;

	private ArrayList<CheckPoint> checkPoints = new ArrayList<CheckPoint>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		int resource = R.layout.fragment_check_cold;
		View view = inflater.inflate(resource, container, false);
		flContainer = (FrameLayout) view.findViewById(R.id.fl_container);
		vCheckHeader = (CheckHeaderView) view.findViewById(R.id.v_check_header);
		lv = (ListView) view.findViewById(R.id.lv);
		fl = (FrameLayout) view.findViewById(R.id.fl);
		lv.setStackFromBottom(false);
		lv.setAdapter(adapter);
		vCheckHeader.setListener(this);
		return view;
	}

	private void check(final List<Check> checks, final int threadCount) {
		checkCount = checks.size();
		checkFinishedCount = 0;
		vCheckHeader.setTotalCheckCount(checkCount);
		vCheckHeader.setPassedCheckCount(0);
		lv.postDelayed(new Runnable() {
			@Override
			public void run() {
				CheckUtil.runChecks(checks, threadCount);
			}
		}, 600);
	}

	private class CheckPoint implements CheckListener {
		private boolean waiting;
		private boolean checking;
		private boolean result;
		private String address;

		public CheckPoint(String address) {
			waiting = true;
			this.address = address;
		}

		@Override
		public void onCheckBegin(Check check) {
			checking = true;
			waiting = false;
			adapter.notifyDataSetChanged();
			final int index = checkPoints.indexOf(this);
			int itemHeight = lv.getChildAt(0).getHeight();
			int lvHeight = lv.getHeight() - lv.getPaddingBottom()
					- lv.getPaddingTop();
			lv.setSelectionFromTop(index, lvHeight - itemHeight);
		}

		@Override
		public void onCheckEnd(Check check, boolean success) {
			checking = false;
			result = success;
			checkFinishedCount++;
			if (success) {
				vCheckHeader.addPassedCheckCount();
			}
			adapter.notifyDataSetChanged();
			if (checkFinishedCount >= checkCount) {
				vCheckHeader.postDelayed(new Runnable() {
					@Override
					public void run() {
						vCheckHeader.stop();
					}
				}, 600);
			}
		}

		public boolean isWaiting() {
			return waiting;
		}

		public boolean isChecking() {
			return checking;
		}

		public boolean getResult() {
			return result;
		}

		public String getAddress() {
			return address;
		}
	}

	@Override
	public void beginCheck(String password) {
		final List<BitherAddressWithPrivateKey> addresses = WalletUtils
				.getPrivateAddressList();
		checkPoints.clear();
		final ArrayList<Check> checks = new ArrayList<Check>();
		for (int i = 0; i < addresses.size(); i++) {
			BitherAddressWithPrivateKey address = addresses.get(i);
			CheckPoint point = new CheckPoint(address.getAddress());
			checkPoints.add(point);
			checks.add(CheckUtil.initCheckForPrivateKey(address, password)
					.setCheckListener(point));
		}
		adapter.notifyDataSetChanged();
		if (lv.getHeight() <= 0) {
			int lvHeight = flContainer.getHeight() - vCheckHeader.getHeight();
			ObjectAnimator animator = ObjectAnimator.ofInt(
					new WrapLayoutParamsForAnimator(fl), "height", lvHeight)
					.setDuration(ListExpandAnimDuration);
			animator.addListener(new AnimatorListener() {

				@Override
				public void onAnimationEnd(Animator animation) {
					check(checks, PrivateKeyCheckThreadCount);
				}

				@Override
				public void onAnimationStart(Animator animation) {
				}

				@Override
				public void onAnimationRepeat(Animator animation) {
				}

				@Override
				public void onAnimationCancel(Animator animation) {
				}
			});
			animator.start();
		} else {
			check(checks, PrivateKeyCheckThreadCount);
		}
	}

    public void check(){
        vCheckHeader.check();
    }

	private BaseAdapter adapter = new BaseAdapter() {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder h;
			if (convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(
						R.layout.list_item_check, null);
				h = new ViewHolder(convertView);
			} else {
				h = (ViewHolder) convertView.getTag();
			}
			CheckPoint point = getItem(position);
			h.tv.setText(getSpannableStringFromAddress(point.getAddress()));
			h.ibtnFull.setOnClickListener(new AddressFullClick(point
					.getAddress()));
			if (point.isWaiting()) {
				h.pb.setVisibility(View.GONE);
				h.iv.setVisibility(View.GONE);
			} else {
				if (point.isChecking()) {
					h.pb.setVisibility(View.VISIBLE);
					h.iv.setVisibility(View.GONE);
				} else {
					h.iv.setVisibility(View.VISIBLE);
					h.pb.setVisibility(View.GONE);
					if (point.getResult()) {
						h.iv.setImageResource(R.drawable.checkmark);
					} else {
						h.iv.setImageResource(R.drawable.check_failed);
					}
				}
			}
			return convertView;
		}

		private SpannableString getSpannableStringFromAddress(String address) {
			address = StringUtil.shortenAddress(address);
			String a = address.substring(0, 4);
			String str = String.format(
					getString(R.string.check_address_private_key_title),
					address);
			int indexOfAddress = str.indexOf(a);
			SpannableString spannable = new SpannableString(str);
			spannable.setSpan(new TypefaceSpan("monospace"), indexOfAddress,
					indexOfAddress + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			return spannable;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public CheckPoint getItem(int position) {
			return checkPoints.get(position);
		}

		@Override
		public int getCount() {
			return checkPoints.size();
		}

		class ViewHolder {
			TextView tv;
			ProgressBar pb;
			ImageView iv;
			ImageButton ibtnFull;

			public ViewHolder(View v) {
				tv = (TextView) v.findViewById(R.id.tv_check_title);
				pb = (ProgressBar) v.findViewById(R.id.pb_check);
				iv = (ImageView) v.findViewById(R.id.iv_check_state);
				ibtnFull = (ImageButton) v.findViewById(R.id.ibtn_address_full);
				v.setTag(this);
			}
		}

		class AddressFullClick implements OnClickListener {
			private String address;

			public AddressFullClick(String address) {
				this.address = address;
			}

			@Override
			public void onClick(View v) {
				ArrayMap<String, BigInteger> map = new ArrayMap<String, BigInteger>();
				map.put(address, null);
				DialogAddressFull dialog = new DialogAddressFull(getActivity(),
						map);
				dialog.show(v);
			}
		};
	};
}
