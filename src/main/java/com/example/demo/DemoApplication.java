package com.example.demo;

import com.example.demo.dao.SampleDao;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(SampleDao dao) {
		return args -> {
			System.out.println("------------------------------------------------------------");
			dao.longRunningTask(3, warning -> System.out.println("msg: " + warning.getMessage()));
			System.out.println("------------------------------------------------------------");
		};
	}
}
