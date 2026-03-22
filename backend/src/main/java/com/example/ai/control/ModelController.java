package com.example.ai.control;

import com.example.ai.config.ChatModelProperties;
import com.example.ai.pojo.LeeResult;
import com.example.ai.pojo.ModelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ModelController {

    private final ChatModelProperties chatModelProperties;

    @GetMapping("/models")
    public LeeResult<List<ModelOption>> listModels() {
        List<ModelOption> models = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (chatModelProperties.getPlatforms() != null) {
            for (ChatModelProperties.Platform platform : chatModelProperties.getPlatforms()) {
                if (platform.getOptions() == null) continue;
                for (ChatModelProperties.Platform.Options option : platform.getOptions()) {
                    String model = option.getModel();
                    if (model == null || model.isBlank() || seen.contains(model)) continue;
                    String platformName = platform.getName();
                    String label = (platformName == null || platformName.isBlank())
                            ? model
                            : model + " (" + platformName + ")";
                    models.add(new ModelOption(model, label, platformName));
                    seen.add(model);
                }
            }
        }
        return LeeResult.ok(models);
    }
}
