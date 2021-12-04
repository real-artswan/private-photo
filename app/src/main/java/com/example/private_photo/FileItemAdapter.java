package com.example.private_photo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileItemAdapter extends RecyclerView.Adapter<FileItemAdapter.FileItemViewHolder> {

    public interface FileItemListener {
        void onItemClick(String item, int position);
        void onItemDelete(String item, int position);
    }

    private Context context;
    private List<String> dataset;
    private FileItemListener fileItemListener;

    FileItemAdapter(Context context, List<String> dataset, FileItemListener fileItemListener) {
        this.context = context;
        this.dataset = dataset;
        this.fileItemListener = fileItemListener;
    }

    @NonNull
    @Override
    public FileItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View adapterLayout = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_list_item, parent, false);

        return new FileItemViewHolder(adapterLayout);

    }

    @Override
    public void onBindViewHolder(@NonNull FileItemViewHolder holder, int position) {
        String item = dataset.get(position);
        int p = position;
        holder.textView.setText(item);
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                fileItemListener.onItemClick(item, p);
            }
        });
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Log.d(getClass().getName(), "Delete click");
                fileItemListener.onItemDelete(item, p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just an Affirmation object.
    class FileItemViewHolder extends RecyclerView.ViewHolder  {

        TextView textView;
        View deleteButton;
        public FileItemViewHolder(@NonNull View fileItemView) {
            super(fileItemView);
            textView = fileItemView.findViewById(R.id.text_file_name);
            deleteButton = fileItemView.findViewById(R.id.btn_delete_file);
        }
    }

}
