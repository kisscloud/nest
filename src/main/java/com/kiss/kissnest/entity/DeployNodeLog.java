package com.kiss.kissnest.entity;

import lombok.Data;

@Data
public class DeployNodeLog {
    private Integer id;
    private Integer teamId;
    private Integer envId;
    private Integer jobId;
    private Integer deployLogId;
    private Integer serverId;
    private String nodeId;
    private Integer status;
    private String output;
}
