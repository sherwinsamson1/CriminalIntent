package android.bignerdranch.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CrimeFragment extends Fragment {
    // request Codes:
    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;
    // DatePickerFragment's tag:
    private static final String DIALOG_DATE = "DialogDate";
    private static final String ARG_CRIME_ID = "crime_id";

    private Crime mCrime;               // A crime object reference.
    private EditText mTitleField;       // an EditText reference
    private Button mDateButton;         // a Button reference
    private CheckBox mSolvedCheckBox;   // CheckBox reference
    private Button mReportButton;       // a reference to the report crime button.
    private Button mSuspectButton;      // a reference to the pick suspect button.
    private ImageButton mPhotoButton;   // references for the photo and
    private ImageView mPhotoView;       // camera button
    private File mPhotoFile;            // stores the location for the file



    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;

        if(requestCode == REQUEST_DATE) {
            Date date = (Date) data.getSerializableExtra(
                    DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            mDateButton.setText(mCrime.getDate().toString());
        }
        // handle the request for contact info
        else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // want the query to return values for these fields
            String[] queryFields = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME
            };
            // do the query, the Uri is the "where" clause
            Cursor c = getActivity().getContentResolver().query(
                    contactUri, queryFields, null, null, null);

            try {
                if(c.getCount() == 0)
                    return;

                // get the field -- the suspect's name
                c.moveToFirst();
                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            } finally {
                c.close();
            }
        }
        // handle the request for taking a picture
        else if (requestCode == REQUEST_PHOTO) {
            updatePhotoView();

            // done taking the picture now, revoke the permission
            // to write files.
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    mPhotoFile);

            getActivity().revokeUriPermission(
                    uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


        }
    }



    @Override
    public void onPause(){
        super.onPause();

        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID)getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
    }



    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(
                R.layout.fragment_crime,        // layout resource id
                container,                      // the view's parent
                false);             // view gets added in view activity's code.

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() { // set listener
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,   // this is what the user types
                                      int start,
                                      int before,
                                      int count) {
                mCrime.setTitle(s.toString());          // used to set the title
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Required to implement this method, but don't need it.
            }
        });



        mDateButton = (Button) v.findViewById(R.id.crime_date);
        mDateButton.setText(mCrime.getDate().toString());
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                DatePickerFragment dialog =
                        new DatePickerFragment().newInstance(mCrime.getDate());

                // set the target fragment:
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(fm, DIALOG_DATE);
            }
        });


        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton buttonView,
                            boolean isChecked) {
                        mCrime.setSolved(isChecked);
                    }
                });


        // Listener for the report crime button
        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));
                startActivity(i);
            }
        });


        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);


        pickContact.addCategory(Intent.CATEGORY_HOME);

        // get a reference for the suspect button
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);

        // add a listener for the suspect button
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start the activity, and request a result.
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });


        if(mCrime.getSuspect() != null)
            mSuspectButton.setText(mCrime.getSuspect());

        PackageManager pm = getActivity().getPackageManager();

        if(pm.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null)
            mSuspectButton.setEnabled(false);

        // Get the references for the widgets for handling the photo and camera:
        mPhotoButton = (ImageButton)v.findViewById(R.id.crime_camera);
        mPhotoView = (ImageView)v.findViewById(R.id.crime_photo);

        mPhotoView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(mPhotoFile!= null && mPhotoFile.exists())
                {
                    FragmentManager fragmentManager = getFragmentManager();
                    ImageFragment.newInstance(mPhotoFile).show(fragmentManager, "IMAGE_DIALOG");
                }
            }
        });




        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null &&  captureImage.resolveActivity(pm) != null;


        mPhotoButton.setEnabled(canTakePhoto);
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Uri uri = FileProvider.getUriForFile(getActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",
                        mPhotoFile);
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);

                List<ResolveInfo> cameraActivities =
                        getActivity().getPackageManager().queryIntentActivities(
                                captureImage, PackageManager.MATCH_DEFAULT_ONLY);
                for(ResolveInfo activity : cameraActivities){
                    getActivity().grantUriPermission(activity.activityInfo.packageName,
                            uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
                // start the activity to take a picture
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        updatePhotoView();
        return v;
    }

    // creates an instance of CrimeFragment with arguments
    public static CrimeFragment newInstance(UUID crimeId){
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // helper method to generate the report
    private String getCrimeReport() {
        String solvedString = null;

        // each argument after the first in getString(...) replaces a place holder.
        if(mCrime.isSolved())
            solvedString = getString(R.string.crime_report_solved);
        else
            solvedString = getString(R.string.crime_report_unsolved);

        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if(suspect == null)
            suspect = getString(R.string.crime_report_no_suspect);
        else
            suspect = getString(R.string.crime_report_suspect, suspect);

        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);

        return report;
    }

    // helper method to load the picture into the ImageView widget
    private void updatePhotoView() {
        // if the picture doesn't exist, clears the ImageView widget so
        // you don't see anything.
        if(mPhotoFile == null || !mPhotoFile.exists())
            mPhotoView.setImageDrawable(null);
        // otherwise, sets the widget with the picture.
        else {
            Bitmap bm = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), getActivity());
            mPhotoView.setImageBitmap(bm);
        }
    }
}




