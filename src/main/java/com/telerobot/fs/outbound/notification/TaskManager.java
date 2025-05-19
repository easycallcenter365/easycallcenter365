package com.telerobot.fs.outbound.notification;

import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dao.CallVoiceNotification;
import com.telerobot.fs.service.VoiceNotificationService;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.RandomUtils;
import com.telerobot.fs.utils.ThreadPoolCreator;
import com.telerobot.fs.utils.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 自动外呼状态监控
 * (一个任务批次启动一个BatchMonitor实例)
 */
@Component
public class TaskManager implements ApplicationListener<ApplicationReadyEvent> {
	private static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    private static int taskThreadNumber;
    /**
     *  外呼任务线程池;
     *  全局共享一个外呼线程池;
     */
    private static ThreadPoolExecutor callTaskThreadPool = null;

    private final Object currentBatchCurrencyLocker = new Object();


    static {
        taskThreadNumber = Integer.parseInt(
                SystemConfig.getValue("voice-notification-thread-num", "1")
        );
        log.info("voice notification, try to start thread pool, cpu-core-num={}, maximumPoolSize={}",
                Runtime.getRuntime().availableProcessors(),
                taskThreadNumber
        );
        callTaskThreadPool = ThreadPoolCreator.create(
                taskThreadNumber,
                "voice_notification_call_task_thread",
                24,
                taskThreadNumber *2
        );
    }

    /**
     *  当前任务已经使用的线路数
     */
    private  int threadNumUsed =0;

    /**
     * @return 当前任务已经使用的线路数
     */
    public int getThreadNumUsed() {
        return threadNumUsed;
    }
    /**
     *  当前任务批次已用线路数目加指定数目
     * @param addNum
     */
    private void addThreadNumUsed(int addNum) {
        synchronized(currentBatchCurrencyLocker){
            threadNumUsed += addNum;
        }
    }

    /**
     * 当前任务批次已用线路数目减1
     */
    private void releaseThreadNumUsed() {
        synchronized (currentBatchCurrencyLocker) {
            threadNumUsed -= 1;
        }
    }

    /**
     * 当前任务批次已用线路数目减指定数目
     * @param num
     */
    private void releaseThreadNumUsed(int num) {
        synchronized (currentBatchCurrencyLocker) {
            threadNumUsed -= num;
        }
    }

    private static TaskManager taskManager;
    public static TaskManager getInstance(){
        return taskManager;
    }
    private static boolean running = false;
	public static synchronized void startTask() {
        taskManager = AppContextProvider.getBean(TaskManager.class);
        int affectRow = taskManager.dbService.resetHistoryData();
        log.info("任务启动前重置外呼数据，影响行数: {}", affectRow);
	    if(!running) {
	        try {
	            new Thread(new Runnable() {
                    @Override
                    public void run() {
                        taskManager.readFromDb();
                    }
                }).start();
                taskManager.startLoop();
            }catch (Throwable e){
	            log.error("taskManager looper error! {} {}",
                        e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
            }
            running = true;
        }
	}

    /**
     * Data is read from the database every 11 seconds to obtain the list of
     * outbound call numbers that meet the conditions. retry outbound call for these phone numbers.
     */
	private void readFromDb(){
	    while (true) {
            List<CallVoiceNotification> list = taskManager.GetPhones();
            if(list != null) {
                taskManager.addCallTaskToQueue(list);
            }
            ThreadUtil.sleep(11000);
        }
    }

    private TaskManager() {
    }
    
    public void releaseThreadNum() {
        releaseThreadNumUsed();
        CallConfig.releaseLine_Used();
    }

    @Autowired
    private VoiceNotificationService dbService;

    private List<CallVoiceNotification> GetPhones() {
        List<CallVoiceNotification> resultList = new LinkedList<>();
        int counter = 1;
        int maxTry = 70;
        boolean errorOccured = false;
        while (counter <= maxTry) {
            try {
                resultList = dbService.selectRetryCallPhones();
                if(resultList.size() > 0){
                    for (CallVoiceNotification item : resultList) {
                        item.setCallstatus(1);
                    }
                    try {
                        dbService.updateBatch(resultList);
                    }catch (Throwable e){
                        log.error("set record status error! {} {}",
                                e.toString(), CommonUtils.getStackTraceString(e.getStackTrace())
                                );
                        return null;
                    }
                }
                if( resultList.size() > 0 ) {
                    log.info("{} 成功获取到需要重新外呼的号码资源: {} 条.", getTraceId(), resultList.size());
                }
                break;
            }catch (Throwable e){
                errorOccured = true;
                int sleepMills = RandomUtils.getRandomByRange(5000, 12000);
                log.error("{} 从数据库 cc_call_voice_notification 表中获取号码资源时发生错误！ {} , {} 秒后将重试, 原因： {}.",
                        getTraceId(),
                        e.toString(),
                        sleepMills,
                        CommonUtils.getStackTraceString(e.getStackTrace())
                );
                ThreadUtil.sleep(sleepMills);
            }
            counter++;
        }
        if(errorOccured && resultList.size() == 0 ){
            log.error("{} 连续 {} 次没有获取到外呼数据", getTraceId(), maxTry);
        }
        return resultList;
    }

    private void sleep(int mills) {
        ThreadUtil.sleep(mills);
    }

    private ArrayBlockingQueue<CallVoiceNotification> callTaskQueue = new ArrayBlockingQueue<>(30000, false);

    private Semaphore semaphore = new Semaphore(0);

    public boolean addCallTaskToQueue(List<CallVoiceNotification> taskList){
        if(taskList.size() > 0) {
            boolean success = callTaskQueue.addAll(taskList);
            if (success) {
                semaphore.release(taskList.size());
                log.info("addCallTaskToQueue successfully, taskList size = {}. ", taskList.size());
                return true;
            } else {
                log.error("addCallTaskToQueue failed, queue is full. ");
            }
        }
       return false;
    }

    /**
     * 启动外呼任务循环
     **/
    private void startLoop() throws InterruptedException {
    	log.info(getTraceId() + "启动外呼主线程.");
        while (true) {
            semaphore.acquire();

            if(this.callTaskQueue.size() == 0){
                continue;
            }

            if (!checkWhetherCanCall()) {
                log.info(getTraceId() + " 外呼总通道数目超过限制，即将暂时任务执行。");
                sleep(CallConfig.waitDelayWhenNoAvailableLine);
                semaphore.release();
                continue;
            }

            int speed = taskThreadNumber - getThreadNumUsed();

            log.info("{} 计算当前可用外呼数, speed={}, ThreadNum={}, current task usedLineNum={}, global maxCallConcurrency={}, MaxLineNumber_Used={}",
                    getTraceId(),
                    speed,
                    taskThreadNumber,
                    getThreadNumUsed(),
                    CallConfig.maxCallConcurrency,
                    CallConfig.getMaxLineNumber_Used()
            );

            synchronized (lockerGlobal) {
                int globalAvaiableLine = CallConfig.maxCallConcurrency - CallConfig.getMaxLineNumber_Used();
                if (speed > globalAvaiableLine) {
                    speed = globalAvaiableLine;
                }
                if (speed > 0) {
                    addThreadNumUsed(speed);
                    CallConfig.addMaxLineNumber_Used(speed);
                }
            }

            if (speed <= 0) {
                int sleepDelay = CallConfig.waitDelayWhenNoAvailableLine;
                log.info(getTraceId() +
                        " 计算出外呼数小于等于0，任务暂停" + sleepDelay + "秒。");
                sleep(sleepDelay);
                semaphore.release();
                continue;
            }

            log.info(getTraceId() + " 即将外呼并发数:{}.", speed);

            for (int i = 1; i <= speed; i++) {
                callOut();
                sleep(CallConfig.singleCallDelay);
            }
            sleep(2000);
        }
    }

    private static final Object lockerGlobal = new Object();

    /**
     * 检测已经使用的通道数目，判断是否可以继续外呼 
     **/
    private boolean checkWhetherCanCall()
    {
        int totalUsedNum = CallConfig.getMaxLineNumber_Used();
        int maxLineNum = CallConfig.maxCallConcurrency;
        if (totalUsedNum >= maxLineNum)
        {
            log.info("{} 已到达[系统]最大并发限制, 已使用外线数={}, 系统最大外线数={}", getTraceId(), totalUsedNum,  maxLineNum);
            return false;
        }
        int currentThreadNumUsed = getThreadNumUsed();
        if (currentThreadNumUsed >= taskThreadNumber)
        {
        	log.info("{} 已到达当前任务的最大并发限制, 已使用外线数={},允许使用最大外线数={}", getTraceId(), currentThreadNumUsed,  taskThreadNumber);
            return false;
        }
        return true;
    }

   
    /**
     * 单个外呼线程 
     **/
    private void callOut()
    {
        CallVoiceNotification phoneNumEntity = this.callTaskQueue.poll();
        if (phoneNumEntity == null)
        {
            releaseThreadNum();
        	log.info(getTraceId() + " 暂无可用号码");
            return;
        }

        try{
            callTaskThreadPool.execute(new CallTask(this, phoneNumEntity));
        }
        catch(Exception e)
        {
        	log.error(getTraceId() +" 外呼任务添加到线程池发生错误!" + e.toString());
        }
    }

    private String tasKTraceId = "voice_notification";
    private  String getTraceId(){
        return tasKTraceId;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ThreadUtil.sleep(3000);
                startTask();
            }
        }).start();
    }
}
