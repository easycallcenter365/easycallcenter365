package com.telerobot.fs.mybatis.dao;

import com.telerobot.fs.entity.bo.ConferenceEntity;
import com.telerobot.fs.mybatis.persistence.ConferenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConferenceDaoImpl  implements ConferenceDao  {

    @Autowired
    private ConferenceMapper mapper;

    @Override
    public int addConference(ConferenceEntity conference) {
       return   mapper.addConference(conference);
    }

    @Override
    public int updateConference(ConferenceEntity conference) {
        return mapper.updateConference(conference);
    }

    @Override
    public ConferenceEntity getConferenceByModerator(String moderator) {
        return mapper.getConferenceByModerator(moderator);
    }
}
