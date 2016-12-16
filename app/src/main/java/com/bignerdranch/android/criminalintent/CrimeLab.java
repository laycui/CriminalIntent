package com.bignerdranch.android.criminalintent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrimeLab {
  private static CrimeLab sCrimeLab;
  private List<Crime> mCrimes;

  static CrimeLab get() {
    if (sCrimeLab == null) {
      sCrimeLab = new CrimeLab();
    }
    return sCrimeLab;
  }

  private CrimeLab() {
    mCrimes = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      Crime crime = new Crime();
      crime.setTitle("Crime #" + i);
      crime.setSolved( i % 2 == 0);
      mCrimes.add(crime);
    }
  };

  List<Crime> getCrimes() {
    return mCrimes;
  }

  Crime getCrime(UUID id) {
    for (Crime crime : mCrimes) {
      if (crime.getId().equals(id)) {
        return crime;
      }
    }
    return null;
  }
}
