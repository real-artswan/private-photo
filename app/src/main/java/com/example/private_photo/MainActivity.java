package com.example.private_photo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.private_photo.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ImageViewViewModel viewModel;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    //    private boolean isTempPhotoAvailable() {
//        return tempPhotoFile != null && tempPhotoFile.exists();
//    }
    private File getTempImageFile() throws IOException {
        File outputDir = this.getCacheDir();
        String uniqueID = UUID.randomUUID().toString();
        File outputFile = File.createTempFile(uniqueID, ".tmp", outputDir);
        outputFile.deleteOnExit();
        return outputFile;
    }

    private void dispatchTakePictureIntent() {
//        if (isTempPhotoAvailable()) {
//            tempPhotoFile.delete();
//        }
        View view = findViewById(android.R.id.content);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File tempPhotoFile;
            try {
                tempPhotoFile = getTempImageFile();
            } catch (IOException ex) {
                Snackbar.make(view, "Could not create temp file!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
            // Continue only if the File was successfully created
            Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID +".fileprovider", tempPhotoFile);
            Log.d(this.getLocalClassName(), photoURI.toString());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            viewModel.fileName.setValue(tempPhotoFile.getAbsolutePath());
            viewModel.isPreview.setValue(true);
            activityResultLauncher.launch(takePictureIntent);
        } else {
            // display error state to the user
            Snackbar.make(view, "Camera is not available!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ImageViewViewModel.class);


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            navController.navigateUp(); // to clear previous navigation history
                            navController.navigate(R.id.ImageViewFragment);
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}