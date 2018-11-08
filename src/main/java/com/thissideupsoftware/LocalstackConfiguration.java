package com.thissideupsoftware;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LocalstackConfiguration {

    // domain name, or IP address
    @JsonProperty("serviceHost")
    String serviceHost;

    // 1 through 64k
    @JsonProperty("servicePort")
    Integer servicePort;

    // http or https
    @JsonProperty("serviceScheme")
    String serviceScheme;
}
