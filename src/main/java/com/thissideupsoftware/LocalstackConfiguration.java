package com.thissideupsoftware;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LocalstackConfiguration {

    @JsonProperty("awsRegion")
    String awsRegion;

    // domain name, or IP address
    @JsonProperty("serviceHost")
    String serviceHost;

    // http or https
    @JsonProperty("serviceScheme")
    String serviceScheme;

    // 1 through 64k
    @JsonProperty("dynamoDbPort")
    Integer dynamoDbPort;
}
