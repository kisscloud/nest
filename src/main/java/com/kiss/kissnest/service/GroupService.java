package com.kiss.kissnest.service;

import com.kiss.kissnest.dao.GroupDao;
import com.kiss.kissnest.dao.GroupProjectDao;
import com.kiss.kissnest.dao.MemberDao;
import com.kiss.kissnest.dao.TeamDao;
import com.kiss.kissnest.entity.Group;
import com.kiss.kissnest.entity.GroupProject;
import com.kiss.kissnest.entity.OperationTargetType;
import com.kiss.kissnest.exception.TransactionalException;
import com.kiss.kissnest.input.CreateGroupInput;
import com.kiss.kissnest.input.UpdateGroupInput;
import com.kiss.kissnest.output.GroupOutput;
import com.kiss.kissnest.status.NestStatusCode;
import com.kiss.kissnest.util.CodeUtil;
import com.kiss.kissnest.util.GitlabApiUtil;
import com.kiss.kissnest.util.ResultOutputUtil;
import entity.Guest;
import org.gitlab.api.models.GitlabGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import output.ResultOutput;
import utils.BeanCopyUtil;
import utils.ThreadLocalUtil;

import java.util.List;

@Service
public class GroupService {

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private GroupProjectDao groupProjectDao;

    @Autowired
    private TeamDao teamDao;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private GitlabApiUtil gitlabApiUtil;

    @Autowired
    private CodeUtil codeUtil;

    @Autowired
    private OperationLogService operationLogService;

    @Transactional
    public ResultOutput createGroup(CreateGroupInput createGroupInput) {

        Group group = (Group) BeanCopyUtil.copy(createGroupInput, Group.class);
        Guest guest = ThreadLocalUtil.getGuest();
        group.setStatus(1);
        group.setMembersCount(1);
        group.setOperatorId(guest.getId());
        group.setOperatorName(guest.getName());
        Integer count = groupDao.createGroup(group);

        if (count == 0) {
            return ResultOutputUtil.error(NestStatusCode.CREATE_GROUP_FAILED);
        }

        teamDao.addGroupsCount(group.getTeamId());

        String accessToken = memberDao.getAccessTokenByAccountId(ThreadLocalUtil.getGuest().getId());
        Integer parentId = teamDao.getRepositoryIdByTeamId(group.getTeamId());

        if (parentId == null) {
            throw new TransactionalException(NestStatusCode.GROUP_PARENTID_LOSED);
        }

        GitlabGroup gitlabGroup = gitlabApiUtil.createSubGroup(group.getSlug(), accessToken, parentId);

        if (gitlabGroup == null) {
            throw new TransactionalException(NestStatusCode.CREATE_GROUP_REPOSITORY_FAILED);
        }

        group.setRepositoryId(gitlabGroup.getId());
        groupDao.addRepositoryIdById(group);
//        operationLogService.saveOperationLog(group.getTeamId(),guest,null,group,"id",OperationTargetType.TYPE_CREATE_GROUP);
        GroupOutput groupOutput = (GroupOutput) BeanCopyUtil.copy(group, GroupOutput.class);
        groupOutput.setStatusText(codeUtil.getEnumsMessage("group.status", String.valueOf(groupOutput.getStatus())));

        return ResultOutputUtil.success(groupOutput);
    }

    public ResultOutput deleteGroup(Integer id) {

        List<GroupProject> groupProjects = groupProjectDao.getGroupProjectsByGroupId(id);

        if (groupProjects != null && groupProjects.size() != 0) {
            return ResultOutputUtil.error(NestStatusCode.GROUP_PROJECT_EXIST);
        }

        Group group = groupDao.getGroupById(id);

        if (group == null) {
            return ResultOutputUtil.error(NestStatusCode.GROUP_NOT_EXIST);
        }

        Integer count = groupDao.deleteGroupById(id);

        if (count == 0) {
            return ResultOutputUtil.error(NestStatusCode.DELETE_GROUP_FAILED);
        }

//        operationLogService.saveOperationLog(group.getTeamId(),ThreadLocalUtil.getGuest(),group,null,"id",OperationTargetType.TYPE_DELETE_GROUP);
        return ResultOutputUtil.success();
    }

    public ResultOutput updateGroup(UpdateGroupInput updateGroupInput) {

        Group group = (Group) BeanCopyUtil.copy(updateGroupInput, Group.class);
        Guest guest = ThreadLocalUtil.getGuest();
        group.setOperatorId(guest.getId());
        group.setOperatorName(guest.getName());
        group.setStatus(1);
        Group oldValue = groupDao.getGroupById(group.getId());
        Integer count = groupDao.updateGroupById(group);

        if (count == 0) {
            return ResultOutputUtil.error(NestStatusCode.UPDATE_GROUP_FAILED);
        }

//        operationLogService.saveOperationLog(group.getTeamId(),guest,oldValue,group,"id",OperationTargetType.TYPE_UPDATE_GROUP);

        return ResultOutputUtil.success(BeanCopyUtil.copy(group, GroupOutput.class));
    }

    public ResultOutput getGroupById(Integer id) {

        Group group = groupDao.getGroupById(id);

        if (group == null) {
            return ResultOutputUtil.error(NestStatusCode.GROUP_NOT_EXIST);
        }

        return ResultOutputUtil.success(BeanCopyUtil.copy(group, GroupOutput.class));
    }

    public ResultOutput getGroups(Integer teamId) {

        List<Group> groups = groupDao.getGroups(teamId);

        List<GroupOutput> groupOutputs = (List) BeanCopyUtil.copyList(groups, GroupOutput.class, BeanCopyUtil.defaultFieldNames);

        groupOutputs.forEach(groupOutput -> groupOutput.setStatusText(codeUtil.getEnumsMessage("group.status", String.valueOf(groupOutput.getStatus()))));

        return ResultOutputUtil.success(groupOutputs);
    }
}
