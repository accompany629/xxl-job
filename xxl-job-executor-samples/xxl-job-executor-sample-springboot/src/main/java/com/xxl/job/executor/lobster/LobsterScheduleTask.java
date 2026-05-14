package com.xxl.job.executor.lobster;

public class LobsterScheduleTask {

	private long id;
	private long userId;
	private long lobsterId;
	private String messageContent;
	private int status;
	private int deleted;

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

	public String getMessageContent() {
		return messageContent;
	}

	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
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
}
