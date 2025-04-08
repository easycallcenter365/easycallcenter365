package com.telerobot.fs;

import com.telerobot.fs.config.AppContextProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;

@ComponentScan(basePackages = {"com.telerobot.fs.*"},nameGenerator = AnnotationBeanNameGenerator.class)
@SpringBootApplication
@MapperScan(basePackages = {"com.telerobot.fs.mybatis.persistence"})
public class CallApplication {
    private static Logger logger =  LoggerFactory.getLogger(CallApplication.class);

    /**
     * Start
     * @throws ParseException 
     * @throws InterruptedException 
     * @throws IOException 
     */
    public static void main(String[] args) throws  Exception {
        SpringApplication.run(CallApplication.class, args);

        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = AppContextProvider.getEnvConfig("server.port");
        String path = AppContextProvider.getEnvConfig("server.servlet.context-path");
        logger.info("\n---------------  Call-Center Application Start Success  ---------------\n\t" +
                "call-center-for-robot  started successfully .... \n\t" +
                "local: \t\thttp://localhost:" + port + path + "/\n\t" +
                "external: \thttp://" + ip + ":" + port + path + "/\n\t" +
                "----------------------------------------------------------");
     }


}



