package com.benzveen.pdfdigitalsignature.Adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;


import com.benzveen.pdfdigitalsignature.R;
import com.benzveen.pdfdigitalsignature.Signature.SignatureView;
import com.benzveen.pdfdigitalsignature.utils.PDSSignatureUtils;

import java.io.File;
import java.util.List;

public class SignatureRecycleViewAdapter extends RecyclerView.Adapter<SignatureRecycleViewAdapter.MyViewHolder> {
    private List<File> signatures;

    public SignatureRecycleViewAdapter(List<File> myDataset) {
        signatures = myDataset;
    }

    private OnItemClickListener onClickListener = null;

    public void setOnItemClickListener(OnItemClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.signature_item, viewGroup, false);

        MyViewHolder vh = new MyViewHolder(viewGroup.getContext(), v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder myViewHolder, final int i) {

        SignatureView signatureView = (SignatureView) myViewHolder.layout.getChildAt(0);
        if (signatureView != null) {
            myViewHolder.layout.removeViewAt(0);
        }

        signatureView = PDSSignatureUtils.showFreeHandView(myViewHolder.context, signatures.get(i));
        myViewHolder.layout.addView(signatureView);

        myViewHolder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener == null) return;
                onClickListener.onItemClick(v, signatures.get(i), myViewHolder.getAdapterPosition());
            }

        });
        signatureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener == null) return;
                onClickListener.onItemClick(v, signatures.get(i), myViewHolder.getAdapterPosition());
            }

        });

        myViewHolder.deleteSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener == null) return;
                onClickListener.onDeleteItemClick(v, signatures.get(i), myViewHolder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return signatures.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public FrameLayout layout;
        public Context context;
        public ImageButton deleteSignature;
        public SignatureView signatureView;

        public MyViewHolder(Context cont, View v) {
            super(v);
            context = cont;
            layout = v.findViewById(R.id.freehanditem);
            deleteSignature = v.findViewById(R.id.deleteSignature);
            signatureView = v.findViewById(R.id.signatureview);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, File obj, int pos);

        void onDeleteItemClick(View view, File obj, int pos);
    }

}
