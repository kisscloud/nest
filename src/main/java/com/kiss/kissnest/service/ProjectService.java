package com.kiss.kissnest.service;

import com.kiss.kissnest.dao.*;
import com.kiss.kissnest.entity.*;
import com.kiss.kissnest.enums.OperationTargetType;
import com.kiss.kissnest.exception.TransactionalException;
import com.kiss.kissnest.input.CreateProjectInput;
import com.kiss.kissnest.input.CreateTagInput;
import com.kiss.kissnest.input.UpdateProjectInput;
import com.kiss.kissnest.output.ProjectOutput;
import com.kiss.kissnest.output.ProjectTypeOutput;
import com.kiss.kissnest.output.TagOutput;
import com.kiss.kissnest.status.NestStatusCode;
import com.kiss.kissnest.util.CodeUtil;
import com.kiss.kissnest.util.GitlabApiUtil;
import com.kiss.kissnest.util.JenkinsUtil;
import com.kiss.kissnest.util.ResultOutputUtil;
import entity.Guest;
import lombok.extern.slf4j.Slf4j;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabBranchCommit;
import org.gitlab.api.models.GitlabRelease;
import org.gitlab.api.models.GitlabTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import output.ResultOutput;
import utils.BeanCopyUtil;
import utils.GuestUtil;
import utils.ThreadLocalUtil;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProjectService {

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private GitlabApiUtil gitlabApiUtil;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private BuildLogDao buildLogDao;

    @Autowired
    private JenkinsUtil jenkinsUtil;

    @Autowired
    private ProjectRepositoryDao projectRepositoryDao;

    @Autowired
    private CodeUtil codeUtil;

    @Value("${project.type}")
    private String projectTypes;

    @Autowired
    private OperationLogService operationLogService;

    public ResultOutput createProject(CreateProjectInput createProjectInput) {

        Project project = BeanCopyUtil.copy(createProjectInput, Project.class);
        Guest guest = ThreadLocalUtil.getGuest();
        project.setMembersCount(1);
        project.setOperatorId(guest.getId());
        project.setOperatorName(guest.getName());
        Integer count = projectDao.createProject(project);

        if (count == 0) {
            return ResultOutputUtil.error(NestStatusCode.CREATE_PROJECT_FAILED);
        }

        groupDao.addCount(project.getTeamId(), project.getGroupId(), "projects", 1);
        Member member = memberDao.getMemberByAccountId(ThreadLocalUtil.getGuest().getId());
        memberDao.addCount(member.getId(), 1, "projects");
        ProjectOutput projectOutput = projectDao.getProjectOutputById(project.getId());
        projectOutput.setTypeText(codeUtil.getEnumsMessage("project.type", String.valueOf(projectOutput.getType())));
        Integer id = project.getId();
        project = projectDao.getProjectById(id);
        operationLogService.saveOperationLog(project.getTeamId(), guest, null, project, "id", OperationTargetType.TYPE_CREATE_PROJECT);
        operationLogService.saveDynamic(guest, project.getTeamId(), project.getGroupId(), project.getId(), OperationTargetType.TYPE_CREATE_PROJECT, project);
        return ResultOutputUtil.success(projectOutput);
    }

    @Transactional
    public ResultOutput deleteProject(Integer id) {

        Project project = projectDao.getProjectById(id);

        if (project == null) {
            return ResultOutputUtil.error(NestStatusCode.PROJECT_NOT_EXIST);
        }

        Integer count = projectDao.deleteProjectById(id);

        if (count == 0) {
            return ResultOutputUtil.error(NestStatusCode.DELETE_PROJECT_FAILED);
        }

        buildLogDao.deleteBuildLogsByProjectId(project.getId());

        List<Job> jobs = jobDao.getJobByProjectId(project.getId());
        Guest guest = ThreadLocalUtil.getGuest();
        Member member = memberDao.getMemberByAccountId(guest.getId());

        if (jobs != null && jobs.size() != 0) {
            jobs.forEach(job -> {
                Integer jobCount = jobDao.deleteJobById(job.getId());

                if (jobCount == 0) {
                    throw new TransactionalException(NestStatusCode.DELETE_JOB_FAILED);
                }

                jenkinsUtil.deleteJob(job.getJobName(), guest.getName(), member.getApiToken());
            });
        }

        ProjectRepository projectRepository = projectRepositoryDao.getProjectRepositoryByProjectId(id);

        if (projectRepository != null) {
            projectRepositoryDao.deleteProjectRepositoryById(projectRepository.getId());
            gitlabApiUtil.deleteProject(projectRepository.getRepositoryId(), member.getAccessToken());
        }

        operationLogService.saveOperationLog(project.getTeamId(), ThreadLocalUtil.getGuest(), project, null, "id", OperationTargetType.TYPE_DELETE_PROJECT);
        operationLogService.saveDynamic(guest, project.getTeamId(), project.getGroupId(), project.getId(), OperationTargetType.TYPE_DELETE_PROJECT, project);
        return ResultOutputUtil.success();
    }

    public ResultOutput updateProject(UpdateProjectInput updateProjectInput) {

        Project project = BeanCopyUtil.copy(updateProjectInput, Project.class);
        Project oldValue = projectDao.getProjectById(updateProjectInput.getId());
        Guest guest = ThreadLocalUtil.getGuest();
        project.setOperatorId(guest.getId());
        project.setOperatorName(guest.getName());
        Integer count = projectDao.updateProject(project);

        if (count == 0) {
            return ResultOutputUtil.error(NestStatusCode.UPDATE_PROJECT_FAILED);
        }

        operationLogService.saveOperationLog(project.getTeamId(), guest, oldValue, project, "id", OperationTargetType.TYPE_UPDATE_PROJECT);
        operationLogService.saveDynamic(guest, project.getTeamId(), project.getGroupId(), project.getId(), OperationTargetType.TYPE_UPDATE_PROJECT, project);

        ProjectOutput projectOutput = BeanCopyUtil.copy(project, ProjectOutput.class);
        projectOutput.setTypeText(codeUtil.getEnumsMessage("project.type", String.valueOf(project.getType())));

        return ResultOutputUtil.success(projectOutput);
    }

    public ResultOutput getProjectById(Integer id) {

        Project project = projectDao.getProjectById(id);

        return ResultOutputUtil.success(BeanCopyUtil.copy(project, ProjectOutput.class));
    }

    public ResultOutput getProjects(Integer teamId, Integer groupId) {

        List<ProjectOutput> projectOutputs = projectDao.getProjectOutputs(teamId, groupId);
        projectOutputs.forEach((projectOutput -> projectOutput.setTypeText(codeUtil.getEnumsMessage("project.type", String.valueOf(projectOutput.getType())))));

        return ResultOutputUtil.success(projectOutputs);
    }

    public ResultOutput getProjectBranches(Integer projectId) {

        ProjectRepository projectRepository = projectRepositoryDao.getProjectRepositoryByProjectId(projectId);

        if (projectRepository == null || projectRepository.getRepositoryId() == null) {
            return ResultOutputUtil.error(NestStatusCode.PROJECT_NOT_EXIST);
        }

        String accessToken = memberDao.getAccessTokenByAccountId(ThreadLocalUtil.getGuest().getId());
        List<GitlabBranch> gitlabBranches = gitlabApiUtil.getBranches(projectRepository.getRepositoryId(), accessToken);
        List<String> branches = new ArrayList<>();

        if (gitlabBranches != null) {

            for (GitlabBranch gitlabBranch : gitlabBranches) {

                if (!StringUtils.isEmpty(gitlabBranch.getName())) {
                    branches.add(gitlabBranch.getName());
                }
            }
        }

        return ResultOutputUtil.success(branches);
    }

    public ResultOutput getProjectTags(Integer projectId) {

        ProjectRepository projectRepository = projectRepositoryDao.getProjectRepositoryByProjectId(projectId);

        if (projectRepository == null || projectRepository.getRepositoryId() == null) {
            return ResultOutputUtil.error(NestStatusCode.PROJECT_NOT_EXIST);
        }

        String accessToken = memberDao.getAccessTokenByAccountId(ThreadLocalUtil.getGuest().getId());
        List<GitlabTag> gitlabTags = gitlabApiUtil.getTags(projectRepository.getRepositoryId(), accessToken);
        List<TagOutput> branches = new ArrayList<>();

        if (gitlabTags != null) {

            for (GitlabTag gitlabTag : gitlabTags) {

                if (!StringUtils.isEmpty(gitlabTag.getName())) {
                    TagOutput tagOutput = new TagOutput();
                    GitlabBranchCommit gitlabBranchCommit = gitlabTag.getCommit();
                    GitlabRelease gitlabRelease = gitlabTag.getRelease();
                    tagOutput.setDescription(gitlabRelease.getTagName());
                    tagOutput.setDescription(gitlabRelease.getDescription());
                    tagOutput.setCreatedAt(gitlabBranchCommit.getCommittedDate().getTime());
                }
            }
        }

        log.info("gitlabTags：" + gitlabTags);
        return ResultOutputUtil.success(branches);
    }

    public ResultOutput getProjectsWithoutBuildJob(Integer teamId) {

        List<Project> projects = projectDao.getProjectsWithoutBuildJob(teamId);
        List<ProjectOutput> projectOutputs = BeanCopyUtil.copyList(projects, ProjectOutput.class);

        return ResultOutputUtil.success(projectOutputs);
    }

    public ResultOutput getProjectsWithoutDeployJob(Integer teamId) {

        List<Project> projects = projectDao.getProjectsWithoutDeployJob(teamId);
        List<ProjectOutput> projectOutputs = BeanCopyUtil.copyList(projects, ProjectOutput.class);

        return ResultOutputUtil.success(projectOutputs);
    }

    public ResultOutput getProjectsWithBuildJob(Integer teamId) {

        List<Project> projects = projectDao.getProjectsWithBuildJob(teamId);
        List<ProjectOutput> projectOutputs = BeanCopyUtil.copyList(projects, ProjectOutput.class);

        return ResultOutputUtil.success(projectOutputs);
    }

    public ResultOutput getProjectsWithDeployJob(Integer teamId) {

        List<Project> projects = projectDao.getProjectsWithDeployJob(teamId);
        List<ProjectOutput> projectOutputs = BeanCopyUtil.copyList(projects, ProjectOutput.class);

        return ResultOutputUtil.success(projectOutputs);
    }

    public ResultOutput getProjectTypes() {
        String[] types = projectTypes.split(",");
        List<ProjectTypeOutput> typeList = new ArrayList<>();

        for (String type : types) {
            ProjectTypeOutput projectTypeOutput = new ProjectTypeOutput();
            projectTypeOutput.setId(Integer.valueOf(type));
            projectTypeOutput.setName(codeUtil.getEnumsMessage("project.type", type));
            typeList.add(projectTypeOutput);
        }

        return ResultOutputUtil.success(typeList);
    }

    public ResultOutput addTag(CreateTagInput createTagInput) {

        ProjectRepository projectRepository = projectRepositoryDao.getProjectRepositoryByProjectId(createTagInput.getProjectId());
        Integer repositoryId = projectRepository.getRepositoryId();
        Member member = memberDao.getMemberByAccountId(GuestUtil.getGuestId());
        GitlabTag gitlabTag = gitlabApiUtil.addTag(repositoryId, createTagInput.getTagName(), createTagInput.getRef(), createTagInput.getMessage(), createTagInput.getReleaseDescription(), member.getAccessToken());

        if (gitlabTag == null) {
            return ResultOutputUtil.error(NestStatusCode.CREATE_PROJECT_TAG_FAILED);
        }

        operationLogService.saveOperationLog(projectRepository.getTeamId(), ThreadLocalUtil.getGuest(), null, createTagInput, "id", OperationTargetType.TYPE__CREATE_PROJECT_TAG);
        operationLogService.saveDynamic(ThreadLocalUtil.getGuest(), projectRepository.getTeamId(), null, projectRepository.getProjectId(), OperationTargetType.TYPE__CREATE_PROJECT_TAG, createTagInput);

        TagOutput tagOutput = new TagOutput();
        GitlabBranchCommit gitlabBranchCommit = gitlabTag.getCommit();
        GitlabRelease gitlabRelease = gitlabTag.getRelease();
        tagOutput.setDescription(gitlabRelease.getTagName());
        tagOutput.setDescription(gitlabRelease.getDescription());
        tagOutput.setCreatedAt(gitlabBranchCommit.getCommittedDate().getTime());

        return ResultOutputUtil.success(tagOutput);
    }
}
