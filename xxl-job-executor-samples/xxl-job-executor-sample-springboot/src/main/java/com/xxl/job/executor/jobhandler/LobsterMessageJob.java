package com.xxl.job.executor.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.lobster.LobsterMessageClient;
import com.xxl.job.executor.lobster.LobsterMessageRequest;
import com.xxl.job.executor.lobster.LobsterScheduleTask;
import com.xxl.job.executor.lobster.LobsterScheduleTaskRepository;
import com.xxl.tool.core.StringTool;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LobsterMessageJob {
	private static final Logger logger = LoggerFactory.getLogger(LobsterMessageJob.class);

	@Resource
	private LobsterScheduleTaskRepository lobsterScheduleTaskRepository;
	@Resource
	private LobsterMessageClient lobsterMessageClient;

	@XxlJob("lobsterMessageJobHandler")
	public void lobsterMessageJobHandler() {
		String result = handleTask(XxlJobHelper.getJobParam());
		if (result == null) {
			XxlJobHelper.handleSuccess("lobster message sent");
		} else {
			XxlJobHelper.handleFail(result);
		}
	}

	public String handleTask(String jobParam) {
		if (StringTool.isBlank(jobParam) || !StringTool.isNumeric(jobParam.trim())) {
			return "scheduleTaskId invalid";
		}

		long scheduleTaskId = Long.parseLong(jobParam.trim());
		LobsterScheduleTask task = lobsterScheduleTaskRepository.loadById(scheduleTaskId);
		if (task == null) {
			logger.warn("lobster schedule task not found, scheduleTaskId={}", scheduleTaskId);
			return "lobster schedule task not found";
		}
		if (task.getDeleted() == 1 || task.getStatus() != 1) {
			logger.warn("lobster schedule task unavailable, scheduleTaskId={}, status={}, deleted={}",
					scheduleTaskId, task.getStatus(), task.getDeleted());
			return "lobster schedule task unavailable";
		}

		try {
			LobsterMessageRequest request = new LobsterMessageRequest(
					task.getUserId(),
					task.getLobsterId(),
					task.getMessageContent(),
					task.getId());
			lobsterMessageClient.send(request);
			lobsterScheduleTaskRepository.updateLastExecuteResult(scheduleTaskId, "SUCCESS", "lobster message sent");
			return null;
		} catch (Exception e) {
			logger.error("lobster message job failed, scheduleTaskId={}", scheduleTaskId, e);
			lobsterScheduleTaskRepository.updateLastExecuteResult(scheduleTaskId, "FAIL", e.getMessage());
			return e.getMessage();
		}
	}
}
