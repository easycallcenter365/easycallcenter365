package com.telerobot.fs.mybatis.persistence;

 import com.telerobot.fs.entity.bo.ConferenceEntity;
 //import com.telerobot.fs.wshandle.impl.Conference;


public interface ConferenceMapper {

    int addConference(ConferenceEntity conference);

    int updateConference(ConferenceEntity conference);

    ConferenceEntity getConferenceByModerator(String moderator);

}
