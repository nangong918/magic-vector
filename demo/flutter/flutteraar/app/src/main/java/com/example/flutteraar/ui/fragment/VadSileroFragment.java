package com.example.flutteraar.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.demo.aarlib.vad.silero.SileroVadBridge;
import com.example.flutteraar.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VadSileroFragment extends Fragment {
    private static final int REQ_RECORD_AUDIO = 0x302;
    private static final String DEFAULT_SAMPLE_RATE = "SAMPLE_RATE_8K";
    private static final String DEFAULT_FRAME_SIZE = "FRAME_SIZE_256";
    private static final String DEFAULT_MODE = "NORMAL";

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
        title.setText(R.string.vad_silero);

        bindSpinners();
        bindActions();
    }

    private void bindSpinners() {
        bindSpinner(spinnerSampleRate, SileroVadBridge.getSampleRates());
        bindSpinner(spinnerMode, SileroVadBridge.getModes());
        setSpinnerSelection(spinnerSampleRate, DEFAULT_SAMPLE_RATE);
        setSpinnerSelection(spinnerMode, DEFAULT_MODE);

        refreshFrameSizeBySampleRate();
        setSpinnerSelection(spinnerFrameSize, DEFAULT_FRAME_SIZE);
        spinnerSampleRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshFrameSizeBySampleRate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void bindActions() {
        btnToggle.setOnClickListener(v -> {
            if (!running) {
                startWithPermissionCheck();
            } else {
                SileroVadBridge.stop();
            }
        });
    }

    private void startWithPermissionCheck() {
        if (SileroVadBridge.hasRecordPermission(requireContext())) {
            updateConfigAndStart();
            return;
        }
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
    }

    private void updateConfigAndStart() {
        SileroVadBridge.updateConfig(
                selectedValue(spinnerSampleRate),
                selectedValue(spinnerFrameSize),
                selectedValue(spinnerMode)
        );
        SileroVadBridge.start(requireContext());
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

    private void refreshFrameSizeBySampleRate() {
        String rate = selectedValue(spinnerSampleRate);
        String currentFrameSize = selectedValue(spinnerFrameSize);
        bindSpinner(spinnerFrameSize, SileroVadBridge.getFrameSizes(rate));
        if (currentFrameSize.isEmpty()) {
            setSpinnerSelection(spinnerFrameSize, DEFAULT_FRAME_SIZE);
        } else {
            setSpinnerSelection(spinnerFrameSize, currentFrameSize);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (spinner.getAdapter() == null || value == null || value.isEmpty()) {
            return;
        }
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            Object item = spinner.getAdapter().getItem(i);
            if (value.equals(String.valueOf(item))) {
                spinner.setSelection(i, false);
                return;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        SileroVadBridge.setEventListener(this::onVadEvent);
    }

    @Override
    public void onStop() {
        super.onStop();
        SileroVadBridge.clearEventListener();
        SileroVadBridge.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SileroVadBridge.release();
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
