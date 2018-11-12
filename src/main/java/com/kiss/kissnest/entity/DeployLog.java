package com.kiss.kissnest.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DeployLog {

    private Integer id;

    private Integer teamId;

    private String jobName;

    private String serverIds;

    private String branch;

    private Integer number;

    private String version;

    private Integer projectId;

    private String remark;

    private Integer status;

    private String output;

    private Integer operatorId;

    private String operatorName;

    private Long deployAt;

    private Date createdAt;

    private Date updatedAt;
}
