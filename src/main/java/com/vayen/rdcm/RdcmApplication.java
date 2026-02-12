package com.vayen.rdcm;

import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class RdcmApplication {

	public static void main(String[] args) throws java.sql.SQLException {
		// h2数据库tcp连接启动
		Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists").start();
		SpringApplication.run(RdcmApplication.class, args);
	}

}
