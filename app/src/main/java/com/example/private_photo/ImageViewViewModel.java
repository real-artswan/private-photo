package com.example.private_photo;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;

public class ImageViewViewModel extends ViewModel {
    final MutableLiveData<String> fileName = new MutableLiveData();
    final MutableLiveData<Boolean> isPreview = new MutableLiveData(true);
}
