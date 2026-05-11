package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.glitch.R;
import com.example.glitch.ui.UiAnimations;

/**
 * Legacy alias fragment retained for backward navigation compatibility.
 */
public class StaffVehicleRequestFragment extends Fragment {
    private android.view.ViewGroup animContent;

    @NonNull
    public static StaffVehicleRequestFragment newInstance() {
        return new StaffVehicleRequestFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return new View(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animContent = view.findViewById(R.id.anim_content);
        if (isAdded() && requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).showFragment(SponsorVehicleRegistrationFragment.newInstance(), false);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (animContent != null) UiAnimations.animateFallIn(animContent);
    }
}
