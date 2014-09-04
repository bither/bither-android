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

package net.bither.adapter.hot;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import net.bither.fragment.hot.HotAddressFragment;
import net.bither.fragment.hot.MarketFragment;
import net.bither.fragment.hot.OptionHotFragment;

public class HotFragmentPagerAdapter extends FragmentPagerAdapter {

	@SuppressWarnings("rawtypes")
	private Class fragments[] = new Class[] { MarketFragment.class,
			HotAddressFragment.class, OptionHotFragment.class };

	public HotFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int index) {
		try {
			return (Fragment) fragments[index].newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int getCount() {
		return 3;
	}

}
