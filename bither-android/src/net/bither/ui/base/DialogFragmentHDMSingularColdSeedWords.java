package net.bither.ui.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Joiner;

import net.bither.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/2/13.
 */
public class DialogFragmentHDMSingularColdSeedWords extends Fragment {
    private static final String WordsTag = "words";
    private List<String> words;

    public static DialogFragmentHDMSingularColdSeedWords newInstance(List<String> words) {
        DialogFragmentHDMSingularColdSeedWords page = new DialogFragmentHDMSingularColdSeedWords();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordsTag, new ArrayList<String>(words));
        page.setArguments(bundle);
        return page;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        words = (List<String>) bundle.getSerializable(WordsTag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_hdm_singular_cold_seed_words, null);
        TextView tv = (TextView) v.findViewById(R.id.tv);
        tv.setText(Joiner.on("-").join(words));
        return v;
    }

}
