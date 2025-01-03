package com.zh.sbbot.custom;

import com.mikuac.shiro.annotation.common.Order;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.AnnotationScanner;
import com.mikuac.shiro.model.HandlerMethod;
import com.zh.sbbot.constant.AdminMode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketSession;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 核心逻辑参考 {@link com.mikuac.shiro.core.BotFactory}
 * 将通过注解开发的功能分为两类：普通用户可用和管理员可用
 */
@Component
@RequiredArgsConstructor
public class AnnotationHandlerContainer {
    private final ApplicationContext applicationContext;
    private Set<Class<?>> shiroAnnotations = new LinkedHashSet<>();
    @Getter
    private MultiValueMap<Class<? extends Annotation>, HandlerMethod> annotationHandlerWithoutAdmin;
    @Getter
    private MultiValueMap<Class<? extends Annotation>, HandlerMethod> annotationHandlerWithGroupAdmin;
    @Getter
    private MultiValueMap<Class<? extends Annotation>, HandlerMethod> annotationHandlerWithGroupOwner;
    @Getter
    private MultiValueMap<Class<? extends Annotation>, HandlerMethod> annotationHandler;

    /**
     * 参考：{@link com.mikuac.shiro.core.BotFactory#createBot(long, WebSocketSession)} ()}
     * <p>
     * 将通过注解开发的功能分为两类：普通用户可用和管理员可用
     */
    @PostConstruct
    public void initAnnotationHandler() {
        // 获取 Spring 容器中所有指定类型的对象
        Map<String, Object> beans = new HashMap<>(applicationContext.getBeansWithAnnotation(Shiro.class));
        // 一键多值 注解为 Key 存放所有包含某个注解的方法
        annotationHandlerWithoutAdmin = new LinkedMultiValueMap<>();
        annotationHandlerWithGroupAdmin = new LinkedMultiValueMap<>();
        annotationHandlerWithGroupOwner = new LinkedMultiValueMap<>();
        annotationHandler = new LinkedMultiValueMap<>();
        beans.values().forEach(obj -> {
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(obj);
            Arrays.stream(targetClass.getMethods()).forEach(method -> {
                HandlerMethod handlerMethod = new HandlerMethod();
                handlerMethod.setMethod(method);
                handlerMethod.setType(targetClass);
                handlerMethod.setObject(obj);
                Arrays.stream(method.getDeclaredAnnotations()).forEach(annotation -> {
                    Set<Class<?>> as = getShiroAnnotations();
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    if (as.contains(annotationType)) {
                        annotationHandler.add(annotation.annotationType(), handlerMethod);
                        if (method.getDeclaredAnnotationsByType(Admin.class).length == 0) {
                            // 方法不包含Admin注解的功能群主、群管、普通用户可用
                            annotationHandlerWithoutAdmin.add(annotation.annotationType(), handlerMethod);
                            annotationHandlerWithGroupOwner.add(annotation.annotationType(), handlerMethod);
                            annotationHandlerWithGroupAdmin.add(annotation.annotationType(), handlerMethod);
                        } else {
                            AdminMode mode = method.getDeclaredAnnotationsByType(Admin.class)[0].mode();
                            switch (mode) {
                                case GROUP_ADMIN -> {
                                    annotationHandlerWithGroupAdmin.add(annotation.annotationType(), handlerMethod);
                                    annotationHandlerWithGroupOwner.add(annotation.annotationType(), handlerMethod);
                                }
                                case GROUP_OWNER ->
                                        annotationHandlerWithGroupOwner.add(annotation.annotationType(), handlerMethod);
                            }
                        }
                    }
                });
            });
        });
        this.sort(annotationHandlerWithoutAdmin);
        this.sort(annotationHandlerWithGroupOwner);
        this.sort(annotationHandlerWithGroupAdmin);
        this.sort(annotationHandler);
    }

    /**
     * copied from {@link com.mikuac.shiro.core.BotFactory}
     * 获取所有注解类
     *
     * @return 注解集合
     */
    private Set<Class<?>> getShiroAnnotations() {
        if (!shiroAnnotations.isEmpty()) {
            return shiroAnnotations;
        }
        shiroAnnotations = (new AnnotationScanner()).scan("com.mikuac.shiro.annotation");
        shiroAnnotations.addAll((new AnnotationScanner()).scan("com.zh.sbbot.custom.event.handler"));
        return shiroAnnotations;
    }

    /**
     * copied from {@link com.mikuac.shiro.core.BotFactory}
     * 优先级排序
     *
     * @param annotationHandler 处理方法集合Map
     */
    private void sort(MultiValueMap<Class<? extends Annotation>, HandlerMethod> annotationHandler) {
        if (annotationHandler.isEmpty()) {
            return;
        }
        // 排序
        annotationHandler.keySet().forEach(annotation -> {
            List<HandlerMethod> handlers = annotationHandler.get(annotation);
            handlers = handlers.stream().sorted(Comparator.comparing(handlerMethod -> {
                Order order = handlerMethod.getMethod().getAnnotation(Order.class);
                return Optional.ofNullable(order == null ? null : order.value()).orElse(Integer.MAX_VALUE);
            })).toList();
            annotationHandler.put(annotation, handlers);
        });
    }
}
