package com.benzveen.pdfdigitalsignature.utils;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.benzveen.pdfdigitalsignature.R;
import com.benzveen.pdfdigitalsignature.Signature.SignatureUtils;
import com.benzveen.pdfdigitalsignature.Signature.SignatureView;

import java.io.File;

public class PDSSignatureUtils {

    private static PopupWindow sSignaturePopUpMenu;
    private static View mSignatureLayout;
    public static SignatureView showFreeHandView(Context mCtx, File file) {

        SignatureView createFreeHandView = SignatureUtils.createFreeHandView((((int) mCtx.getResources().getDimension(R.dimen.sign_menu_width)) - ((int) mCtx.getResources().getDimension(R.dimen.sign_left_offset))) - (((int) mCtx.getResources().getDimension(R.dimen.sign_right_offset)) * 3), ((int) mCtx.getResources().getDimension(R.dimen.sign_button_height)) - ((int) mCtx.getResources().getDimension(R.dimen.sign_top_offset)), file, mCtx);
        LayoutParams layoutParams = new LayoutParams(-2, -2);
        layoutParams.addRule(9);
        layoutParams.setMargins((int) mCtx.getResources().getDimension(R.dimen.sign_left_offset), (int) mCtx.getResources().getDimension(R.dimen.sign_top_offset), 0, 0);
        createFreeHandView.setLayoutParams(layoutParams);
        return  createFreeHandView;

       /* createFreeHandView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FASSignatureUtils.addSignElement(z);
            }
        });*/
    }

    public static boolean isSignatureMenuOpen() {
        return sSignaturePopUpMenu != null && sSignaturePopUpMenu.isShowing();
    }
    public static void dismissSignatureMenu() {
        if (sSignaturePopUpMenu != null && sSignaturePopUpMenu.isShowing()) {
            sSignaturePopUpMenu.dismiss();
            mSignatureLayout = null;
        }
    }
}
