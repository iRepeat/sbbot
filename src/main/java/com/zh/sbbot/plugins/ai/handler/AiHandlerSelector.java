package com.zh.sbbot.plugins.ai.handler;

import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AiHandlerSelector implements ApplicationContextAware {
    @Autowired
    private DictRepository dictRepository;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public AiHandler getAiService() {
        String value = dictRepository.getValue(DictKey.PLUGIN_AI_USE);

        return applicationContext.getBeansOfType(AiHandler.class).values()
                .stream()
                .filter(aiHandler -> aiHandler.vendor().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("不支持的AI: " + value));
    }
}
