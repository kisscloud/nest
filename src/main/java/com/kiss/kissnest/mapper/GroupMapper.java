package com.kiss.kissnest.mapper;

import com.kiss.kissnest.entity.Group;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface GroupMapper {

    Integer createGroup(Group group);

    Integer deleteGroupById(Integer id);

    Integer updateGroupById(Group group);

    Integer addCount(Map params);

    Group getGroupById(Integer id);

    List<Group> getGroups();

    List<Group> getGroupsByName(String name);
}
