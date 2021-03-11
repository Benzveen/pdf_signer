package com.benzveen.pdfdigitalsignature.Signature;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.View;

import com.benzveen.pdfdigitalsignature.Adapter.SignatureRecycleViewAdapter;
import com.benzveen.pdfdigitalsignature.R;
import com.benzveen.pdfdigitalsignature.utils.RecyclerViewEmptySupport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SignatureActivity extends AppCompatActivity {
    private static final int FREEHAND_Request_CODE = 43;
    private RecyclerViewEmptySupport mRecyclerView;
    List<File> items = null;
    String message;
    private SignatureRecycleViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = findViewById(R.id.create_signature);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), FreeHandActivity.class);
                startActivityForResult(intent, FREEHAND_Request_CODE);
            }
        });

        InitRecycleViewer();

        Intent intent = getIntent();
        message = intent.getStringExtra("ActivityAction");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void InitRecycleViewer() {
        mRecyclerView = findViewById(R.id.mainRecycleView);
        mRecyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        CreateDataSource();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == FREEHAND_Request_CODE && resultCode == Activity.RESULT_OK) {
            if (result != null) {
                CreateDataSource();
                mAdapter.notifyItemInserted(items.size() - 1);
            }
        }
    }

    private void CreateDataSource() {

        items = new ArrayList<>();

        File root = getFilesDir();
        File myDir = new File(root + "/FreeHand");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        File[] files = myDir.listFiles();

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long result = file2.lastModified() - file1.lastModified();
                if (result < 0) {
                    return -1;
                } else if (result > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (int i = 0; i < files.length; i++) {
            items.add(files[i]);
        }

        //set data and list adapter
        mAdapter = new SignatureRecycleViewAdapter(items);
        mAdapter.setOnItemClickListener(new SignatureRecycleViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, File value, int position) {
                if (message == null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("FileName", value.getPath());
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            }

            @Override
            public void onDeleteItemClick(View view, final File obj, final int pos) {
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(SignatureActivity.this);
                builder.setMessage("Are you sure you want to delete this Signature?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (obj.exists()) {
                                    obj.delete();
                                }
                                CreateDataSource();
                                mAdapter.notifyItemInserted(items.size() - 1);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                dialog = builder.create();
                dialog.show();

            }

        });

        mRecyclerView.setAdapter(mAdapter);
    }
}
