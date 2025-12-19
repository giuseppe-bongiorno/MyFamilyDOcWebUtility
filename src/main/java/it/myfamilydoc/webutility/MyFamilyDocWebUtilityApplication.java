package it.myfamilydoc.webutility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyFamilyDocWebUtilityApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyFamilyDocWebUtilityApplication.class, args);
		System.out.println("=== MICROSERVIZIO MyFamilyDocWebUtility AVVIATO CORRETTAMENTE ===");
	}

}