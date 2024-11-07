package com.zh.sbbot.plugin.ai.handler;

import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.dao.PluginAiRepository;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiHandlerSelector implements ApplicationContextAware {
    private final PluginAiRepository pluginAiRepository;
    private final DictRepository dictRepository;
    private ApplicationContext applicationContext;
    private final ConcurrentHashMap<Long, AiHandler> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        pluginAiRepository.createTable();
        List<PluginAi> pluginAis = pluginAiRepository.getAll();
        pluginAis.forEach(pluginAi -> {
            AiHandler aiHandler = get(pluginAi.getVendor());
            handlerMap.put(pluginAi.getGroupId(), aiHandler);
        });
        log.info("init ai handler for group: {}", handlerMap);
    }

    public AiHandler get(String vendor) {
        return applicationContext.getBeansOfType(AiHandler.class).values()
                .stream()
                .filter(aiHandler -> aiHandler.vendor().equalsIgnoreCase(vendor))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("不支持的AI: " + vendor));
    }

    public AiHandler get(Long groupId) {
        return handlerMap.get(groupId);
    }

    public AiHandler getDefault() {
        String defaultAIVendor = dictRepository.get(DictKey.PLUGIN_AI_DEFAULT);
        return get(defaultAIVendor);
    }

    public AiHandler set(Long groupId, String vendor) {
        AiHandler newAiHandler = get(vendor);
        handlerMap.put(groupId, newAiHandler);
        return newAiHandler;
    }
}
