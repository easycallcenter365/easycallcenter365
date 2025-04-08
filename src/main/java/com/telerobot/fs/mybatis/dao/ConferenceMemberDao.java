package com.telerobot.fs.mybatis.dao;

import com.telerobot.fs.entity.bo.ConferenceMemberRecord;

public interface ConferenceMemberDao {
    int insertConferenceMember(ConferenceMemberRecord conferenceMemberRecord);

    int updateConferenceMember(ConferenceMemberRecord conferenceMemberRecord);
}
