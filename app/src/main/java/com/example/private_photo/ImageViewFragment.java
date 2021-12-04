package com.example.private_photo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class ImageViewFragment extends Fragment {

    private boolean isPreview = false;

    private ImageViewViewModel viewModel;
    private FragmentImageViewBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private boolean deleteFile() {
        String fn = viewModel.fileName.getValue();
        if (fn == null) return true;
        File file = new File(fn);
        return file.delete();
    }

    private void saveEncryptedImage(String password) throws Exception {
        String fileName = new SimpleDateFormat("'IMG_'yy-MM-dd-HH:mm:ss'.ppenc'", Locale.getDefault()).format(new Date());
        File encryptedFile = new File(requireActivity().getExternalFilesDir(null), fileName);

        FileInputStream imageFileStream = new FileInputStream(viewModel.fileName.getValue());
        FileOutputStream encryptedFileStream = new FileOutputStream(encryptedFile);
        try {
            PPEncryption.encryptData(imageFileStream, encryptedFileStream, password);
        } finally {
            imageFileStream.close();
            encryptedFileStream.close();
        }
    }

    private InputStream pipeOutputToInputStream(ByteArrayOutputStream outputStream) throws IOException {
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

    private Bitmap loadEncryptedImage(String password) throws Exception {
        FileInputStream encryptedData = new FileInputStream(viewModel.fileName.getValue());
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
                if (deleteFile()) {
                    NavHostFragment.findNavController(ImageViewFragment.this)
                            .navigate(R.id.action_SecondFragment_to_FirstFragment);
                }
                return true;
            }
            case R.id.action_save: {
                try {
                    boolean isPreview = viewModel.isPreview.getValue() == null || viewModel.isPreview.getValue();
                    if (isPreview) {
                        PasswordProvider.getPassword(requireActivity().getSupportFragmentManager(), password -> {
                            try {
                                saveEncryptedImage(password);
                                NavHostFragment.findNavController(ImageViewFragment.this)
                                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Snackbar.make(this.requireView(), "Could not save image!", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        });
                    }
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
        item.setVisible(isPreview);
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
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ImageViewViewModel.class);
        viewModel.fileName.observe(getViewLifecycleOwner(), fileName -> {
            Log.d(this.getClass().getName(), "fileName " + fileName);

            boolean isPreview = viewModel.isPreview.getValue() == null || viewModel.isPreview.getValue();
            if (isPreview) {
                Bitmap imageBitmap = BitmapFactory.decodeFile(fileName);
                binding.imageView.setImageBitmap(imageBitmap);
            } else {
                PasswordProvider.getPassword(requireActivity().getSupportFragmentManager(), password -> {
                    if (password == null) {
                        NavHostFragment.findNavController(ImageViewFragment.this)
                                .navigate(R.id.action_SecondFragment_to_FirstFragment);
                        return;
                    }
                    try {
                        Bitmap imageBitmap = loadEncryptedImage(password);
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
        });
        viewModel.isPreview.observe(getViewLifecycleOwner(), isPreview -> this.isPreview = isPreview);
    }

    @Override
    public void onDestroyView() {
        boolean isPreview = viewModel.isPreview.getValue() == null || viewModel.isPreview.getValue();
        if (isPreview) {
            String fileName = viewModel.fileName.getValue();
            if (fileName != null) {
                File file = new File(fileName);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        super.onDestroyView();
        binding = null;
    }

}