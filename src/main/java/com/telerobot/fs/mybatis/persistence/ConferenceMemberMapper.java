package com.telerobot.fs.mybatis.persistence;

import com.telerobot.fs.entity.bo.ConferenceMemberRecord;

public interface ConferenceMemberMapper {
    int insertConferenceMember(ConferenceMemberRecord conferenceMemberRecord);

    int updateConferenceMember(ConferenceMemberRecord conferenceMemberRecord);
}
