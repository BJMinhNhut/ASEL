package com.cs426.asel.ui.account;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class InfoViewModel extends ViewModel {
    private final MutableLiveData<String> fullName = new MutableLiveData<>();
    private final MutableLiveData<String> studentId = new MutableLiveData<>();
    private final MutableLiveData<String> school = new MutableLiveData<>();
    private final MutableLiveData<String> faculty = new MutableLiveData<>();
    private final MutableLiveData<String> degree = new MutableLiveData<>();
    private final MutableLiveData<String> birthdate = new MutableLiveData<>();
    private final MutableLiveData<String> avatar = new MutableLiveData<>(); // Avatar as Base64 encoded string

    public InfoViewModel() {
        Log.d("InfoViewModel", "InfoViewModel created!");
    }

    public void setFullName(String fullName) {
        this.fullName.setValue(fullName);
    }

    public LiveData<String> getFullName() {
        return fullName;
    }

    public void setStudentId(String studentId) {
        this.studentId.setValue(studentId);
    }

    public LiveData<String> getStudentId() {
        return studentId;
    }

    public void setSchool(String school) {
        this.school.setValue(school);
    }

    public LiveData<String> getSchool() {
        return school;
    }

    public void setFaculty(String faculty) {
        this.faculty.setValue(faculty);
    }

    public LiveData<String> getFaculty() {
        return faculty;
    }

    public void setDegree(String degree) {
        this.degree.setValue(degree);
    }

    public LiveData<String> getDegree() {
        return degree;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate.setValue(birthdate);
    }

    public LiveData<String> getBirthdate() {
        return birthdate;
    }

    public void setAvatar(String avatar) {
        this.avatar.setValue(avatar);
    }

    public LiveData<String> getAvatar() {
        return avatar;
    }
}
