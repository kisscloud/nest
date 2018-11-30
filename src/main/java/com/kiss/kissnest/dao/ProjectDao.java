package com.kiss.kissnest.dao;

import com.kiss.kissnest.entity.Group;
import com.kiss.kissnest.entity.Project;
import com.kiss.kissnest.output.ProjectOutput;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ProjectDao {

    Integer createProject(Project project);

    Integer deleteProjectById(Integer id);

    Integer updateProject(Project project);

    Integer addCount(Map map);

    Project getProjectById(Integer id);

    List<Project> getProjects(Integer teamId, Integer groupId);

    Project getProjectByNameAndTeamId(String name, Integer teamId);

    Project getProjectBySlugAndTeamId(String slug, Integer teamId);

    Integer addRepositoryIdById(Project project);

    Project getProjectByRepositoryId(Integer repositoryId);

    List<Project> getProjectsWithoutBuildJob(Integer teamId);

    List<Project> getProjectsWithoutDeployJob(Integer teamId);

    List<Project> getProjectsWithBuildJob(Integer teamId);

    List<Project> getProjectsWithDeployJob(Integer teamId);

    String getProjectOperatorAccessToken(Integer projectId);

    String getProjectNameByServerId(String serverId);

    List<Project> getProjectsByGroupId(Integer groupId);

    List<ProjectOutput> getProjectOutputs(Integer teamId, Integer groupId);
}
