package com;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
public class ResourceManagementSystemApplication {

	public static void main(String[] args) {
		try {
			// Load .env file if it exists (for local development)
			Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();
			
			// Set system properties from .env file for fallback
			dotenv.entries().forEach(entry -> 
				System.setProperty(entry.getKey(), entry.getValue()));
			
			SpringApplication app = new SpringApplication(ResourceManagementSystemApplication.class);
			
			// Add a listener to validate critical properties after context is loaded
			app.addListeners(event -> {
				if (event instanceof org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent) {
					Environment env = ((org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent) event).getEnvironment();
					validateCriticalProperties(env);
				}
			});
			
			app.run(args);
		} catch (Exception e) {
			System.err.println("Failed to start application: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void validateCriticalProperties(Environment env) {
		List<String> criticalProperties = Arrays.asList(
			"spring.application.name",
			"spring.datasource.url",
			"spring.datasource.username",
			"spring.datasource.password"
		);
		
		boolean hasErrors = false;
		
		for (String property : criticalProperties) {
			String value = env.getProperty(property);
			if (value == null || value.trim().isEmpty() || value.contains("${")) {
				System.err.println("CRITICAL: Missing or invalid property: " + property);
				hasErrors = true;
			}
		}
		
		if (hasErrors) {
			System.err.println("Application startup failed due to missing critical properties.");
			System.err.println("Please check your environment variables or .env file.");
			throw new IllegalStateException("Critical properties are missing or invalid");
		}
		
		System.out.println("Environment validation completed successfully.");
	}
}
