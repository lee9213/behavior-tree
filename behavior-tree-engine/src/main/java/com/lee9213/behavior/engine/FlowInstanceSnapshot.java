package com.lee9213.behavior.engine;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.Map;

@Value
@Builder
public class FlowInstanceSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    String definitionId;
    String definitionVersion;
    String status;
    Map<String, Integer> retryCountByStepPath;
}
