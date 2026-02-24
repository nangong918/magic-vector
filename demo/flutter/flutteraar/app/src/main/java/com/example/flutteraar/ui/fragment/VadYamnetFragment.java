package com.example.flutteraar.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.demo.aarlib.vad.yamnet.YamnetVadBridge;
import com.example.flutteraar.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VadYamnetFragment extends Fragment {
    private static final int REQ_RECORD_AUDIO = 0x303;

    private TextView tvState;
    private Spinner spinnerSampleRate;
    private Spinner spinnerFrameSize;
    private Spinner spinnerMode;
    private Button btnToggle;
    private boolean running;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vad_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvState = view.findViewById(R.id.tvVadState);
        spinnerSampleRate = view.findViewById(R.id.sampleRateSpinner);
        spinnerFrameSize = view.findViewById(R.id.frameSampleRateSpinner);
        spinnerMode = view.findViewById(R.id.modeSpinner);
        btnToggle = view.findViewById(R.id.btnToggleVad);
        TextView title = view.findViewById(R.id.titleTextView);
        title.setText(R.string.vad_yamnet);

        bindSpinners();
        bindActions();
    }

    private void bindSpinners() {
        bindSpinner(spinnerSampleRate, YamnetVadBridge.getSampleRates());
        bindSpinner(spinnerFrameSize, YamnetVadBridge.getFrameSizes());
        bindSpinner(spinnerMode, YamnetVadBridge.getModes());
    }

    private void bindActions() {
        btnToggle.setOnClickListener(v -> {
            if (!running) {
                startWithPermissionCheck();
            } else {
                YamnetVadBridge.stop();
            }
        });
    }

    private void startWithPermissionCheck() {
        if (YamnetVadBridge.hasRecordPermission(requireContext())) {
            updateConfigAndStart();
            return;
        }
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
    }

    private void updateConfigAndStart() {
        YamnetVadBridge.updateConfig(
                selectedValue(spinnerSampleRate),
                selectedValue(spinnerFrameSize),
                selectedValue(spinnerMode)
        );
        YamnetVadBridge.start(requireContext());
    }

    private String selectedValue(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void bindSpinner(Spinner spinner, List<String> values) {
        List<String> safeValues = values == null ? new ArrayList<>() : values;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, safeValues);
        spinner.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        YamnetVadBridge.setEventListener(this::onVadEvent);
    }

    @Override
    public void onStop() {
        super.onStop();
        YamnetVadBridge.clearEventListener();
        YamnetVadBridge.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        YamnetVadBridge.release();
    }

    private void onVadEvent(Map<String, Object> payload) {
        if (getActivity() == null) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            String type = String.valueOf(payload.get("type"));
            String message = String.valueOf(payload.get("message"));
            tvState.setText(message);
            if ("state".equals(type)) {
                Object stateValue = payload.get("running");
                running = stateValue instanceof Boolean && (Boolean) stateValue;
                btnToggle.setText(running ? R.string.vad_stop : R.string.vad_start);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_RECORD_AUDIO) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            updateConfigAndStart();
        } else {
            tvState.setText(R.string.vad_permission_denied);
        }
    }
}
