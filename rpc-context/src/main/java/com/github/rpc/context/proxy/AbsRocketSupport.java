package com.github.rpc.context.proxy;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.rpc.context.bean.RocketRequest;
import com.github.rpc.context.exception.RocketException;
import com.github.rpc.context.netty.client.NettyClient;
import com.github.rpc.context.netty.helper.TaskThreadPoolHelper;
import com.github.rpc.context.protocol.ProtocolFactory;
import com.github.rpc.context.protocol.RocketHttpProtocol;
import com.github.rpc.context.protocol.RocketNettyProtocol;
import com.github.rpc.context.spring.annotation.RocketReferenceAttribute;
import com.github.rpc.context.util.RocketContext;
import com.github.rpc.context.bean.RocketResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author jianlei.shi
 * @date 2021/3/15 3:32 下午
 * @description AbsRocketSuuport
 */
@Slf4j
public abstract class AbsRocketSupport implements InvocationHandler {


    protected Object doInvoke(RocketReferenceAttribute referenceAttribute, Method method, Object[] args) {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        String version = referenceAttribute.getVersion();
        try {
            RocketRequest request = getRequest(method, args, referenceAttribute);
            Object protocol = ProtocolFactory.getProtocol(referenceAttribute.getProtocol());
            if (protocol instanceof RocketNettyProtocol) {
                final NettyClient nettyClient = doHandleTcp(protocol, request);
                final RocketResponse response =nettyClient.take();
                log.info("-->>>>>>>>response result is:{}", response.getResult());
                return response.getResult();
            } else if (protocol instanceof RocketHttpProtocol) {
                //todo
                return null;

            }
        } catch (Exception e) {
            log.error("远程调用服务类:[" + className + "]" + "-方法[" + methodName + "]异常!异常信息是:", e);
            throw new RocketException("远程调用服务类:[" + className + "]" + "-方法[" + methodName + "]异常!");
        }
        return null;
    }

    private NettyClient doHandleTcp(Object protocol, RocketRequest request) {
        RocketNettyProtocol nettyProtocol = (RocketNettyProtocol) protocol;
        NettyClient nettyClient = nettyProtocol.export();
        final ExecutorService executor = TaskThreadPoolHelper.getExecutor();
        setAttachments(request);
        log.info("#########client get  Attachment is:{}", JSONObject.toJSONString(request.getRpcAttachments() == null ? "a" : request.getRpcAttachments()));
        executor.execute(()->{
            log.info("netty connect thread info:{}",Thread.currentThread().getName());
            nettyClient.connect(request,nettyClient);
            if (log.isDebugEnabled()) {
                log.debug("remote request params is:{} ", JSONObject.toJSONString(request));
            }
        });
        TaskThreadPoolHelper.addExecutor(executor); //GC thread pool
        return nettyClient;
    }

    private void setAttachments(RocketRequest request) {
        final Map<String, String> attachments = RocketContext.getContext().getAttachments();
        if (CollectionUtil.isNotEmpty(attachments)){
            request.setAttachments(attachments);
        }
    }

    private Object doHandleHttp(RocketRequest request, Object protocol) {


        return null;
    }

    private RocketRequest getRequest(Method method, Object[] args, RocketReferenceAttribute referenceAttribute){
            RocketRequest request = new RocketRequest();
            request.setRequestId(UUID.randomUUID().toString());
            request.setClassName(method.getDeclaringClass().getName());
            request.setMethodName(method.getName());
            request.setParameters(args);
            request.setParameterTypes(method.getParameterTypes());
            request.setVersion(referenceAttribute.getVersion());
            return request;

        }
    }