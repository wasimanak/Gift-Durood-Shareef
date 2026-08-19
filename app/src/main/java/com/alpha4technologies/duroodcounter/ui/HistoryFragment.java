package com.alpha4technologies.duroodcounter.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alpha4technologies.duroodcounter.databinding.FragmentHistoryBinding;
import com.alpha4technologies.duroodcounter.viewmodel.HistoryViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private HistoryViewModel historyViewModel;
    private UserAdapter userAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        setupRecyclerView();
        setupSearchView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter();
        binding.rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLeaderboard.setAdapter(userAdapter);
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                historyViewModel.setSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                historyViewModel.setSearchQuery(newText);
                return true;
            }
        });
    }

    private void observeViewModel() {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);

        historyViewModel.getGlobalTodayTotalLiveData().observe(getViewLifecycleOwner(), todayTotal -> {
            long val = todayTotal != null ? todayTotal : 0L;
            binding.tvGlobalTodayTotal.setText(formatter.format(val));
        });

        historyViewModel.getGlobalOverallTotalLiveData().observe(getViewLifecycleOwner(), overallTotal -> {
            long val = overallTotal != null ? overallTotal : 0L;
            binding.tvGlobalOverallTotal.setText(formatter.format(val));
        });

        historyViewModel.getFilteredUsersLiveData().observe(getViewLifecycleOwner(), users -> {
            userAdapter.submitList(users);
            if (users == null || users.isEmpty()) {
                binding.tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                binding.tvEmptyState.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
