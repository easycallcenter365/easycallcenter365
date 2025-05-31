package com.telerobot.fs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.telerobot.fs.config.UuidGenerator;
import com.telerobot.fs.entity.dao.CallVoiceNotification;
import com.telerobot.fs.outbound.notification.TaskManager;
import com.telerobot.fs.service.VoiceNotificationService;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.wshandle.MessageResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/VoiceNotification")
public class VoiceNotificationController {

    private final static Logger logger = LoggerFactory.getLogger(CdrController.class);

    @Autowired
    private VoiceNotificationService service;

    /**
     *
     *  客户请求json格式：
         {
             "wav_base_url": "http://192.168.1.2/",
             "gateway_name" : "MRWG",
             "data": [
                 {"mobile": "15005600327", "wav": "111.wav" },
                 {"mobile": "15005600328", "wav": "222.wav" }
             ]
         }
     *
     * @param requestBody
     * @return
     */
    @PostMapping("/start")
    @ResponseBody
    public String start(@RequestBody JSONObject requestBody, HttpServletRequest request, HttpServletResponse response) {
        String vResult = CommonUtils.validateHttpHeaderToken(request, response);
        if(!StringUtils.isEmpty(vResult)){ return vResult; }

        String url = requestBody.getString("wav_base_url");
        String gatewayName = requestBody.getString("gateway_name");
        JSONArray dataArray = requestBody.getJSONArray("data");
        List<CallVoiceNotification> list = new ArrayList<>(1000);
        String batchId = UuidGenerator.GetOneUuid();

        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            String mobile = item.getString("mobile");
            String wav = item.getString("wav");
            CallVoiceNotification task = new CallVoiceNotification();
            task.setId(UuidGenerator.GetOneUuid());
            task.setBatchId(batchId);
            task.setTelephone(mobile);
            task.setCreatetime(System.currentTimeMillis());
            task.setGatewayName(gatewayName);
            task.setVoiceFileUrl(url + wav);
            task.setCallstatus(0);
            list.add(task);
        }
        MessageResponse msg = new MessageResponse();
        if(list.size() > 30000 ){
            msg.setMsg("单次提交的号码不能超过3万个.");
            msg.setStatus(400);
            return JSON.toJSONString(msg);
        }
        if(service.insertBatch(list) > 0){
           boolean success = TaskManager.getInstance().addCallTaskToQueue(list);
           if(!success){
               msg.setMsg("queue is full");
               msg.setStatus(500);
               List<String> ids = new ArrayList<>(1000);
               for (CallVoiceNotification task : list) {
                   ids.add(task.getId());
               }
               int rows = service.deleteBatchByIds(ids);
               logger.info("addCallTaskToQueue error. deleteBatchByIds affect rows={}", rows);
           }else{
              msg.setStatus(200);
              JSONObject jsonObject = new JSONObject();
               jsonObject.put("batch_id", batchId);
              msg.setObject(jsonObject);
           }
        }else{
            msg.setMsg("database error.");
            msg.setStatus(500);
        }
        return JSON.toJSONString(msg);
    }

    @GetMapping("/query")
    @ResponseBody
    public String query(HttpServletRequest request, HttpServletResponse response) {
        String vResult = CommonUtils.validateHttpHeaderToken(request, response);
        if (!StringUtils.isEmpty(vResult)) {   return vResult;   }

        MessageResponse msg = new MessageResponse();
        String phone = request.getParameter("phone");
        String batchId = request.getParameter("batch_id");
        if(StringUtils.isEmpty(phone) && StringUtils.isEmpty(batchId)){
            msg.setStatus(400);
            msg.setMsg("The two parameters, \"phone\" and \"batch_id\", cannot be empty simultaneously");
        }else{
            List<CallVoiceNotification> list = null;
            if(!StringUtils.isEmpty(phone)) {
                list =   service.selectByTelephone(phone);
            }
            if(!StringUtils.isEmpty(batchId)) {
                list =   service.selectByBatchId(batchId);
            }
            msg.setObject(list);
        }
        return JSON.toJSONString(msg);
    }

    @GetMapping("/delete")
    @ResponseBody
    public String delete(HttpServletRequest request, HttpServletResponse response) {
        List<String> ids = new ArrayList<>(1000);
        ids.add("1877272030483398651");
        ids.add("1877272030483398652");
        ids.add("1877272030483398653");
        int rows = service.deleteBatchByIds(ids);
        logger.info("addCallTaskToQueue error. deleteBatchByIds affect rows={}", rows);
        return "success";
    }

}
