package com.telerobot.fs.mybatis.dao;

import com.telerobot.fs.entity.bo.ConferenceMemberRecord;
import com.telerobot.fs.mybatis.persistence.ConferenceMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConferenceMemberDaoImpl  implements  ConferenceMemberDao {

    @Autowired
    private ConferenceMemberMapper mapper;

    @Override
    public int insertConferenceMember(ConferenceMemberRecord conferenceMemberRecord) {
        return mapper.insertConferenceMember(conferenceMemberRecord);
    }

    @Override
    public int updateConferenceMember(ConferenceMemberRecord conferenceMemberRecord) {
        return mapper.updateConferenceMember(conferenceMemberRecord);
    }
}
