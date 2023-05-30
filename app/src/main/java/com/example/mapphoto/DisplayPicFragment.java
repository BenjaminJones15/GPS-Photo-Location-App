package com.example.mapphoto;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.DialogCompat;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

import androidx.fragment.app.Fragment;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DisplayPicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DisplayPicFragment extends DialogFragment {

    Uri picUri;

    public static DisplayPicFragment newInstance(Uri mediaUri) {
        DisplayPicFragment frag = new DisplayPicFragment();
        Bundle args = new Bundle();
        args.putString("uri", mediaUri.toString());
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        picUri = Uri.parse(requireArguments().getString("uri"));
        Log.wtf("dialog", picUri.toString());
        ImageView iv;

        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        View myView = inflater.inflate(R.layout.fragment_display_pic, null);
        iv = myView.findViewById(R.id.imageView);
        Bitmap bitmap = null;

        try {
            bitmap = BitmapFactory.decodeStream(requireActivity().getContentResolver().openInputStream(picUri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        iv.setImageBitmap(bitmap);

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(requireActivity(), R.style.Theme_MapPhoto));
        builder.setView(myView).setTitle("Picture Taken.");
        builder.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }
}