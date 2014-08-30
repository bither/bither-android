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

package net.bither.ui.base;

import android.view.View;

import net.bither.preference.AppSharedPreference;
import net.bither.ui.base.CurrencyAmountView.Listener;

import java.math.BigInteger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CurrencyCalculatorLink {
    private final CurrencyAmountView btcAmountView;
    private final CurrencyAmountView localAmountView;

    private Listener listener = null;
    private boolean enabled = true;
    private double exchangeRate = 0;
    private boolean exchangeDirection = true;

    private final CurrencyAmountView.Listener btcAmountViewListener = new CurrencyAmountView
            .Listener() {
        @Override
        public void changed() {
            if (btcAmountView.getAmount() > 0) {
                exchangeDirection = true;

                update();
            } else {
                localAmountView.setHint(0);
            }

            if (listener != null) {
                listener.changed();
            }
        }

        @Override
        public void done() {
            if (listener != null) {
                listener.done();
            }
        }

        @Override
        public void focusChanged(final boolean hasFocus) {
            if (listener != null) {
                listener.focusChanged(hasFocus);
            }
        }
    };

    private final CurrencyAmountView.Listener localAmountViewListener = new CurrencyAmountView
            .Listener() {
        @Override
        public void changed() {
            if (localAmountView.getAmount() > 0) {
                exchangeDirection = false;

                update();
            } else {
                btcAmountView.setHint(0);
            }

            if (listener != null) {
                listener.changed();
            }
        }

        @Override
        public void done() {
            if (listener != null) {
                listener.done();
            }
        }

        @Override
        public void focusChanged(final boolean hasFocus) {
            if (listener != null) {
                listener.focusChanged(hasFocus);
            }
        }
    };

    public CurrencyCalculatorLink(@Nonnull final CurrencyAmountView btcAmountView,
                                  @Nonnull final CurrencyAmountView localAmountView) {
        this.btcAmountView = btcAmountView;
        this.btcAmountView.setListener(btcAmountViewListener);

        this.localAmountView = localAmountView;
        this.localAmountView.setListener(localAmountViewListener);

        update();
    }

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;

        update();
    }

    public void setExchangeRate(@Nonnull final double exchangeRate) {
        this.exchangeRate = exchangeRate;
        update();
    }

    @CheckForNull
    public long getAmount() {
        if (exchangeDirection) {
            return btcAmountView.getAmount();
        } else if (exchangeRate > 0) {
            final long localAmount = localAmountView.getAmount();
            return localAmount > 0 ? BigInteger.valueOf((long) (localAmount / exchangeRate))
                    .longValue() : 0;
        } else {
            return 0;
        }
    }

    private void update() {
        btcAmountView.setEnabled(enabled);

        if (exchangeRate > 0) {
            localAmountView.setEnabled(enabled);
            localAmountView.setCurrencySymbol(AppSharedPreference.getInstance()
                    .getDefaultExchangeType().getSymbol());

            if (exchangeDirection) {
                final long btcAmount = btcAmountView.getAmount();
                if (btcAmount > 0) {
                    localAmountView.setAmount(0, false);
                    localAmountView.setHint(BigInteger.valueOf((long) (btcAmount * exchangeRate))
                            .longValue());
                    btcAmountView.setHint(0);
                }
            } else {
                final long localAmount = localAmountView.getAmount();
                if (localAmount > 0) {
                    btcAmountView.setAmount(0, false);
                    btcAmountView.setHint(BigInteger.valueOf((long) (localAmount / exchangeRate))
                            .longValue());
                    localAmountView.setHint(0);
                }
            }
        } else {
            localAmountView.setEnabled(false);
        }
    }

    public View activeView() {
        if (exchangeDirection) {
            return btcAmountView;
        } else {
            return localAmountView;
        }
    }

    public void requestFocus() {
        activeView().requestFocus();
    }

    public void setBtcAmount(@Nonnull final long amount) {
        btcAmountView.setAmount(amount, true);
    }
}
