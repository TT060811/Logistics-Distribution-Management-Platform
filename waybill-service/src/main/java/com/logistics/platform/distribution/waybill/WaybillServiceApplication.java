package com.logistics.platform.distribution.waybill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EntityScan(basePackages = "com.logistics.platform.distribution.waybill")
public class WaybillServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaybillServiceApplication.class, args);
    }
}