package com.xxl.job.executor.lobster;

public interface LobsterScheduleTaskRepository {

	LobsterScheduleTask loadById(long id);

	void updateLastExecuteResult(long id, String status, String message);
}
