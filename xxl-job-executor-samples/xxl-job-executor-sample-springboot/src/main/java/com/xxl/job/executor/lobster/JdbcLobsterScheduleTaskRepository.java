package com.xxl.job.executor.lobster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Repository
public class JdbcLobsterScheduleTaskRepository implements LobsterScheduleTaskRepository {

	@Value("${lobster.schedule.datasource.url}")
	private String url;
	@Value("${lobster.schedule.datasource.username}")
	private String username;
	@Value("${lobster.schedule.datasource.password}")
	private String password;

	@Override
	public LobsterScheduleTask loadById(long id) {
		String sql = "SELECT id, user_id, lobster_id, message_content, status, deleted FROM lobster_schedule_task WHERE id = ?";
		try (Connection connection = DriverManager.getConnection(url, username, password);
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, id);
			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				LobsterScheduleTask task = new LobsterScheduleTask();
				task.setId(rs.getLong("id"));
				task.setUserId(rs.getLong("user_id"));
				task.setLobsterId(rs.getLong("lobster_id"));
				task.setMessageContent(rs.getString("message_content"));
				task.setStatus(rs.getInt("status"));
				task.setDeleted(rs.getInt("deleted"));
				return task;
			}
		} catch (Exception e) {
			throw new IllegalStateException("load lobster schedule task failed", e);
		}
	}

	@Override
	public void updateLastExecuteResult(long id, String status, String message) {
		String safeMsg = message;
		if (safeMsg != null && safeMsg.length() > 512) {
			safeMsg = safeMsg.substring(0, 512);
		}
		String sql = "UPDATE lobster_schedule_task SET last_execute_time = now(), last_execute_status = ?, last_execute_msg = ?, updated_at = now() WHERE id = ?";
		try (Connection connection = DriverManager.getConnection(url, username, password);
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, status);
			statement.setString(2, safeMsg);
			statement.setLong(3, id);
			statement.executeUpdate();
		} catch (Exception e) {
			throw new IllegalStateException("update lobster schedule task execute result failed", e);
		}
	}
}
