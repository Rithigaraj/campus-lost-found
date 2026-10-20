package com.campuslostfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Campus Lost &amp; Found web application.
 *
 * Exposes a REST API under /api/** and serves the static web frontend
 * (src/main/resources/static) from the embedded Tomcat server.
 */
@SpringBootApplication
public class CampusLostFoundApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLostFoundApplication.class, args);
    }
}
