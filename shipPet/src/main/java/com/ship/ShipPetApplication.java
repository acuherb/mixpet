package com.ship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShipPetApplication {
    private static final Logger log = LoggerFactory.getLogger(ShipPetApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ShipPetApplication.class, args);
        log.info(":: MixPet ::  智慧宠物医疗管理系统启动");
        log.info(":: http://localhost:8085");
    }
}
