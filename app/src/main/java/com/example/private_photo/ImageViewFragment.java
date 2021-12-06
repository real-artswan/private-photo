package com.example.private_photo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.private_photo.databinding.FragmentImageViewBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class ImageViewFragment extends Fragment {

    // These matrices will be used to scale points of the image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // The 3 states (events) which the user is trying to perform
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // these PointF objects are used to record the point(s) the user is touching
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    private FragmentImageViewBinding binding;

    private File tempPhotoFile;
    private ActivityResultLauncher<Intent> activityLauncher;
    private String fileName = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null)
            fileName = getArguments().getString("fileName", null);
        activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap imageBitmap = BitmapFactory.decodeFile(tempPhotoFile.getAbsolutePath());
                        binding.imageView.setImageBitmap(imageBitmap);
                    }
                });
    }

    private File getTempImageFile() throws IOException {
        File outputDir = requireActivity().getCacheDir();
        String uniqueID = UUID.randomUUID().toString();
        File outputFile = File.createTempFile(uniqueID, ".tmp", outputDir);
        outputFile.deleteOnExit();
        return outputFile;
    }

    private void dispatchTakePictureIntent() {
        View view = requireActivity().findViewById(android.R.id.content);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            try {
                tempPhotoFile = getTempImageFile();
            } catch (IOException ex) {
                Snackbar.make(view, "Could not create temp file!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
            // Continue only if the File was successfully created
            Uri photoURI = FileProvider.getUriForFile(requireActivity(), BuildConfig.APPLICATION_ID +".fileprovider", tempPhotoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            activityLauncher.launch(takePictureIntent);
        } else {
            // display error state to the user
            Snackbar.make(view, "Camera is not available!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private static boolean deleteFile(String fileName) {
        if (fileName == null) return true;
        File file = new File(fileName);
        return file.delete();
    }

    private void saveEncryptedPreviewImage(String password) throws Exception {
        String fileName = new SimpleDateFormat("'IMG_'yy-MM-dd-HH:mm:ss'.ppenc'", Locale.getDefault()).format(new Date());
        File encryptedFile = new File(requireActivity().getExternalFilesDir(null), fileName);

        FileInputStream imageFileStream = new FileInputStream(tempPhotoFile);
        FileOutputStream encryptedFileStream = new FileOutputStream(encryptedFile);
        try {
            PPEncryption.encryptData(imageFileStream, encryptedFileStream, password);
        } finally {
            imageFileStream.close();
            encryptedFileStream.close();
        }
    }

    private static InputStream pipeOutputToInputStream(ByteArrayOutputStream outputStream) throws IOException {
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        new Thread(() -> {
            try {
                // write the original OutputStream to the PipedOutputStream
                // note that in order for the below method to work, you need
                // to ensure that the data has finished writing to the
                // ByteArrayOutputStream
                outputStream.writeTo(out);
            }
            catch (IOException e) {
                // logging and exception handling should go here
                e.printStackTrace();
            }
            finally {
                // close the PipedOutputStream here because we're done writing data
                // once this thread has completed its run
                // close the PipedOutputStream cleanly
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return in;
    }

    private static Bitmap loadEncryptedImage(String password, String fileName) throws Exception {
        FileInputStream encryptedData = new FileInputStream(fileName);
        ByteArrayOutputStream decryptedData = new ByteArrayOutputStream();
        try {
            PPEncryption.decryptData(encryptedData, decryptedData, password);
            InputStream in = pipeOutputToInputStream(decryptedData);
            return BitmapFactory.decodeStream(in);
        } finally {
            encryptedData.close();
            decryptedData.close();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:  {
                String fn = fileName;
                if (fileName == null) {
                    fn = tempPhotoFile.getAbsolutePath();
                }
                if (deleteFile(fn)) {
                    NavHostFragment.findNavController(ImageViewFragment.this)
                            .navigate(R.id.action_SecondFragment_to_FirstFragment);
                }
                return true;
            }
            case R.id.action_save: {
                if (fileName != null) return true;
                try {
                    PasswordProvider.getPassword(requireActivity().getSupportFragmentManager(), requireContext(), password -> {
                        try {
                            saveEncryptedPreviewImage(password);
                            NavHostFragment.findNavController(ImageViewFragment.this)
                                    .navigate(R.id.action_SecondFragment_to_FirstFragment);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.make(this.requireView(), "Could not save image!", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(this.requireView(), "Could not save image!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_save);
        item.setVisible(fileName == null);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_image_view, menu);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentImageViewBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        binding.imageView.setOnTouchListener((View var1, MotionEvent var2) -> touchImageListener((ImageView) var1, var2));
        super.onViewCreated(view, savedInstanceState);
        if (fileName == null) {
            dispatchTakePictureIntent();
        } else {
            Log.d(this.getClass().getName(), "fileName " + fileName);
            PasswordProvider.getPassword(requireActivity().getSupportFragmentManager(), requireContext(), password -> {
                if (password == null) {
                    NavHostFragment.findNavController(ImageViewFragment.this)
                            .navigate(R.id.action_SecondFragment_to_FirstFragment);
                    return;
                }
                try {
                    Bitmap imageBitmap = loadEncryptedImage(password, fileName);
                    binding.imageView.setImageBitmap(imageBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(requireView(), "Could not load encrypted image!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    NavHostFragment.findNavController(ImageViewFragment.this)
                            .navigate(R.id.action_SecondFragment_to_FirstFragment);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        if (tempPhotoFile != null && tempPhotoFile.exists()) {
            tempPhotoFile.delete();
        }
        super.onDestroyView();
        binding = null;
    }

    /*
     * --------------------------------------------------------------------------
     * Method: spacing Parameters: MotionEvent Returns: float Description:
     * checks the spacing between the two fingers on touch
     * ----------------------------------------------------
     */

    private float spacing(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
    /*
     * --------------------------------------------------------------------------
     * Method: midPoint Parameters: PointF object, MotionEvent Returns: void
     * Description: calculates the midpoint between the two fingers
     * ------------------------------------------------------------
     */

    private void midPoint(PointF point, MotionEvent event)
    {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private boolean touchImageListener(ImageView view, MotionEvent event) {
        view.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;

        // Handle touch events here...

        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:   // first finger down only
                matrix.set(view.getImageMatrix());
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: // first finger lifted

            case MotionEvent.ACTION_POINTER_UP: // second finger lifted

                mode = NONE;
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

                oldDist = spacing(event);
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (mode == DRAG)
                {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                }
                else if (mode == ZOOM)
                {
                    // pinch zooming
                    float newDist = spacing(event);
                    if (newDist > 5f)
                    {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist; // setting the scaling of the
                        // matrix...if scale > 1 means
                        // zoom in...if scale < 1 means
                        // zoom out
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix); // display the transformation on screen
        return true;
    }
}