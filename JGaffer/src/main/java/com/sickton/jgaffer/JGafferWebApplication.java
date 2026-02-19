package com.sickton.jgaffer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the JGaffer web application.
 *
 * <p>Starts the embedded Tomcat server on port 8080 and serves the Thymeleaf
 * web UI. The original CLI entry point ({@code jgafferApplication}) remains
 * runnable as a fallback from an IDE.</p>
 *
 * @author sickton
 */
@SpringBootApplication
public class JGafferWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(JGafferWebApplication.class, args);
    }
}
