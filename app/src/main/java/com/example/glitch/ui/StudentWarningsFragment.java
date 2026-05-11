package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.InterventionRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.StudentWarning;
import com.example.glitch.model.UserProfile;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import com.example.glitch.ui.UiAnimations;

public class StudentWarningsFragment extends Fragment {
    private android.view.ViewGroup animContent;
    private InterventionRepository interventionRepository;
    private StudentWarningAdapter adapter;
    private TextView textEmpty;

    @NonNull
    public static StudentWarningsFragment newInstance() {
        return new StudentWarningsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_warnings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animContent = view.findViewById(R.id.anim_content);
        interventionRepository = RepositoryProvider.getInterventionRepository();
        textEmpty = view.findViewById(R.id.text_warnings_empty);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_warnings);
        adapter = new StudentWarningAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.STUDENT_WARNINGS);

        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) return;

        interventionRepository.removeListeners();

        interventionRepository.listenWarningsByStudent(profile.getUid(), new InterventionRepository.WarningListListener() {
            @Override
            public void onData(@NonNull List<StudentWarning> warnings) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(warnings);
                    textEmpty.setVisibility(warnings.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), "Failed to load warnings.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (interventionRepository != null) interventionRepository.removeListeners();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (animContent != null) UiAnimations.animateFallIn(animContent);
    }
}
