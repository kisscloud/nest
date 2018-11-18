package com.kiss.kissnest.dao;

import com.kiss.kissnest.entity.BuildLog;
import com.kiss.kissnest.output.BuildLogOutput;

import java.util.List;

public interface BuildLogDao {

    Integer createBuildLog (BuildLog buildLog);

    Integer deleteBuildLogById(Integer id);

    Integer updateBuildLog(BuildLog buildLog);

    BuildLog getBuildLogById(Integer id);

    List<BuildLog> getBuildLogs();

    BuildLog getLastBuildByJobNameAndProjectId(String jobName,Integer projectId);

    List<BuildLogOutput> getBuildLogsByTeamId(Integer teamId,Integer start,Integer size);

    BuildLogOutput getBuildRecentLog(Integer teamId, Integer projectId, Long queueId);

    Integer deleteBuildLogsByProjectId(Integer projectId);

    Integer getBuildLogCountByTeamId(Integer teamId);
}
