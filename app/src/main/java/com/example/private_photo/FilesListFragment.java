package com.example.private_photo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.private_photo.databinding.FragmentFilesListBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilesListFragment extends Fragment {



    private FragmentFilesListBinding binding;
    private final List<String> filesList = new ArrayList<String>();
    private FileItemAdapter adapter;

    private void reloadFiles() {
        filesList.clear();
        File dir = this.getActivity().getExternalFilesDir(null);
        this.filesList.addAll(Arrays.asList(dir.list()));
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        ImageViewViewModel viewModel = new ViewModelProvider(getActivity()).get(ImageViewViewModel.class);
        adapter = new FileItemAdapter(this.getActivity(), this.filesList, new FileItemAdapter.FileItemListener() {
            @Override
            public void onItemClick(String item, int position) {
                viewModel.isPreview.setValue(false);
                String fullFileName = new File(getActivity().getExternalFilesDir(null), item).getAbsolutePath();
                viewModel.fileName.setValue(fullFileName);
                NavHostFragment.findNavController(FilesListFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
            @Override
            public void onItemDelete(String item, int position) {
                File fullFileName = new File(getActivity().getExternalFilesDir(null), item);
                fullFileName.delete();
                filesList.remove(position);
                adapter.notifyItemRemoved(position);
            }
        });
        binding = FragmentFilesListBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(adapter);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reloadFiles();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}