package com.sjl.rpc.context.netty.client;

import com.sjl.rpc.context.bean.RocketRequest;
import com.sjl.rpc.context.bean.RocketResponse;
import com.sjl.rpc.context.netty.abs.BaseClientTransporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: jianlei
 * @date: 2019/12/1
 * @description: NettyClient
 */
@Component
@Slf4j
public class NettyClient extends BaseClientTransporter {


  public static RocketResponse start(RocketRequest request) {
    return new NettyClient().connect(request);
  }

}
