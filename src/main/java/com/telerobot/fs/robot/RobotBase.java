package com.telerobot.fs.robot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.entity.bo.RobotInteractiveParam;
import com.telerobot.fs.entity.dto.LlmAiphoneRes;
import com.telerobot.fs.entity.pojo.LlmToolRequest;
import com.telerobot.fs.entity.pojo.SpeechResultEntity;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.FileUtils;
import com.telerobot.fs.utils.ThreadPoolCreator;
import com.telerobot.fs.utils.ThreadUtil;
import link.thingscloud.freeswitch.esl.EslConnectionDetail;
import link.thingscloud.freeswitch.esl.EslConnectionPool;
import link.thingscloud.freeswitch.esl.EslConnectionUtil;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public abstract class RobotBase implements IEslEventListener {

    protected final static Logger logger = LoggerFactory.getLogger(RobotBase.class);
    public static  final String  ASR_TYPE_MRCP = "mrcp";
    public static  final String  ASR_TYPE_WEBSOCKET = "websocket";
    public static final  String NO_VOICE = "NO_VOICE";

    /**
     * 大模型底座相关的三个参数请在数据库中cc_params表中设置
     */
    private static final String API_KEY = SystemConfig.getValue("model-api-key");
    private static  final  String BASE_URL = SystemConfig.getValue("model-url");
    private static final String MODEL_NAME =  SystemConfig.getValue("model-name");


    /**
     *  交互轮次;
     */
    protected volatile LongAdder talkRound = new LongAdder();


    /**
     * 当前机器人数量； AtomicInteger 在高并发下导致cpu飙高;
     */
    protected  static LongAdder robotCounter = new LongAdder();

    private String currentAsrType;

    /**
     * 通话已经转人工处理
     */
    protected volatile boolean transferToAgent = false;

    /**
     *  动态切换语音识别方式；
     */
    public  String getAsrModelType(){
        return this.currentAsrType;
    }

    public void setAsrModelType(String asrType){
        this.currentAsrType = asrType;
    }

    /**
     * 接收到了挂机信号
     */
    protected volatile boolean recvHangupSignal = false;

    /**
     * 收到了播放完毕事件
     */
    protected volatile boolean recvPlayBackEndEvent = false;

    /**
     *  tts语音播放开始时间
     */
    protected volatile long playbackStartTime;

    /**
     *  tts语音播放完毕时间
     */
    protected volatile long playbackEndTime;

    /**
     *  检测到讲话开始事件之后，最大等待语音识别结果的超时时长;
     */
    protected static long maxWaitTimeMills = 0L;

    protected static boolean asrPauseEnabled;

    /**
     *  faq问题手册内容
     */
    protected static String faq;

    /**
     *  大模型提示词内容
     */
    protected static String llmTips;

    static {
        maxWaitTimeMills = 1000 * Long.parseLong(SystemConfig.getValue("max-wait-time-after-vad-start", "11"));
        asrPauseEnabled = Boolean.parseBoolean(SystemConfig.getValue("asr-pause-enabled", "true"));

        String modelFaqDir = SystemConfig.getValue("model-faq-dir", "/home/call-center/kb/");
        String modelFaqPath = modelFaqDir + "faq.txt";
        faq = FileUtils.ReadFile(modelFaqPath, "utf-8");
        if(StringUtils.isEmpty(faq)){
            logger.error("cant not read file model-faq-path = {} , check whether the file exists!", modelFaqPath);
            System.exit(0);
        }
        logger.info("****** faq file content: {}", faq);

        String llmTipsPath = modelFaqDir + "llmTips.txt";
        llmTips = FileUtils.ReadFile(llmTipsPath, "utf-8");
        if(StringUtils.isEmpty(llmTips)){
            logger.error("cant not read file  llmTipsPath = {} ，check whether the file exists!", llmTipsPath);
            System.exit(0);
        }
        logger.info("****** llmTips content: {}", llmTips);
    }

    /**
     * 被叫（客户）讲话次数;
     */
    protected volatile AtomicInteger calleeSpeakNumber = new AtomicInteger(0);

    /**
     *  记录对话交互轮次;
     */
    protected volatile AtomicInteger interactRounds = new AtomicInteger(0);


    /**
     *  通话记录信息
     */
    protected volatile InboundDetail callDetail;


    protected volatile RobotInteractiveParam interactiveParam = new RobotInteractiveParam();


    /**
     * 所有通话对象; 用于通话异常管理
     */
    protected static ConcurrentHashMap<String, RobotBase> callTaskList = new ConcurrentHashMap<>(500);

    protected volatile long lastTalkTime = System.currentTimeMillis();

    /**
     * 丢弃asr识别结果次数;
     */
    protected volatile AtomicInteger dropAsrCounter = new AtomicInteger(0);

    /**
     *  当前通道是否已经释放;
     */
    protected volatile boolean isReleased = false;

    /**
     * 当前通话的uuid
     */
    protected volatile String uuid = "";

    /**
     * 电话是否已经挂机
     **/
    protected  volatile  boolean isHangup = false;


    public boolean getHangup() {
        return isHangup;
    }

    public String getTraceId(){
        return uuid;
    }

    /**
     *  当前通话的录音文件名称
     */
    protected String  recordingsFileName = "";

    /**
     *  保存全量的语音识别结果;  机器话术播放中，机器话术播放完毕后； (仅限pcap识别方式有效)
     */
    protected ArrayBlockingQueue<String> asrResultEx = new ArrayBlockingQueue<>(10);

    /**
     * 客户是否抢话机器人；[机器话术播放中，客户讲话]
     */
    protected volatile boolean interruptRobot = false;

    /**
     * 已经发生了打断事件
     */
    protected volatile boolean interruptRobotHappened = false;

    public void setRecordingsFileName(String recordingsFileName) {
        this.recordingsFileName = recordingsFileName;
    }

    protected boolean vadWaitEnabled  = Boolean.parseBoolean(SystemConfig.getValue("vad-intelligent-wait", "true"));

    /**
     *  客户打断机器人讲话后，继续等待的秒数，判断客户是否有继续讲话，如果呀有则继续等待
     */
    protected int interruptWaitMills = Integer.parseInt(SystemConfig.getValue("vad-intelligent-wait-ms", "600"));

    /**
     *  计算在6秒后首次收到vad事件后，需要继续等待多久，以便收录完整的客户讲话内容;
     * @return
     */
    protected long calcWaitSecsDuration6Secs(){
        if(!vadWaitEnabled){
            return 0L;
        }
        if(recvPlayBackEndEvent){
            return 0L;
        }

        long secsPassedIn6SECS = System.currentTimeMillis() - playbackEndTime;
        long waitMills = 100;
        if(secsPassedIn6SECS <= 6000 && secsPassedIn6SECS > 5000){
            waitMills = interruptRobot ? 301 : 200;
        }
        if(secsPassedIn6SECS <= 5000 && secsPassedIn6SECS > 4000){
            waitMills =  interruptRobot ? 800 : 400;
        }
        if(secsPassedIn6SECS <= 4000 && secsPassedIn6SECS > 3000){
            waitMills = interruptRobot ? 1305 : 1000;
        }
        if(secsPassedIn6SECS <= 3000 && secsPassedIn6SECS > 2000){
            waitMills = interruptRobot ? 1500 : 1101;
        }
        if(secsPassedIn6SECS <= 2000 && secsPassedIn6SECS > 1000){
            waitMills = interruptRobot ? 2001 : 1800;;
        }
        if(secsPassedIn6SECS <= 1000 && secsPassedIn6SECS > 500){
            waitMills = interruptRobot ? 2005 : 2001;
        }
        if(secsPassedIn6SECS <= 500) {
            waitMills = 5500L;
        }
        return waitMills;
    }

    private static final OkHttpClient client =  new OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    private int ttsTextLength = 0;
    private ArrayBlockingQueue<String> ttsTextCache = new ArrayBlockingQueue<String>(2000);
    private final  String[] pauseFlags = new String[]{
            "？", "?",
            "，", ",",
            "；", ";",
            "。", ".",
            "、",
            "！", "!",
            "：", ":"
    };
    private boolean checkPauseFlag(String input){
        String lastChar = input.substring(input.length() - 1);
        for (String flag : pauseFlags) {
            if(flag.equalsIgnoreCase(lastChar)){
                return true;
            }
        }
        return false;
    }
    private void sendToTts() {
        StringBuilder tmpText = new StringBuilder("");
        while (ttsTextCache.peek() != null) {
            tmpText.append(ttsTextCache.poll());
        }
        String text = tmpText.toString();
        sendTtsRequest(text);
        ttsTextLength = 0;
    }

    List<JSONObject> llmRoundMessages = new ArrayList<>();
    private volatile boolean firstRound = true;
    protected String getDialogueContent(){
        List<JSONObject> chatMsg = new ArrayList<>();
        if(llmRoundMessages.size() > 3){
            for (int i = 3; i < llmRoundMessages.size(); i++) {
                chatMsg.add(llmRoundMessages.get(i));
            }
        }
        return JSON.toJSONString(chatMsg);
    }

    protected LlmAiphoneRes  talkWithLargeModel(String question) throws IOException {
        LlmAiphoneRes aiphoneRes = new  LlmAiphoneRes();
        aiphoneRes.setStatus_code(1);
        aiphoneRes.setClose_phone(0);
        aiphoneRes.setIfcan_interrupt(0);
        if(firstRound) {
            firstRound = false;
            JSONObject userMessage0 = new JSONObject();
            userMessage0.put("role", "system");
            String tips = llmTips + "\n" + faq;
            userMessage0.put("content", tips);
            llmRoundMessages.add(userMessage0);

            JSONObject userMessage1 = new JSONObject();
            userMessage1.put("role", "system");
            userMessage1.put("content", "电话已经接通，请播报开场白，不超过30个字。");
            llmRoundMessages.add(userMessage1);
        }else{
            JSONObject userMessage1 = new JSONObject();
            userMessage1.put("role", "user");
            userMessage1.put("content", question);
            llmRoundMessages.add(userMessage1);
        }

        JSONObject response = sendStreamingRequest(aiphoneRes, llmRoundMessages);
        llmRoundMessages.add(response);
        return aiphoneRes;
    }


    private  JSONObject sendStreamingRequest(LlmAiphoneRes aiphoneRes, List<JSONObject> messages) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("stream", true);
        // enable stream output

        JSONArray messagesArray = new JSONArray();
        messagesArray.addAll(messages);
        requestBody.put("messages", messagesArray);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toJSONString()
        );

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        boolean recvData = false;
        boolean jsonFormat = false;
        long startTime = System.currentTimeMillis();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Model api error: http-code={}, msg={}, url={}",
                        response.code(),
                        response.message(),
                        BASE_URL
                );
                throw new IOException("Unexpected code " + response);
            }

            BufferedSource source = response.body().source();
            StringBuilder responseBuilder = new StringBuilder();

            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null && line.startsWith("data: ")) {
                    String jsonData = line.substring(6).trim(); // 去掉 "data: " 前缀
                    if (jsonData.equals("[DONE]")) {
                        break; // 流式响应结束
                    }

                    JSONObject jsonResponse = JSON.parseObject(jsonData);
                    JSONObject message = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta"); // 注意：流式响应中消息在 "delta" 字段中

                    if (message.containsKey("content")) {
                        String speechContent = message.getString("content");

                        if (!recvData) {
                            recvData = true;
                            long costTime = (System.currentTimeMillis() - startTime);
                            logger.info("http request cost time : {} ms.", costTime);
                            aiphoneRes.setCostTime(costTime);
                        }

                        if (!StringUtils.isEmpty(speechContent)) {
                            //  send to tts server
                            String tmpText = speechContent.trim().replace(" ", "").replace(" ", "");
                            if (tmpText.startsWith("{") && !jsonFormat) {
                                logger.info("{} json response detected.", getTraceId());
                                jsonFormat = true;
                                aiphoneRes.setJsonResponse(true);
                            }

                            if (!StringUtils.isEmpty(tmpText) && !jsonFormat) {
                                ttsTextCache.add(tmpText);
                                ttsTextLength += tmpText.length();
                                // 积攒足够的字数之后，才发送给tts，避免播放异常;
                                if (ttsTextLength >= 10 && checkPauseFlag(tmpText)) {
                                    sendToTts();
                                }
                            }
                            responseBuilder.append(tmpText);
                        }
                    }
                }
            }

            String answer = responseBuilder.toString();
            logger.info("{} recv llm response end flag. answer={}", this.uuid, answer);
            if(ttsTextLength > 0 && !jsonFormat){
                sendToTts();
            }

            JSONObject finalResponse = new JSONObject();
            finalResponse.put("role", "assistant");
            finalResponse.put("content", answer);
            aiphoneRes.setBody(answer);
            return finalResponse;
        }
    }

    /**
     *  tts通道已关闭
     */
    protected volatile boolean ttsChannelClosed = true;

    protected void breakSoundPlay(){
        EslConnectionUtil.sendSyncApiCommand("uuid_break", uuid + " all");
    }

    protected void sendTtsRequest(String text){
        if(StringUtils.isEmpty(text)){
            return;
        }
        String voiceSource =  SystemConfig.getValue("stream-tts-voice-source", "aliyuntts");
        String voiceCode =  SystemConfig.getValue("stream-tts-voice-name", "aixia");

        if(ttsChannelClosed) {
            breakSoundPlay();
            EslConnectionUtil.sendExecuteCommand("speak", String.format("%s|%s|%s", voiceSource, voiceCode, text), uuid);
            ttsChannelClosed = false;
            logger.info("{} sendTtsRequest speak tts text {}", getTraceId(), text);
        }else{
            EslConnectionUtil.sendExecuteCommand("aliyuntts_resume", text, uuid);
            logger.info("{} sendTtsRequest cosvtts_resume text {}", getTraceId(), text);
        }
    }

    /**
     *  当前esl连接池对象;
     */
    protected EslConnectionPool eslConnectionPool;

    protected  boolean getAllowInterrupt(){
        return interactiveParam.getAllowInterrupt() == 1;
    }

    /**
     * 父类构造方法;
     */
    public RobotBase(){}

    /**
     *  创建esl连接池
     * @param uuid
     * @param eslHost
     * @param eslPort
     */
    protected void getEslConnectionPool(String uuid, String eslHost, int eslPort){
        EslConnectionPool connectionPool = EslConnectionUtil.getEslConnectionPool(
                eslHost,
                eslPort
        );
        if(connectionPool == null){
            logger.error("{} cant not get esl connection pool: host:{}, port:{}, uuid:{}, wavFile: {}",
                    eslHost,
                    eslPort,
                    uuid,
                    getTraceId(),
                    this.recordingsFileName
            );
            return;
        }
        this.eslConnectionPool = connectionPool;
        connectionPool.getDefaultEslConn().addListener(uuid + "-ex", this);
        robotCounter.increment();
    }



    private final Object globalLocker = new Object();


    public void releaseThreadNum() {
        if (!isReleased) {
            synchronized (globalLocker) {
                if (!isReleased) {
                    isReleased = true;
                    robotCounter.decrement();
                }
            }
        }
        callTaskList.remove(this.uuid);
        logger.info("{} release the call session concurrency.", getTraceId());
    }


    /**
     *  发送挂机指令; 释放计数器; 关闭Esl连接;
     */
    public void hangupAndCloseConn(boolean...killcall){
        if(killcall.length == 0) {
            EslConnectionUtil.sendExecuteCommand(
                    "hangup",
                    "callCenter-mandatory-hangup",
                    this.uuid,
                    this.eslConnectionPool
            );
        }
        releaseThreadNum();
    }

    private final Object threadLocker = new Object();
    protected void acquire(long... waitTimeMills){
        try {
            synchronized (threadLocker) {
                if (waitTimeMills.length == 0) {
                    threadLocker.wait();
                } else {
                    threadLocker.wait(waitTimeMills[0]);
                }
            }
        } catch (Exception e) {
            logger.info(getTraceId() + " thread wait failed: " + e.toString());
        }
    }
    protected void releaseSignal(){
        try {
            synchronized (threadLocker) {
                threadLocker.notifyAll();
            }
        }catch (Exception e) {
            logger.info(getTraceId() + " thread notifyAll failed: " + e.toString());
        }
    }

    protected volatile boolean inIvrProcess = false;

    protected Semaphore dtmfSignal = new Semaphore(0);

    /**
     * 当前流程要求捕获的dtmf字符长度
     */
    protected volatile Integer dtmfLength = 0;

    /**
     * 当前流程已接收的dtmf字符串
     */
    protected volatile String dtmfReceivedCache = "";
    private final Object lockerForDtmf = new Object();
    protected void acquireDtmf(long waitTimeLong) {
        try {
            synchronized (lockerForDtmf) {
                lockerForDtmf.wait(waitTimeLong);
            }
        } catch (Exception e) {
            logger.info(getTraceId() + " thread wait failed: " + e.toString());
        }
        // 更新上次讲话时间，避免触发长时间不讲话超时事件，导致通话呗挂断;
        lastTalkTime = System.currentTimeMillis();
    }


    /**
     * 通知主线程结束dtmf按键等待;
     */
    protected void releaseDtmf(){
        try {
            synchronized (lockerForDtmf) {
                lockerForDtmf.notifyAll();
            }
        }catch (Exception e) {
            logger.info(getTraceId() + " thread notifyAll failed: " + e.toString());
        }
    }


    protected   static void startRobotStatThread() {
        logger.info("startRobotStatThread ...");
        robotMainThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(robotCounter.intValue() > 0) {
                        try {
                            // RedisUtils.set("sys_robot_used_count", String.valueOf(robotCounter.intValue()));
                            logger.info("Now robot call session counter is:{} ", robotCounter.intValue());
                        } catch (Exception e) {
                            logger.warn("update robot call session counter error: " + e.toString());
                        }
                    }
                    ThreadUtil.sleep(11000);
                }
            }
        });
        startMonitor();
    }

    /**
     *  开启监视线程; 如果一个通话1分钟还收到客户讲话识别结果，则强制结束它;
     *  （很有可能是未收到挂机信号，强制结束是为了避免ThreadNum泄漏，也为了避免电话一直占线;）
     */
    private static  void startMonitor() {
        robotMainThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                logger.info("Started the robot monitoring thread ...");

                while (true) {
                    try {
                        doMonitor();
                    } catch (Throwable e) {
                        logger.info("doMonitor thread exception: {}  {}  ", e.toString(),
                                CommonUtils.getStackTraceString(e.getStackTrace()));
                    }
                    ThreadUtil.sleep(9000);
                }
            }
        });
    }

	private static void doMonitor() {
        if (callTaskList.size() > 0) {
            logger.info("doMonitor thread, call session counter ：" + callTaskList.size());
        }
        Iterator<Map.Entry<String, RobotBase>> iterator = callTaskList.entrySet().iterator();
        int processCount = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, RobotBase> entry = iterator.next();
            RobotBase task = entry.getValue();
            Long timePassed = System.currentTimeMillis() - task.lastTalkTime;
            Long maxNoTalkTime = 1000 * Long.parseLong(SystemConfig.getValue("robot-max-no-speak-time", "120"));
            if (timePassed > maxNoTalkTime) {
                processCount++;
                iterator.remove();
                if (task.uuid.length() != 0) {
                    logger.info("{} The call is abnormal, The customer has not spoken for a long time and is about to end the call.", task.uuid);
                    task.hangupAndCloseConn();
                }
                task.processFsMsg(task.generateHangupEvent("doMonitor-Longtime-No-Speak"));
            }
        }
        if (processCount != 0) {
            logger.info("Number of abnormal calls forcibly ended: " + processCount);
        }
    }

    /**
     * 为异常情况生成挂机事件数据;
     * @return
     */
    protected   Map<String, String> generateHangupEvent(String hangupClause){
        Map<String, String> eslHeaders = new HashMap<>(5);
        eslHeaders.put("Event-Name", "CHANNEL_HANGUP");
        eslHeaders.put("Callee", this.callDetail.getCaller());
        eslHeaders.put("Unique-ID", this.uuid);
        eslHeaders.put("Hangup-Cause", hangupClause);
        return eslHeaders;
    }


    protected abstract void processFsMsg(Map<String, String> headers);

    public static ThreadPoolExecutor getRobotMainThreadPool(){
        return robotMainThreadPool;
    }

    private static ThreadPoolExecutor robotMainThreadPool = null;
    protected  static void startRobotMainThreadPool(int maxRobotNumber){
        int  threadNum = maxRobotNumber + (maxRobotNumber / 5);
        //额外提供现有设置并发数的20% 的余量
        robotMainThreadPool = ThreadPoolCreator.create(
                 threadNum,
                "robot_main_thread_",
                24L,
                threadNum + 5
        );
        logger.info("successfully create robot_main_thread_pool.");
    }

    /**
     *  Freeswitch esl消息回调线程池;
     */
    protected static ThreadPoolExecutor fsEsNotifyThreadPool = null;
    protected  static void startFsEsNotifyThreadPool(int maxRobotNumber){
        int threadNum = (maxRobotNumber / 4);
        if(threadNum < 10){
            threadNum = 10;
        }
        // 通知回调线程数量不需要那么多，提供机器人总数量的20%的线程数量即可;
        fsEsNotifyThreadPool = ThreadPoolCreator.create(
                threadNum,
                "fs_esl_notify_thread_",
                24L,
                maxRobotNumber * 5
        );
        // 这里需要注意，线程池的 queue size 要尽可能大些，避免任务被丢弃;  这里设置为机器人数量的5倍；
        logger.info("successfully create fs_es_notify_thread_pool.");
    }

    /**
     *  从xml中解析mrcp接口返回的语音识别结果;
     * @param xml
     * @return
     */
    protected static SpeechResultEntity getSpeechResult(String xml){
        SpeechResultEntity speechResult = new SpeechResultEntity("", "");
        SAXReader reader = new SAXReader();
        InputStream input = new ByteArrayInputStream(xml.getBytes());
        Document document = null;
        try {
            document = reader.read(input);
        } catch (Exception e) {
            logger.info("An exception occurred when parsing xml :" + e.toString());
            return  speechResult;
        }
        try {
            Element root = ((List<Element>) document.getRootElement().elements()).get(0);
            List<Element> childElements = root.elements();
            for (Element child : childElements) {
                if ("instance".equalsIgnoreCase(child.getName())) {
                    speechResult.setRequestId(child.attributeValue("requestId"));
                    Element resultNode = ((List<Element>) child.elements()).get(0);
                    String result = resultNode.getText();
                    if (null != speechResult) {
                        speechResult.setResult(result);
                    }
                }
            }
        }
        catch (Exception e){
            logger.warn("An exception occurred when parsing xml:" + e.toString());
        }finally {
            reader = null;
            document = null;
            try {
                input.close();
            } catch (IOException e) {
            }
        }
        return speechResult;
    }

    /**
     *  使用 mrcp 或者 websocket 方式启动语音识别流程
     */
    protected void startAsrProcess(String  asrType, boolean changeAsrType){
        if (asrType.equalsIgnoreCase(ASR_TYPE_WEBSOCKET)){
            EslConnectionUtil.sendExecuteCommand(
                    "start_asr",
                    "hello",
                    uuid,
                    this.eslConnectionPool
            );
        }
        if(changeAsrType){
            this.setAsrModelType(asrType);
            logger.info("{} current session robot_asr_type is changed: {} ", getTraceId(), asrType);
        }
    }

    /**
     *  启用mrcp实时语音检测;
     */
    protected void startMrcp(){
        String mrcpParams = SystemConfig.getValue("fs-asr-mrcp-param");
        if(StringUtils.isEmpty(mrcpParams)){
            logger.error("{} fs-asr-mrcp-param is null, cannot start asr process. {}", uuid);
            return;
        }
        logger.info("{} start mrcp detect_speech param: {}", uuid, mrcpParams);
        //启动语音检测;
        EslConnectionUtil.sendExecuteCommand("detect_speech",
                mrcpParams, uuid, EslConnectionUtil.getDefaultEslConnectionPool()
        );
    }



    protected void pauseAsr(){
        if(asrPauseEnabled) {
            logger.info("{} try to pause Funasr  ", this.uuid);
            EslConnectionUtil.sendExecuteCommand("pause_asr", "1", this.uuid, this.eslConnectionPool);
        }
    }

    protected void resumeAsr(){
        if(asrPauseEnabled) {
            logger.info("{} try to resume asr  ", this.uuid);
            EslConnectionUtil.sendExecuteCommand("pause_asr", "0", this.uuid, this.eslConnectionPool);
        }
    }
}
