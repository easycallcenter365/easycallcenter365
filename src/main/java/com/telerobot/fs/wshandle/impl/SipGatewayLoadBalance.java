package com.telerobot.fs.wshandle.impl;

import com.telerobot.fs.entity.dto.GatewayConfig;
import com.telerobot.fs.entity.dto.GatewayGroup;
import com.telerobot.fs.entity.dto.GatewayConfig;
import com.telerobot.fs.entity.dto.GatewayGroup;
import com.telerobot.fs.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 *  网关负载平衡控制;
 *
 *  记录所有网关的实时并发信息;
 *  一个项目可能有多个网关;
 *  项目中的员工可能有不同于项目配置的网关;
 *  不同项目不允许用同一个网关;
 *  每个网关有一个上次更新时间，根据这个字段来确定是否更新网关;
 *
 */
public class SipGatewayLoadBalance {

    protected static final Logger logger = LoggerFactory.getLogger(SipGatewayLoadBalance.class);

    private static final String DEFAULT_CALL_PROFILE = "external";

    /**
     * 网关组列表;
     *  key: 一个或多个网关uuid字符串的拼接;
     *  value: 网关列表；
     *  系统根据用户提交的参数动态创建网关组;
     */
    private static ConcurrentHashMap<String, GatewayGroup>  gatewayGroupList = new ConcurrentHashMap<>(50);


    /**
     *  把网关按照优先级排序：
     * @param gatewayConfigList
     */
    private static void sort(List<GatewayConfig> gatewayConfigList){
        gatewayConfigList.sort(new Comparator<GatewayConfig>() {
            @Override
            public int compare(GatewayConfig g1, GatewayConfig g2) {
                if(g1.getPriority().intValue() == g2.getPriority().intValue()){
                    return 0;
                }
                return g1.getPriority() > g2.getPriority() ? 1 : -1;
            }
        });
    }

    private static GatewayConfig getGatewayByUuid(String uuid, GatewayGroup gatewayGroup){
        for (GatewayConfig gatewayConfig : gatewayGroup.getGatewayList()) {
            if(uuid.equalsIgnoreCase(gatewayConfig.getUuid())){
                return gatewayConfig;
            }
        }
        return null;
    }

    /**
     *  检测网关数据是否改变;
     *    判断信息md5是否相一致;
     * @return
     */
    private static void checkGatewayChanged(GatewayGroup gatewayGroup, List<GatewayConfig> gatewayConfigList) {
        for (GatewayConfig gateway : gatewayConfigList) {
            GatewayConfig gw = getGatewayByUuid(gateway.getUuid(), gatewayGroup);
            if (null != gw) {
                if (!gw.getMd5Info().equalsIgnoreCase(gateway.getMd5Info())  &&
                     gw.getUpdateTime() < gateway.getUpdateTime()
                ) {
                    if (gatewayGroup.getSemaphore().tryAcquire()) {
                        logger.info("检测到网关信息改变，尝试重新加载最新配置...gateway uuid={}", gw.getUuid());
                        updateGroup(gatewayGroup, gatewayConfigList);
                        gatewayGroup.getSemaphore().release();
                    }
                }
            }
        }
    }

    private static GatewayGroup updateGroup(GatewayGroup  gatewayGroup, List<GatewayConfig> gatewayConfigList){
        List<GatewayConfig> gatewayList = new ArrayList<>();
        // 逐一设置每个网关的可用并发数;
        for (int i = 0; i < gatewayConfigList.size(); i++) {
            GatewayConfig config = gatewayConfigList.get(i);
            config.setAvailableConcurrency(new Semaphore(config.getConcurrency()));
            // 对接模式下，需要解析GatewayAddr中的callProfile参数;
            if (!config.getRegister()) {
                if (config.getGatewayAddr().contains(";")) {
                    String[] tmpArray = config.getGatewayAddr().split(";");
                    config.setGatewayAddr(tmpArray[0]);
                    config.setCallProfile(tmpArray[1]);
                }

                if(StringUtils.isNullOrEmpty(config.getCallProfile())) {
                    config.setCallProfile(DEFAULT_CALL_PROFILE);
                }
            }
            if ("g711".equalsIgnoreCase(config.getAudioCodec())) {
                config.setAudioCodec("pcma");
            }
            gatewayList.add(config);
        }
        logger.info("gateway list initialized ...");
        gatewayGroup.setGatewayList(gatewayList);
        gatewayGroup.setGroupId(genGatewayGroupId(gatewayList));
        return gatewayGroup;
    }

    private static GatewayGroup initGatewayGroup(String gatewayGroupId, List<GatewayConfig> gatewayConfigList) {
        GatewayGroup gatewayGroup = gatewayGroupList.get(gatewayGroupId);
        if(null == gatewayGroup){
            synchronized (gatewayGroupId.intern()){
                gatewayGroup = gatewayGroupList.get(gatewayGroupId);
                if(null == gatewayGroup){
                    gatewayGroup = new GatewayGroup();
                    gatewayGroupList.put(gatewayGroupId, updateGroup(gatewayGroup, gatewayConfigList));
                }
            }
        }
        return gatewayGroup;
    }

    private static List<GatewayConfig> filterGatewayList(GatewayGroup gatewayGroup, List<GatewayConfig> triedList){
        if(triedList == null || triedList.size() == 0){
            return gatewayGroup.getGatewayList();
        }
        List<GatewayConfig> candidateList = new ArrayList<>();
        for (int i = 0; i < gatewayGroup.getGatewayList().size(); i++) {
            if(!triedList.contains(gatewayGroup.getGatewayList().get(i))){
                candidateList.add(gatewayGroup.getGatewayList().get(i));
            }
        }
        return candidateList;
    }

    /**
     * 根据一个或多个 gateway 的 uuid 生成 gatewayGroup 的groupId
     * @return
     */
    public static String genGatewayGroupId(List<GatewayConfig> gatewayConfigList){
        StringBuilder groupId = new StringBuilder();
        for (int i = 0; i < gatewayConfigList.size(); i++) {
            groupId.append("_").append(gatewayConfigList.get(i).getUuid());
        }
        return groupId.toString();
    }

    /**
     *  获取一个可用的网关;
     *  获取策略： 根据优先级、网关负载情况
     * @param gatewayConfigList
     * @param triedList 已尝试的未成功的网关列表;
     * @return
     */
    public static GatewayConfig getGateway(List<GatewayConfig> gatewayConfigList, List<GatewayConfig> triedList){
        if(gatewayConfigList.size() > 1) {
            sort(gatewayConfigList);
        }
        String gatewayGroupId = genGatewayGroupId(gatewayConfigList);
        GatewayGroup gatewayGroup = initGatewayGroup(gatewayGroupId, gatewayConfigList);

        checkGatewayChanged(gatewayGroup, gatewayConfigList);

        List<GatewayConfig> availableGwList = filterGatewayList(gatewayGroup, triedList);
        for (int i = 0; i < availableGwList.size(); i++) {
            GatewayConfig gw = availableGwList.get(i);
            if(gw.getAvailableConcurrency().tryAcquire()){
                return gw;
            }
        }
        return null;
    }

    /**
     *  电话挂机后，归还网关，以便释放占用的并发数;
     * @param gatewayConfig
     */
    public static void releaseGateway(GatewayConfig gatewayConfig){
        if(null != gatewayConfig) {
            gatewayConfig.getAvailableConcurrency().release();
        }
    }

}
