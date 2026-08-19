package com.alpha4technologies.duroodcounter.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.alpha4technologies.duroodcounter.data.DuroodRepository;
import com.alpha4technologies.duroodcounter.model.User;

import java.util.ArrayList;
import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    private final DuroodRepository repository;
    private final MediatorLiveData<List<User>> filteredUsersLiveData = new MediatorLiveData<>();
    private final MutableLiveData<String> searchQueryLiveData = new MutableLiveData<>("");

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = DuroodRepository.getInstance();

        LiveData<List<User>> allUsersLiveData = repository.getAllUsersLiveData();

        filteredUsersLiveData.addSource(allUsersLiveData, users -> filterList(users, searchQueryLiveData.getValue()));
        filteredUsersLiveData.addSource(searchQueryLiveData, query -> filterList(allUsersLiveData.getValue(), query));
    }

    public LiveData<List<User>> getFilteredUsersLiveData() {
        return filteredUsersLiveData;
    }

    public LiveData<Long> getGlobalTodayTotalLiveData() {
        return repository.getGlobalTodayTotalLiveData();
    }

    public LiveData<Long> getGlobalOverallTotalLiveData() {
        return repository.getGlobalOverallTotalLiveData();
    }

    public void setSearchQuery(String query) {
        searchQueryLiveData.setValue(query != null ? query : "");
    }

    private void filterList(List<User> originalList, String query) {
        if (originalList == null) {
            filteredUsersLiveData.setValue(new ArrayList<>());
            return;
        }

        if (query == null || query.trim().isEmpty()) {
            filteredUsersLiveData.setValue(originalList);
            return;
        }

        String q = query.trim().toLowerCase();
        List<User> filtered = new ArrayList<>();
        for (User user : originalList) {
            boolean usernameMatch = user.getUsername() != null && user.getUsername().toLowerCase().contains(q);
            boolean cityMatch = user.getCity() != null && user.getCity().toLowerCase().contains(q);
            boolean nameMatch = user.getFullName() != null && user.getFullName().toLowerCase().contains(q);

            if (usernameMatch || cityMatch || nameMatch) {
                filtered.add(user);
            }
        }
        filteredUsersLiveData.setValue(filtered);
    }
}
