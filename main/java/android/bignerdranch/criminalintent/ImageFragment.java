package android.bignerdranch.criminalintent;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import java.io.File;

public class ImageFragment extends DialogFragment {
    private static final String ARG_IMAGE = "IMAGE_ZOOM";

    public static ImageFragment newInstance(File photoFile){
        Bundle args = new Bundle();
        args.putSerializable(ARG_IMAGE, photoFile);
        ImageFragment fragment = new ImageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        File photoFile = (File)getArguments().getSerializable(ARG_IMAGE);
        Bitmap image = PictureUtils.getScaledBitmap(photoFile.getPath(), getActivity());
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_image, null);
        ImageView imageView = v.findViewById(R.id.zoom_picture);
        imageView.setImageBitmap(image);

        return new AlertDialog.Builder(getActivity()).setView(imageView).create();
    }

}

