package com.kiss.kissnest.validator;

import com.kiss.kissnest.dao.ProjectDao;
import com.kiss.kissnest.entity.Project;
import com.kiss.kissnest.input.BuildJobInput;
import com.kiss.kissnest.input.CreateJobInput;
import com.kiss.kissnest.status.NestStatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class JobValidator implements Validator {

    @Autowired
    private ProjectDao projectDao;

    @Override
    public boolean supports(Class<?> clazz) {

        return clazz.equals(CreateJobInput.class) ||
                clazz.equals(BuildJobInput.class);
    }

    @Override
    public void validate(Object target, Errors errors) {

        if (CreateJobInput.class.isInstance(target)) {
            CreateJobInput createJobInput = (CreateJobInput) target;
            validateProjectId(createJobInput.getProjectId(),errors);
            validateShell(createJobInput.getShell(),errors);
            validateType(createJobInput.getType(),errors);
        } else if (BuildJobInput.class.isInstance(target)) {
            BuildJobInput buildJobInput = (BuildJobInput) target;
            validateProjectId(buildJobInput.getProjectId(),errors);
            validateBranch(buildJobInput.getBranch(),errors);
        }
    }

    public void validateProjectId (Integer projectId,Errors errors) {

        if (projectId == null) {
            errors.rejectValue("projectId",String.valueOf(NestStatusCode.PROJECT_ID_IS_EMPTY),"项目id不能为空");
            return;
        }

        Project project = projectDao.getProjectById(projectId);

        if (project == null) {
            errors.rejectValue("projectId",String.valueOf(NestStatusCode.PROJECT_NOT_EXIST),"项目不存在");
        }
    }

    public void validateShell (String shell,Errors errors) {

        if (StringUtils.isEmpty(shell)) {
            errors.rejectValue("shell",String.valueOf(NestStatusCode.SHELL_IS_EMPTY));
        }
    }

    public void validateType (Integer type,Errors errors) {

        if (type == null) {
            errors.rejectValue("type",String.valueOf(NestStatusCode.TYPE_IS_EMPTY),"job类型不能为空");
        }
    }

    public void validateBranch (String branch,Errors errors) {

        if (StringUtils.isEmpty(branch)) {
            errors.rejectValue("branch",String.valueOf(NestStatusCode.BRANCH_IS_EMPTY),"分支名不能为空");
        }
    }
}