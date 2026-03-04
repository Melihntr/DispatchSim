package com.dispatchsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Zamanlanmış görevleri aktif eder
public class DispatchsimApplication {

	public static void main(String[] args) {
		SpringApplication.run(DispatchsimApplication.class, args);
	}

}
