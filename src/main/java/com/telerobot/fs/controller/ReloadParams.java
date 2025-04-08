package com.telerobot.fs.controller;

import com.alibaba.fastjson.JSON;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.telerobot.fs.config.AppContextProvider;
import com.telerobot.fs.config.SystemConfig;
import com.telerobot.fs.entity.dto.FreeswitchNodeInfo;
import com.telerobot.fs.service.SysService;
import com.telerobot.fs.utils.CommonUtils;
import com.telerobot.fs.utils.DESUtil;
import com.telerobot.fs.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Scope("request")
@Controller
public class ReloadParams {
	private static final Logger logger = LoggerFactory.getLogger(ReloadParams.class);

	@Resource
	private SysService sysService;
  
	@RequestMapping("/reloadParams")
	@ResponseBody
	public String reload(HttpServletRequest request,Map<String,Object> model) throws InstantiationException, IllegalAccessException {
		sysService.refreshParams();
		return "success";
	}

	@RequestMapping("/getParams")
	@ResponseBody
	public String getParams(HttpServletRequest request,Map<String,Object> model) throws InstantiationException, IllegalAccessException {
		String key = request.getParameter("key");
		return key + "=" + SystemConfig.getValue(key);
	}

	@RequestMapping("/shutdown")
	@ResponseBody
	public String shutdown(HttpServletRequest request,Map<String,Object> model) throws InstantiationException, IllegalAccessException {
		String clientIP = request.getRemoteAddr();
		if(!"127.0.0.1".equalsIgnoreCase(clientIP)){
            return  "forbidden, only 127.0.0.1 allowed.";
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.exit(0);
			}
		}).start();
		return "success";
	}

	@RequestMapping("/create-ext-password")
	@ResponseBody
	public String createExtensionPassword(HttpServletRequest request){
		String extPass = request.getParameter("pass");
		String encryptStr = DESUtil.encrypt(extPass + "," + DateUtils.format(DateUtils.addDays(new Date(), 1), "yyyyMMddHHmm"));
		return String.format("var _phoneEncryptPassword='%s';", encryptStr);
	}

	@RequestMapping("/create-gateway-list")
	@ResponseBody
	public String createGatewayList(HttpServletRequest request){
		String gwList = "[\n" +
				"                     {\n" +
				"                         uuid         :  '01',  \n" +
				"                         updateTime         :  1675953810537,  \n" +
				"                         gatewayAddr  : '192.168.66.71:4080',  \n" +
				"                         callerNumber : '007',    \n" +
				"                         calleePrefix : '',       \n" +
				"                         priority     : 1,        \n" +
				"                         concurrency  : 110,       \n" +
				"                         register     : false,    \n" +
				"                         audioCodec   : 'g729'    \n" +
				"                     },\n" +
				"                     {\n" +
				"                         uuid         :  '02',  \n" +
				"                         updateTime         :  1675953810497,  \n" +
				"                         gatewayAddr  : 'MRWG',  \n" +
				"                         callerNumber : '019',\n" +
				"                         calleePrefix : '',\n" +
				"                         priority     : 2,\n" +
				"                         concurrency  : 10,\n" +
				"                         register     : true,\n" +
				"                         audioCodec   : 'g711'\n" +
				"                     }\n" +
				" ]";
		String encryptStr = DESUtil.encrypt(gwList);
		return String.format("var _configGatewayList='%s';", encryptStr);
	}


	@RequestMapping("/create-token")
	@ResponseBody
	public String createToken(HttpServletRequest request) throws Exception {
		try {
			String extnum = request.getParameter("extnum");
			String opnum =  request.getParameter("opnum");
			String groupId =  request.getParameter("groupId");
			String skillLevel =  request.getParameter("skillLevel");
			String projectId = request.getParameter("projectId");

			//登录成功后生成JWT
			//JWT的header部分,该map可以是空的,因为有默认值{"alg":HS256,"typ":"JWT"}
			Map<String, Object> map = new HashMap<>();
			Calendar instance = Calendar.getInstance();
			instance.add(Calendar.HOUR, 24);
			String token = JWT.create()
					//添加头部
					.withHeader(map)
					//添加payload
					.withClaim("extnum", extnum)
					.withClaim("opnum", opnum)
					.withClaim("groupId", groupId)
					.withClaim("skillLevel", skillLevel)
					.withClaim("projectId", projectId)
					//设置过期时间
					.withExpiresAt(instance.getTime())
					//设置签名 密钥
					.sign(Algorithm.HMAC256(
							SystemConfig.getValue("ws-server-auth-token-secret").trim()
					));
			return String.format("var loginToken=\"%s\";",token);

		} catch (Exception err) {
			throw new Exception("error:" + err.getMessage());
		}
	}

	@RequestMapping("/verify-token")
	@ResponseBody
	public String verifyToken(HttpServletRequest request) throws Exception {
		String token = request.getParameter("token");
 		try {
			Map<String,String> map = CommonUtils.validateToken(token, "");
			logger.info("extnum={}, opnum={}, groupId={}, skillLevel={}",
					map.get("extnum"),
					map.get("opnum"),
					map.get("groupId"),
					map.get("skillLevel")
			);
			return "验证成功";
		} catch (Exception err) {
			return "验证错误:" + err.getMessage();
		}
	}

	//

	@RequestMapping("/freeswitch_nodes")
	@ResponseBody
	public String getFreeswitchNodes(HttpServletRequest request) throws Exception {
		List<FreeswitchNodeInfo> nodes = new ArrayList<>(10);
		FreeswitchNodeInfo nodeInfo = new FreeswitchNodeInfo();
		nodeInfo.setCurrentLoad(0);
		nodeInfo.setHost("192.168.67.251");
		nodeInfo.setMaxLoad(32);
		nodeInfo.setPass("ClueCon");
		nodeInfo.setPort(8021);
		nodes.add(nodeInfo);
		return JSON.toJSONString(nodes);
	}

	@RequestMapping("/saveCdrTest")
	@ResponseBody
	public String saveCdrTest(HttpServletRequest request,Map<String,Object> model) throws InstantiationException, IllegalAccessException {
		String cdrString = request.getParameter("cdr");
		//logger.info("cdr=" + cdrString);
		return  "success";
	}

}
