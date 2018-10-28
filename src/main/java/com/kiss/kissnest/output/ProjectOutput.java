package com.kiss.kissnest.output;

import lombok.Data;

import java.util.Date;

@Data
public class ProjectOutput {

    private Integer id;

    private String name;

    private Integer membersCount;

    private Integer needsCount;

    private Integer issuesCount;

    private Integer operatorId;

    private String operatorName;

    private Date createdAt;

    private Date updatedAt;
}
