package com.thissideupsoftware;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class Thing {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    UUID id;

    String data;
}
