package com.xxl.job.admin.model;

import java.util.Date;

public class LobsterScheduleTask {

	private long id;
	private long userId;
	private long lobsterId;
	private String taskName;
	private String messageContent;
	private String scheduleType;
	private String cronExpr;
	private Date executeTime;
	private String timezone;
	private Integer xxlJobId;
	private int status;
	private int deleted;
	private Date lastExecuteTime;
	private String lastExecuteStatus;
	private String lastExecuteMsg;
	private Date createdAt;
	private Date updatedAt;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getLobsterId() {
		return lobsterId;
	}

	public void setLobsterId(long lobsterId) {
		this.lobsterId = lobsterId;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getMessageContent() {
		return messageContent;
	}

	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}

	public String getScheduleType() {
		return scheduleType;
	}

	public void setScheduleType(String scheduleType) {
		this.scheduleType = scheduleType;
	}

	public String getCronExpr() {
		return cronExpr;
	}

	public void setCronExpr(String cronExpr) {
		this.cronExpr = cronExpr;
	}

	public Date getExecuteTime() {
		return executeTime;
	}

	public void setExecuteTime(Date executeTime) {
		this.executeTime = executeTime;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Integer getXxlJobId() {
		return xxlJobId;
	}

	public void setXxlJobId(Integer xxlJobId) {
		this.xxlJobId = xxlJobId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	public Date getLastExecuteTime() {
		return lastExecuteTime;
	}

	public void setLastExecuteTime(Date lastExecuteTime) {
		this.lastExecuteTime = lastExecuteTime;
	}

	public String getLastExecuteStatus() {
		return lastExecuteStatus;
	}

	public void setLastExecuteStatus(String lastExecuteStatus) {
		this.lastExecuteStatus = lastExecuteStatus;
	}

	public String getLastExecuteMsg() {
		return lastExecuteMsg;
	}

	public void setLastExecuteMsg(String lastExecuteMsg) {
		this.lastExecuteMsg = lastExecuteMsg;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
}
