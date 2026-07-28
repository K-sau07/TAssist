package com.tassist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TAssist application entry point.
 *
 * <p>Architecture (spec §7): layered with hexagonal influence. Dependencies point
 * inward — {@code infrastructure → application → domain}. The {@code com.tassist.domain}
 * package must contain zero framework imports.
 */
@SpringBootApplication
public class TassistApplication {

    public static void main(String[] args) {
        SpringApplication.run(TassistApplication.class, args);
    }
}
