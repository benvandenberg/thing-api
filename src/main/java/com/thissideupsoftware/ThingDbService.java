package com.thissideupsoftware;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThingDbService {

    private LocalstackConfiguration localstackConfiguration;

    public ThingDbService(LocalstackConfiguration localstackConfiguration) {
        this.localstackConfiguration = localstackConfiguration;
        log.info("serviceHost=" + localstackConfiguration.getServiceHost());
        log.info("servicePort=" + localstackConfiguration.getServicePort());
        log.info("serviceScheme=" + localstackConfiguration.getServiceScheme());
    }
}
