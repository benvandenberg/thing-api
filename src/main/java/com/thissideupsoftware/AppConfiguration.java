package com.thissideupsoftware;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import lombok.Getter;

@Getter
public class AppConfiguration extends Configuration {

    @JsonProperty("localstack")
    LocalstackConfiguration localstack;
}
