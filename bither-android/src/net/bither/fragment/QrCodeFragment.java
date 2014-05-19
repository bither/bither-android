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

package net.bither.fragment;

import net.bither.R;
import net.bither.ui.base.QrCodeImageView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class QrCodeFragment extends Fragment {
	public static interface QrCodeFragmentDelegate {
		public String getContent();

		public void btnPressed();

		public String getButtonTitle();

		public int pageIndex();

		public int pageCount();
	}

	private QrCodeFragmentDelegate delegate;

	private QrCodeImageView ivQrCode;
	private TextView tv;
	private Button btn;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_qr_code, container, false);
		ivQrCode = (QrCodeImageView) v.findViewById(R.id.iv_qrcode);
		tv = (TextView) v.findViewById(R.id.tv);
		btn = (Button) v.findViewById(R.id.btn);
		btn.setOnClickListener(btnClick);
		configureView();
		return v;
	}

	public void setDelegate(QrCodeFragmentDelegate delegate) {
		this.delegate = delegate;
		configureView();
	}

	private void configureView() {
		if (delegate != null && ivQrCode != null) {
			ivQrCode.setContent(delegate.getContent());
			btn.setText(delegate.getButtonTitle());
			if (delegate.pageIndex() == 0 && delegate.pageCount() == 1) {
				tv.setVisibility(View.GONE);
			} else {
				tv.setVisibility(View.VISIBLE);
				tv.setText(String.format(getString(R.string.qr_code_page),
						delegate.pageIndex() + 1, delegate.pageCount()));
			}
		}
	}

	private OnClickListener btnClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (delegate != null) {
				delegate.btnPressed();
			}
		}
	};

}
