package com.telerobot.fs.service;

import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
public class InboundDetailService {
    @Resource
    private JdbcTemplate jdbcTemplate;
    private final static Logger logger = LoggerFactory.getLogger(InboundDetailService.class);

    public void insertInbound(InboundDetail inbound) {
        String sql = "INSERT INTO `cc_inbound_cdr` (id, caller, callee, inbound_time, uuid, wav_file, group_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql,
                    inbound.getId(),
                    inbound.getCaller(),
                    inbound.getCallee(),
                    inbound.getInboundTime(),
                    inbound.getUuid(),
                    inbound.getWavFile(),
                    inbound.getGroupId()
            );
        }catch (Throwable e){
            logger.error("insertInbound error: {} {}", e.toString(), CommonUtils.getStackTraceString(e.getStackTrace()));
        }
    }

    // 更新数据
    public void updateInbound(InboundDetail inbound) {
        String sql = "UPDATE `cc_inbound_cdr` SET caller=?, callee=?, inbound_time=?, group_id=?, " +
                "answered_time=?, extnum=?, opnum=?, hangup_time=?, answered_time_len=?, " +
                "time_len=?, uuid=?, wav_file=? WHERE id=?";

        jdbcTemplate.update(sql,
                inbound.getCaller(),
                inbound.getCallee(),
                inbound.getInboundTime(),
                inbound.getGroupId(),
                inbound.getAnsweredTime(),
                inbound.getExtnum(),
                inbound.getOpnum(),
                inbound.getHangupTime(),
                inbound.getAnsweredTimeLen(),
                inbound.getTimeLen(),
                inbound.getUuid(),
                inbound.getWavFile(),
                inbound.getId()
        );
    }

    public void updateInbound(final List<InboundDetail> dataList) {
        String sql = "UPDATE `cc_inbound_cdr` SET caller=?, callee=?, inbound_time=?, group_id=?, " +
                "answered_time=?, extnum=?, opnum=?, hangup_time=?, answered_time_len=?, " +
                "time_len=?, uuid=?, wav_file=?, chat_content=?  WHERE id=?";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                InboundDetail inbound = dataList.get(i);

                preparedStatement.setString(1, inbound.getCaller());
                preparedStatement.setString(2, inbound.getCallee());
                preparedStatement.setLong(3, inbound.getInboundTime());
                preparedStatement.setString(4, inbound.getGroupId());
                preparedStatement.setLong(5, inbound.getAnsweredTime());
                preparedStatement.setString(6, inbound.getExtnum());
                preparedStatement.setString(7, inbound.getOpnum());
                preparedStatement.setLong(8, inbound.getHangupTime());
                preparedStatement.setLong(9, inbound.getAnsweredTimeLen());
                preparedStatement.setLong(10, inbound.getTimeLen());
                preparedStatement.setString(11, inbound.getUuid());
                preparedStatement.setString(12, inbound.getWavFile());
                preparedStatement.setString(13, inbound.getChatContent());
                preparedStatement.setString(14, inbound.getId());
            }

            @Override
            public int getBatchSize() {
                return dataList.size();
            }
        });
    }

}
