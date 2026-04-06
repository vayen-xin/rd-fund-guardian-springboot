package com.vayen.rdcm;

import cn.dev33.satoken.secure.BCrypt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RdcmApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void generatePassword() {
		String hashpw = BCrypt.hashpw("123456");
		System.out.println(hashpw);

		boolean checkpw = BCrypt.checkpw("123456", "$2a$10$FJBzVFB8k/mtBaRuiy87LeOeEou2hDQHx4UcNSjsL7KuFsQ/LFniW");
		System.out.println(checkpw);
	}

}
