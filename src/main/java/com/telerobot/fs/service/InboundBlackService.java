package com.telerobot.fs.service;

import com.telerobot.fs.entity.bo.InboundBlack;
import com.telerobot.fs.entity.bo.InboundDetail;
import com.telerobot.fs.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class InboundBlackService {
    @Resource
    private JdbcTemplate jdbcTemplate;
    private final static Logger logger = LoggerFactory.getLogger(InboundBlackService.class);

    public class InboundBlackRowMapper implements RowMapper<InboundBlack> {
        @Override
        public InboundBlack mapRow(ResultSet rs, int rowNum) throws SQLException {
            InboundBlack inboundBlack = new InboundBlack();
            inboundBlack.setId(rs.getInt("id"));
            inboundBlack.setCaller(rs.getString("caller"));
            inboundBlack.setExpiredTime(rs.getLong("expired_time"));
            inboundBlack.setAddTime(rs.getLong("add_time"));
            inboundBlack.setAddUser(rs.getString("add_user"));
            return inboundBlack;
        }
    }

    public void addInboundBlack(InboundBlack inboundBlack) {
        String sql = "INSERT INTO cc_inbound_black (caller, expired_time, add_time, add_user) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, inboundBlack.getCaller(), inboundBlack.getExpiredTime(), inboundBlack.getAddTime(), inboundBlack.getAddUser());
    }

    public void deleteInboundBlackById(int id) {
        String sql = "DELETE FROM cc_inbound_black WHERE id=?";
        jdbcTemplate.update(sql, id);
    }

    public void updateInboundBlack(InboundBlack inboundBlack) {
        String sql = "UPDATE cc_inbound_black SET caller=?, expired_time=?, add_time=?, add_user=? WHERE id=?";
        jdbcTemplate.update(sql, inboundBlack.getCaller(), inboundBlack.getExpiredTime(), inboundBlack.getAddTime(), inboundBlack.getAddUser(), inboundBlack.getId());
    }

    public InboundBlack getInboundBlackByCaller(String caller) {
        String sql = "SELECT * FROM cc_inbound_black WHERE caller=? and expired_time > ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{caller, System.currentTimeMillis()}, new InboundBlackRowMapper());
        }
        catch (EmptyResultDataAccessException e) {
            return null; // 返回null表示没有找到记录
        }
    }

    public List<InboundBlack> getAllInboundBlacks() {
        String sql = "SELECT * FROM cc_inbound_black";
        return jdbcTemplate.query(sql, new InboundBlackRowMapper());
    }

}
