package com.example.cocktailviewer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cocktailviewer.MainActivity;
import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;
import com.example.cocktailviewer.data.YamlIO;
import com.google.android.material.button.MaterialButton;

public class YamlTextImportFragment extends Fragment {

    public static YamlTextImportFragment newInstance() {
        return new YamlTextImportFragment();
    }

    private RecipeRepository repo;

    private EditText etYamlInput;
    private MaterialButton btnYamlImport;
    private TextView tvResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_yaml_text_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        repo = ((MainActivity) requireActivity()).repo();

        etYamlInput = v.findViewById(R.id.etYamlInput);
        btnYamlImport = v.findViewById(R.id.btnYamlImport);
        tvResult = v.findViewById(R.id.tvResult);

        btnYamlImport.setOnClickListener(view -> {
            String text = etYamlInput.getText() == null ? "" : etYamlInput.getText().toString();

            try {
                YamlIO.ImportResult r = YamlIO.importFromTextSkipDuplicates(repo, text);

                String msg = "등록 결과\n"
                        + "성공: " + r.success + "\n"
                        + "중복 스킵: " + r.duplicate + "\n"
                        + "실패: " + r.failed;
                tvResult.setText(msg);

            } catch (Exception e) {
                tvResult.setText("등록 실패: " + e.getMessage());
            }
        });
    }
}