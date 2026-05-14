package com.xxl.job.executor.lobster;

public class LobsterMessageRequest {

	private long userId;
	private long lobsterId;
	private String message;
	private long scheduleTaskId;

	public LobsterMessageRequest() {
	}

	public LobsterMessageRequest(long userId, long lobsterId, String message, long scheduleTaskId) {
		this.userId = userId;
		this.lobsterId = lobsterId;
		this.message = message;
		this.scheduleTaskId = scheduleTaskId;
	}

	public long getUserId() { return userId; }
	public void setUserId(long userId) { this.userId = userId; }
	public long getLobsterId() { return lobsterId; }
	public void setLobsterId(long lobsterId) { this.lobsterId = lobsterId; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	public long getScheduleTaskId() { return scheduleTaskId; }
	public void setScheduleTaskId(long scheduleTaskId) { this.scheduleTaskId = scheduleTaskId; }
}
