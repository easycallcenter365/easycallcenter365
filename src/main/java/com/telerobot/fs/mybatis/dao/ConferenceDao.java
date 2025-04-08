package com.telerobot.fs.mybatis.dao;

 import com.telerobot.fs.entity.bo.ConferenceEntity;


public interface ConferenceDao {

    int addConference(ConferenceEntity conference);

    int updateConference(ConferenceEntity conference);

    ConferenceEntity getConferenceByModerator(String moderator);

}
