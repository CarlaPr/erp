package com.alfatahi.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Habilita execução de rotinas automáticas de madrugada
public class ErpApplication {
	public static void main(String[] args) {
		SpringApplication.run(ErpApplication.class, args);
	}
}
