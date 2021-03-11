package com.benzveen.pdfdigitalsignature.Document;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.benzveen.pdfdigitalsignature.DigitalSignatureActivity;
import com.benzveen.pdfdigitalsignature.R;

public class PDSFragment extends Fragment {

    PDSPageViewer mPageViewer = null;

    public static PDSFragment newInstance(int i) {
        PDSFragment fASFragment = new PDSFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("pageNum", i);
        fASFragment.setArguments(bundle);
        return fASFragment;
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_layout, viewGroup, false);
        LinearLayout linearLayout = (LinearLayout) inflate.findViewById(R.id.fragment);
        try {
            PDSPageViewer fASPageViewer = new PDSPageViewer(viewGroup.getContext(),(DigitalSignatureActivity) getActivity(),((DigitalSignatureActivity) getActivity()).getDocument().getPage(getArguments().getInt("pageNum")));
            this.mPageViewer = fASPageViewer;
            linearLayout.addView(fASPageViewer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inflate;
    }

    public void onDestroyView() {
        if (this.mPageViewer != null) {
            this.mPageViewer.cancelRendering();
            this.mPageViewer = null;
        }
        super.onDestroyView();
    }
}
