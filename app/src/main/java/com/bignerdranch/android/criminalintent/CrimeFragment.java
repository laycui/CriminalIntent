package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {

  private static final String ARG_CRIME_ID = "crime_id";
  private static final String DIALOG_DATE = "DialogDate";
  private static final int REQUEST_DATE = 0;
  private static final int REQUEST_CONTACT = 1;
  private static final int REQUEST_PHOTO = 2;

  private Crime mCrime;
  private File mPhotoFile;
  private EditText mTitleField;
  private Button mDateButton;
  private CheckBox mSolvedCheckBox;
  private Button mReportButton;
  private Button mSuspectButton;
  private ImageButton mPhotoButton;
  private ImageView mPhotoView;
  private Callbacks mCallbacks;

  public static CrimeFragment newInstance(UUID crimeId) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_CRIME_ID, crimeId);

    CrimeFragment fragment = new CrimeFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
    mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
    mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mCallbacks = (Callbacks) activity;
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mCallbacks = null;
  }

  @Override
  public void onPause() {
    super.onPause();

    CrimeLab.get(getActivity()).updateCrime(mCrime);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
          savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_crime, container, false);

    mTitleField = (EditText) v.findViewById(R.id.crime_title);
    mTitleField.setText(mCrime.getTitle());
    mDateButton = (Button) v.findViewById(R.id.crime_date);
    mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
    mReportButton = (Button) v.findViewById(R.id.crime_report);
    mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
    mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
    mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);

    mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
      }
    });

    mTitleField.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        mCrime.setTitle(charSequence.toString());
        updateCrime();
      }

      @Override
      public void afterTextChanged(Editable editable) {
      }
    });

    updateDate();
    mDateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        FragmentManager manager = getFragmentManager();
        DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
        dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
        dialog.show(manager, DIALOG_DATE);
      }
    });

    mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        mCrime.setSolved(b);
        updateCrime();
      }
    });

    mReportButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
        i = Intent.createChooser(i, getString(R.string.send_report));
        startActivity(i);
      }
    });

    final Intent pickContact =
            new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    mSuspectButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        startActivityForResult(pickContact, REQUEST_CONTACT);
      }
    });

    if (mCrime.getSuspect() != null) {
      mSuspectButton.setText(mCrime.getSuspect());
    }

    PackageManager packageManager = getActivity().getPackageManager();
    if (packageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
      mSuspectButton.setEnabled(false);
    }

    final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    boolean canTakePhoto =
            mPhotoFile != null && captureImage.resolveActivity(packageManager) != null;
    mPhotoButton.setEnabled(canTakePhoto);

    if (canTakePhoto) {
      Uri uri = FileProvider.getUriForFile(getActivity(),
              getActivity().getApplicationContext().getPackageName() + ".provider", mPhotoFile);
      captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
    }

    mPhotoButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivityForResult(captureImage, REQUEST_PHOTO);
      }
    });

    updatePhotoView();
    return v;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }
    if (requestCode == REQUEST_DATE) {
      Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
      mCrime.setDate(date);
      updateCrime();
      updateDate();
    } else if (requestCode == REQUEST_CONTACT && data != null) {
      Uri contactUri = data.getData();
      String[] queryFields = new String[] {
              ContactsContract.Contacts.DISPLAY_NAME
      };
      Cursor c = getActivity()
              .getContentResolver().query(contactUri, queryFields, null, null, null);
      try {
        if (c.getCount() == 0) {
          return;
        }
        c.moveToFirst();
        String suspect = c.getString(0);
        mCrime.setSuspect(suspect);
        updateCrime();
        mSuspectButton.setText(suspect);
      } finally {
        c.close();
      }
    } else if (requestCode == REQUEST_PHOTO) {
      updateCrime();
      updatePhotoView();
    }
  }

  private void updateCrime() {
    CrimeLab.get(getActivity()).updateCrime(mCrime);
    mCallbacks.onCrimeUpdated(mCrime);
  }

  private void updateDate() {
    mDateButton.setText(mCrime.getDate().toString());
  }

  private String getCrimeReport() {
    String solvedString;
    if (mCrime.isSolved()) {
      solvedString = getString(R.string.crime_report_solved);
    } else {
      solvedString = getString(R.string.crime_report_unsolved);
    }

    String dateFormat = "EEE, MMM dd";
    String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

    String suspect = mCrime.getSuspect();
    if (suspect == null) {
      suspect = getString(R.string.crime_report_no_suspect);
    } else {
      suspect = getString(R.string.crime_report_suspect, suspect);
    }
    return getString(R.string.crime_report, mCrime.getTitle(), dateString, solvedString, suspect);
  }

  public void returnResult() {
    getActivity().setResult(Activity.RESULT_OK, null);
  }

  private void updatePhotoView() {
    if (mPhotoFile == null || !mPhotoFile.exists()) {
      mPhotoView.setImageDrawable(null);
    } else {
      Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), getActivity());
      mPhotoView.setImageBitmap(bitmap);
    }
  }

  public interface Callbacks {
    void onCrimeUpdated(Crime crime);
  }

}