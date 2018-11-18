package com.kiss.kissnest.mapper;

import com.kiss.kissnest.entity.Job;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface JobMapper {

    Integer createJob(Job job);

    List<Job> getJobByProjectId(Integer projectId);

    Job getJobByProjectIdAndType(Map params);

    Integer updateJobStatus(Map params);

    Integer updateJobStatusAndNumber(Map params);

    List<Job> getJobsByTeamId(Map params);

    Integer updateJob(Job job);

    Job getJobById(Integer id);

    Integer deleteJobById(Integer id);
}
