package com.benzveen.pdfdigitalsignature;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.benzveen.pdfdigitalsignature.Adapter.MainRecycleViewAdapter;
import com.benzveen.pdfdigitalsignature.Signature.SignatureActivity;
import com.benzveen.pdfdigitalsignature.utils.RecyclerViewEmptySupport;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.Menu;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final int Merge_Request_CODE = 43;
    private RecyclerViewEmptySupport recyclerView;
    List<File> items = null;
    private MainRecycleViewAdapter mAdapter;
    private BottomSheetDialog mBottomSheetDialog;
    Uri pdfData = null;
    private static final int RQS_OPEN_DOCUMENT_TREE = 24;
    private File selectedFile;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), DigitalSignatureActivity.class);
                intent.putExtra("ActivityAction", "FileSearch");
                startActivityForResult(intent, Merge_Request_CODE);
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        CheckStoragePermission();

        recyclerView = findViewById(R.id.mainRecycleView);
        recyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            CheckStoragePermission();
        }

        HandleExternalData();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Merge_Request_CODE && resultCode == Activity.RESULT_OK) {
            if (result != null) {
                CreateDataSource();
                mAdapter.notifyItemInserted(items.size() - 1);
            }
        }
        if (resultCode == RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE) {
            if (result != null) {
                Uri uriTree = result.getData();
                DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);
                if (selectedFile != null) {
                    DocumentFile newFile = documentFile.createFile("application/pdf", selectedFile.getName());
                    try {
                        copy(selectedFile, newFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    selectedFile = null;
                    if (mBottomSheetDialog != null)
                        mBottomSheetDialog.dismiss();
                    Toast toast = Toast.makeText(this, "Copy files to: " + documentFile.getName(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        CreateDataSource();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_signatures) {

            mHandler.postDelayed(mUpdateTimeTask, 100);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            Intent intent = new Intent(getApplicationContext(), SignatureActivity.class);
            intent.putExtra("ActivityAction", "Open");
            startActivity(intent);
        }
    };

    public void copy(File selectedFile, DocumentFile newFile) throws IOException {
        try {
            OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
            FileInputStream in = new FileInputStream(selectedFile.getPath());
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void HandleExternalData() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri imageUri = null;
        if ((Intent.ACTION_SEND.equals(action) || Intent.ACTION_VIEW.equals(action)) && type != null) {
            if ("application/pdf".equals(type)) {
                if (Intent.ACTION_SEND.equals(action))
                    imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                else if (Intent.ACTION_VIEW.equals(action))
                    imageUri = intent.getData();
                if (imageUri != null) {
                    ArrayList<Uri> list = new ArrayList<>();
                    list.add(imageUri);
                    StartSignatureActivity("PDFOpen", list);
                }
            }
        }
    }

    public void StartSignatureActivity(String message, ArrayList<Uri> imageUris) {
        Intent intent = new Intent(getApplicationContext(), DigitalSignatureActivity.class);
        intent.putExtra("ActivityAction", message);
        intent.putExtra("PDFOpen", imageUris);
        startActivityForResult(intent, Merge_Request_CODE);
    }

    private void CheckStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Storage Permission");
                alertDialog.setMessage("Storage permission is required in order to " +
                        "provide Image to PDF feature, please enable permission in app settings");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                startActivity(i);
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        2);
            }
        }
    }

    private void CreateDataSource() {
        if (recyclerView != null) {
            items = new ArrayList<File>();

            File root = getFilesDir();
            File myDir = new File(root + "/DigitalSignature");
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
            mAdapter = new MainRecycleViewAdapter(this, items);
            mAdapter.setOnItemClickListener(new MainRecycleViewAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, File value, int position) {
                    showBottomSheetDialog(value);
                }

                @Override
                public void onItemLongClick(View view, File obj, int pos) {
                    showBottomSheetDialog(obj);
                }

            });

            recyclerView.setAdapter(mAdapter);
        }
    }


    private void showBottomSheetDialog(final File currentFile) {
        final View view = getLayoutInflater().inflate(R.layout.sheet_list, null);

        (view.findViewById(R.id.lyt_email)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
                Intent target = new Intent(Intent.ACTION_SEND);
                target.setType("text/plain");
                target.putExtra(Intent.EXTRA_STREAM, contentUri);
                target.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                startActivity(Intent.createChooser(target, "Send via Email..."));
            }
        });

        (view.findViewById(R.id.lyt_share)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
                Intent target = ShareCompat.IntentBuilder.from(MainActivity.this).setStream(contentUri).getIntent();
                target.setData(contentUri);
                target.setType("application/pdf");
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (target.resolveActivity(getPackageManager()) != null) {
                    startActivity(target);
                }
            }
        });

        (view.findViewById(R.id.lyt_rename)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
                showCustomRenameDialog(currentFile);

            }
        });

        (view.findViewById(R.id.lyt_delete)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
                showCustomDeleteDialog(currentFile);

            }
        });

        (view.findViewById(R.id.lyt_copyTo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE);
                selectedFile = currentFile;
            }
        });

        (view.findViewById(R.id.lyt_openFile)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetDialog.dismiss();
                Intent target = new Intent(Intent.ACTION_VIEW);
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", currentFile);
                target.setDataAndType(contentUri, "application/pdf");
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent intent = Intent.createChooser(target, "Open File");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Snackbar.make(mCoordLayout, "Install PDF reader application.", Snackbar.LENGTH_LONG).show();
                }

            }
        });
        mBottomSheetDialog = new BottomSheetDialog(this);
        mBottomSheetDialog.setContentView(view);

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBottomSheetDialog = null;
            }
        });
    }

    public void showCustomRenameDialog(final File currentFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.rename_layout, null);
        builder.setView(view);
        final EditText editText = (EditText) view.findViewById(R.id.renameEditText2);
        editText.setText(currentFile.getName());
        builder.setTitle("Rename");
        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                File root = getFilesDir();
                File file = new File(root + "/DigitalSignature", editText.getText().toString());
                currentFile.renameTo(file);
                dialog.dismiss();
                CreateDataSource();
                mAdapter.notifyItemInserted(items.size() - 1);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showCustomDeleteDialog(final File currentFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure want to delete this file?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                currentFile.delete();
                CreateDataSource();
                mAdapter.notifyItemInserted(items.size() - 1);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
