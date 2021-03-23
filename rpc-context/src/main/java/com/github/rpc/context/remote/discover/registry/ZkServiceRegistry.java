package com.github.rpc.context.remote.discover.registry;

import com.github.rpc.context.constants.Constant;
import com.github.rpc.context.exception.RocketException;
import com.github.rpc.context.remote.discover.abs.BaseRpcHandler;
import com.github.rpc.context.spring.annotation.RocketService;
import com.github.rpc.context.remote.client.CuratorClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author: JianLei
 * @date: 2020/9/6 2:28 下午
 * @description: zk服务注册
 */
@Slf4j
@Component
@DependsOn("springBeanUtil")
public class ZkServiceRegistry extends BaseRpcHandler
        implements EnvironmentAware, DisposableBean {
    private ConfigurableEnvironment env;


    @Override
    public void registry(Class<?> interfaces, Object service, String version, Boolean isService) {
        this.isService = isService;
        // 预留接口
        if (!isService) {
            //registry consumer
            registryConsumer(interfaces, version);
        } else {
            //provider registry
            registryProvider(interfaces,service, version);
        }

    }

    private void registryProvider(Class<?> interfaces, Object service, String version) {
        try {
            final RocketService sjlRpcService = service.getClass().getAnnotation(RocketService.class);
            final String v = sjlRpcService.version();
            //TODO 多协议
            String protocol = getProtocol();
            super.version = v;
            final Class<?> infaces = sjlRpcService.value();
            boolean flag = super.isExistServiceNode(infaces.getName());
            if (!flag) {
                super.createServicePath(CuratorClient.instance(), super.handleCacheMapServiceName(infaces.getName()));
            }

        } catch (Exception e) {
            log.error("====>发布服务出现异常", e);
            throw new RocketException("发布服务出现异常");
        }
    }

    private void registryConsumer(Class<?> interfaces, String version) {
        try {
            //consumer registry
            if (interfaces != null && !isService) {
                this.version = version;
                boolean flag = super.isExistServiceNode(interfaces.getName());
                if (!flag) {
                    super.createServicePath(
                            CuratorClient.instance(), super.handleCacheMapServiceName(interfaces.getName()));
                }
            }
        } catch (Exception e) {
            log.error(interfaces.getName() + "registry error!,current service is service [ " + isService + " ] " + "service name: " + interfaces.getName() + "version is:" + version, e);
            throw new RuntimeException(interfaces.getName() + "registry error");
        }
    }

    private String getProtocol() {
        final String proto = env.getProperty(Constant.PROTOCOL);
        if (StringUtils.isBlank(proto)) {
            throw new RocketException("protocol is null");
        }
        return proto;
    }

    @Override
    public void destroy() {
        log.info("==============开始销毁缓存map,curator关闭!================");
        cacheServiceMap.clear();
        CuratorClient.instance().close();

    }


    @Override
    protected ConfigurableEnvironment getEnvironment() {
        return this.env;
    }

    @Override
    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment) {
            this.env = (ConfigurableEnvironment) environment;
        }
    }
}