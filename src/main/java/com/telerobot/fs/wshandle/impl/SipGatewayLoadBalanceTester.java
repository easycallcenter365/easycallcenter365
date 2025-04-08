package com.telerobot.fs.wshandle.impl;

import com.alibaba.fastjson.JSON;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.dto.GatewayConfig;
import com.telerobot.fs.utils.RandomUtils;
import com.telerobot.fs.utils.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SipGatewayLoadBalanceTester {

    protected static final Logger logger = LoggerFactory.getLogger(SipGatewayLoadBalanceTester.class);

    private static final ArrayList<GatewayConfig> gatewayList = new ArrayList<>(20);

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            gatewayList.add(new GatewayConfig(
                    System.currentTimeMillis(),
                    "external_" + i,
                    UuidGenerator.GetOneUuid(),
                    "192.168.66." + RandomUtils.getRandomByRange(100, 228) + ":" + RandomUtils.getRandomByRange(5010, 5080),
                    "00" + i,
                    "" + i,
                    i,
                    RandomUtils.getRandomByRange(5, 8),
                    false,
                    "g711"
            ));
        }

        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<GatewayConfig> gateways = new ArrayList<>(10);
                    // 通过随机数得出待测试的网关数量;
                    int gatewaysCount = RandomUtils.getRandomByRange(1, 3);
                    ThreadUtil.sleep(1000);
                    for (int i = 0; i < gatewaysCount; i++) {
                        // 通过随机数得出随机网关的index
                        int index = RandomUtils.getRandomByRange(0, 9);
                        gateways.add(gatewayList.get(index));
                        ThreadUtil.sleep(1000);
                    }

                    String groupId = SipGatewayLoadBalance.genGatewayGroupId(gateways);

                    while (true) {
                        GatewayConfig gatewayConfig = SipGatewayLoadBalance.getGateway(gateways, null);
                        if (null == gatewayConfig) {
                            logger.error("{} 没有获取到可用网关", groupId);
                            ThreadUtil.sleep(7000);
                        } else {
                            long talkTimeLong = RandomUtils.getRandomByRange(10, 30);
                            logger.info("{} call time-long = {},  gateway info = {}", groupId, talkTimeLong, JSON.toJSONString(gatewayConfig));
                            ThreadUtil.sleep((int) talkTimeLong * 1000);
                            SipGatewayLoadBalance.releaseGateway(gatewayConfig);
                        }

                    }
                }
            }).start();

        }
    }
}


