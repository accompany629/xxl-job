package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.constant.TriggerStatus;
import com.xxl.job.admin.mapper.LobsterScheduleTaskMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.LobsterScheduleTask;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.dto.lobster.CreateLobsterScheduleRequest;
import com.xxl.job.admin.model.dto.lobster.LobsterScheduleTaskVO;
import com.xxl.job.admin.model.dto.lobster.UpdateLobsterScheduleRequest;
import com.xxl.job.admin.scheduler.cron.CronExpression;
import com.xxl.job.admin.service.LobsterScheduleTaskService;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.job.admin.util.JobGroupPermissionUtil;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Service
public class LobsterScheduleTaskServiceImpl implements LobsterScheduleTaskService {
	private static final Logger logger = LoggerFactory.getLogger(LobsterScheduleTaskServiceImpl.class);

	public static final String SCHEDULE_TYPE_CRON = "CRON";
	public static final String SCHEDULE_TYPE_ONCE = "ONCE";
	public static final String EXECUTOR_HANDLER = "lobsterMessageJobHandler";

	@Resource
	private LobsterScheduleTaskMapper lobsterScheduleTaskMapper;
	@Resource
	private XxlJobService xxlJobService;
	@Resource
	private XxlJobInfoMapper xxlJobInfoMapper;

	@Value("${lobster.schedule.job-group:1}")
	private int lobsterJobGroup;

	@Value("${lobster.schedule.author:lobster-schedule}")
	private String lobsterJobAuthor;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Response<LobsterScheduleTaskVO> createTask(long userId, LoginInfo loginInfo, CreateLobsterScheduleRequest request) {
		String validMsg = validateCreate(request);
		if (validMsg != null) {
			return Response.ofFail(validMsg);
		}
		if (!JobGroupPermissionUtil.hasJobGroupPermission(loginInfo, lobsterJobGroup)) {
			return Response.ofFail(I18nUtil.getString("system_permission_limit"));
		}

		Date now = new Date();
		LobsterScheduleTask task = new LobsterScheduleTask();
		task.setUserId(userId);
		task.setLobsterId(request.getLobsterId());
		task.setTaskName(request.getTaskName());
		task.setMessageContent(request.getMessageContent());
		task.setScheduleType(normalizeScheduleType(request.getScheduleType()));
		task.setCronExpr(request.getCronExpr());
		task.setExecuteTime(request.getExecuteTime());
		task.setTimezone(defaultTimezone(request.getTimezone()));
		task.setStatus(1);
		task.setDeleted(0);
		task.setCreatedAt(now);
		task.setUpdatedAt(now);
		lobsterScheduleTaskMapper.save(task);

		XxlJobInfo jobInfo = buildXxlJobInfo(task, null);
		Response<String> addResponse = xxlJobService.add(jobInfo, loginInfo);
		if (addResponse == null || !addResponse.isSuccess()) {
			throw new IllegalStateException("create xxl-job failed: " + (addResponse == null ? "" : addResponse.getMsg()));
		}
		int xxlJobId = Integer.parseInt(addResponse.getData());
		lobsterScheduleTaskMapper.updateXxlJobId(task.getId(), xxlJobId);
		task.setXxlJobId(xxlJobId);

		Response<String> startResponse = xxlJobService.start(xxlJobId, loginInfo);
		if (startResponse == null || !startResponse.isSuccess()) {
			throw new IllegalStateException("start xxl-job failed: " + (startResponse == null ? "" : startResponse.getMsg()));
		}

		logger.info(">>>>>>>>>>> lobster schedule created, userId={}, taskId={}, xxlJobId={}", userId, task.getId(), xxlJobId);
		return Response.ofSuccess(LobsterScheduleTaskVO.from(lobsterScheduleTaskMapper.loadById(task.getId())));
	}

	@Override
	public Response<List<LobsterScheduleTaskVO>> listByUserId(long userId) {
		List<LobsterScheduleTaskVO> list = lobsterScheduleTaskMapper.listByUserId(userId).stream()
				.map(LobsterScheduleTaskVO::from)
				.toList();
		return Response.ofSuccess(list);
	}

	@Override
	public Response<LobsterScheduleTaskVO> getByIdAndUserId(long id, long userId) {
		LobsterScheduleTask task = lobsterScheduleTaskMapper.loadByIdAndUserId(id, userId);
		return task == null ? Response.ofFail("lobster schedule task not found") : Response.ofSuccess(LobsterScheduleTaskVO.from(task));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Response<LobsterScheduleTaskVO> updateTask(long id, long userId, LoginInfo loginInfo, UpdateLobsterScheduleRequest request) {
		LobsterScheduleTask exists = lobsterScheduleTaskMapper.loadByIdAndUserId(id, userId);
		if (exists == null) {
			return Response.ofFail("lobster schedule task not found");
		}

		String validMsg = validateUpdate(exists, request);
		if (validMsg != null) {
			return Response.ofFail(validMsg);
		}

		boolean scheduleChanged = isScheduleChanged(exists, request);
		mergeUpdate(exists, request);
		exists.setUpdatedAt(new Date());
		int affected = lobsterScheduleTaskMapper.update(exists);
		if (affected < 1) {
			return Response.ofFail("lobster schedule task update failed");
		}

		if (scheduleChanged) {
			syncXxlJob(exists, loginInfo);
		}
		logger.info(">>>>>>>>>>> lobster schedule updated, userId={}, taskId={}, scheduleChanged={}", userId, id, scheduleChanged);
		return Response.ofSuccess(LobsterScheduleTaskVO.from(lobsterScheduleTaskMapper.loadByIdAndUserId(id, userId)));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Response<String> enableTask(long id, long userId, LoginInfo loginInfo) {
		LobsterScheduleTask task = mustOwnedTask(id, userId);
		int affected = lobsterScheduleTaskMapper.updateStatus(id, userId, 1);
		if (affected < 1) {
			return Response.ofFail("lobster schedule task enable failed");
		}
		if (task.getXxlJobId() != null) {
			Response<String> response = xxlJobService.start(task.getXxlJobId(), loginInfo);
			if (response == null || !response.isSuccess()) {
				throw new IllegalStateException("start xxl-job failed: " + (response == null ? "" : response.getMsg()));
			}
		}
		return Response.ofSuccess();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Response<String> disableTask(long id, long userId, LoginInfo loginInfo) {
		LobsterScheduleTask task = mustOwnedTask(id, userId);
		int affected = lobsterScheduleTaskMapper.updateStatus(id, userId, 0);
		if (affected < 1) {
			return Response.ofFail("lobster schedule task disable failed");
		}
		if (task.getXxlJobId() != null) {
			Response<String> response = xxlJobService.stop(task.getXxlJobId(), loginInfo);
			if (response == null || !response.isSuccess()) {
				throw new IllegalStateException("stop xxl-job failed: " + (response == null ? "" : response.getMsg()));
			}
		}
		return Response.ofSuccess();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Response<String> deleteTaskSoft(long id, long userId, LoginInfo loginInfo) {
		LobsterScheduleTask task = mustOwnedTask(id, userId);
		int affected = lobsterScheduleTaskMapper.softDelete(id, userId);
		if (affected < 1) {
			return Response.ofFail("lobster schedule task delete failed");
		}
		if (task.getXxlJobId() != null) {
			Response<String> response = xxlJobService.remove(task.getXxlJobId(), loginInfo);
			if (response == null || !response.isSuccess()) {
				throw new IllegalStateException("remove xxl-job failed: " + (response == null ? "" : response.getMsg()));
			}
		}
		return Response.ofSuccess();
	}

	@Override
	public Response<String> updateLastExecuteResult(long id, String status, String message) {
		String safeMsg = message;
		if (safeMsg != null && safeMsg.length() > 512) {
			safeMsg = safeMsg.substring(0, 512);
		}
		int affected = lobsterScheduleTaskMapper.updateLastExecuteResult(id, status, safeMsg);
		return affected > 0 ? Response.ofSuccess() : Response.ofFail("lobster schedule task execute result update failed");
	}

	private LobsterScheduleTask mustOwnedTask(long id, long userId) {
		LobsterScheduleTask task = lobsterScheduleTaskMapper.loadByIdAndUserId(id, userId);
		if (task == null) {
			throw new IllegalArgumentException("lobster schedule task not found");
		}
		return task;
	}

	private void syncXxlJob(LobsterScheduleTask task, LoginInfo loginInfo) {
		if (task.getXxlJobId() == null) {
			throw new IllegalStateException("xxlJobId missing");
		}
		XxlJobInfo existsJob = xxlJobInfoMapper.loadById(task.getXxlJobId());
		if (existsJob == null) {
			throw new IllegalStateException("xxl-job not found: " + task.getXxlJobId());
		}
		XxlJobInfo updateJob = buildXxlJobInfo(task, existsJob);
		Response<String> response = xxlJobService.update(updateJob, loginInfo);
		if (response == null || !response.isSuccess()) {
			throw new IllegalStateException("update xxl-job failed: " + (response == null ? "" : response.getMsg()));
		}
	}

	private XxlJobInfo buildXxlJobInfo(LobsterScheduleTask task, XxlJobInfo base) {
		XxlJobInfo jobInfo = base == null ? new XxlJobInfo() : base;
		jobInfo.setJobGroup(lobsterJobGroup);
		jobInfo.setJobDesc(StringTool.isBlank(task.getTaskName()) ? "Lobster schedule task " + task.getId() : task.getTaskName());
		jobInfo.setAuthor(lobsterJobAuthor);
		jobInfo.setAlarmEmail("");
		jobInfo.setScheduleType("CRON");
		jobInfo.setScheduleConf(toXxlScheduleConf(task));
		jobInfo.setMisfireStrategy("DO_NOTHING");
		jobInfo.setExecutorRouteStrategy("FIRST");
		jobInfo.setExecutorHandler(EXECUTOR_HANDLER);
		jobInfo.setExecutorParam(String.valueOf(task.getId()));
		jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
		jobInfo.setExecutorTimeout(0);
		jobInfo.setExecutorFailRetryCount(0);
		jobInfo.setGlueType("BEAN");
		jobInfo.setGlueSource("");
		jobInfo.setGlueRemark("GLUE代码初始化");
		jobInfo.setChildJobId("");
		if (base == null) {
			jobInfo.setTriggerStatus(TriggerStatus.STOPPED.getValue());
			jobInfo.setTriggerLastTime(0);
			jobInfo.setTriggerNextTime(0);
		}
		return jobInfo;
	}

	private String validateCreate(CreateLobsterScheduleRequest request) {
		if (request == null) {
			return "request is empty";
		}
		return validateFields(request.getLobsterId(), request.getMessageContent(), request.getScheduleType(), request.getCronExpr(), request.getExecuteTime());
	}

	private String validateUpdate(LobsterScheduleTask exists, UpdateLobsterScheduleRequest request) {
		if (request == null) {
			return "request is empty";
		}
		Long lobsterId = request.getLobsterId() == null ? exists.getLobsterId() : request.getLobsterId();
		String messageContent = request.getMessageContent() == null ? exists.getMessageContent() : request.getMessageContent();
		String scheduleType = request.getScheduleType() == null ? exists.getScheduleType() : request.getScheduleType();
		String cronExpr = request.getCronExpr() == null ? exists.getCronExpr() : request.getCronExpr();
		Date executeTime = request.getExecuteTime() == null ? exists.getExecuteTime() : request.getExecuteTime();
		return validateFields(lobsterId, messageContent, scheduleType, cronExpr, executeTime);
	}

	private String validateFields(Long lobsterId, String messageContent, String scheduleType, String cronExpr, Date executeTime) {
		if (lobsterId == null || lobsterId <= 0) {
			return "lobsterId is required";
		}
		if (StringTool.isBlank(messageContent)) {
			return "messageContent is required";
		}
		if (StringTool.isBlank(scheduleType)) {
			return "scheduleType is required";
		}
		String normalized = normalizeScheduleType(scheduleType);
		if (SCHEDULE_TYPE_CRON.equals(normalized)) {
			if (StringTool.isBlank(cronExpr)) {
				return "cronExpr is required when scheduleType=CRON";
			}
			if (!CronExpression.isValidExpression(cronExpr)) {
				return "cronExpr is invalid";
			}
		} else if (SCHEDULE_TYPE_ONCE.equals(normalized)) {
			if (executeTime == null) {
				return "executeTime is required when scheduleType=ONCE";
			}
		} else {
			return "scheduleType is invalid";
		}
		return null;
	}

	private String normalizeScheduleType(String scheduleType) {
		return scheduleType == null ? null : scheduleType.trim().toUpperCase();
	}

	private void mergeUpdate(LobsterScheduleTask exists, UpdateLobsterScheduleRequest request) {
		if (request.getLobsterId() != null) {
			exists.setLobsterId(request.getLobsterId());
		}
		if (request.getTaskName() != null) {
			exists.setTaskName(request.getTaskName());
		}
		if (request.getMessageContent() != null) {
			exists.setMessageContent(request.getMessageContent());
		}
		if (request.getScheduleType() != null) {
			exists.setScheduleType(normalizeScheduleType(request.getScheduleType()));
		}
		if (request.getCronExpr() != null) {
			exists.setCronExpr(request.getCronExpr());
		}
		if (request.getExecuteTime() != null) {
			exists.setExecuteTime(request.getExecuteTime());
		}
		if (request.getTimezone() != null) {
			exists.setTimezone(defaultTimezone(request.getTimezone()));
		}
	}

	private boolean isScheduleChanged(LobsterScheduleTask exists, UpdateLobsterScheduleRequest request) {
		return request.getScheduleType() != null
				|| request.getCronExpr() != null
				|| request.getExecuteTime() != null;
	}

	private String toXxlScheduleConf(LobsterScheduleTask task) {
		if (SCHEDULE_TYPE_CRON.equals(normalizeScheduleType(task.getScheduleType()))) {
			return task.getCronExpr();
		}
		return toOnceCron(task.getExecuteTime(), task.getTimezone());
	}

	private String toOnceCron(Date executeTime, String timezone) {
		ZoneId zoneId = ZoneId.of(defaultTimezone(timezone));
		ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(executeTime.getTime()), zoneId);
		return String.format("%d %d %d %d %d ? %d",
				time.getSecond(),
				time.getMinute(),
				time.getHour(),
				time.getDayOfMonth(),
				time.getMonthValue(),
				time.getYear());
	}

	private String defaultTimezone(String timezone) {
		return StringTool.isBlank(timezone) ? ZoneId.systemDefault().getId() : timezone.trim();
	}
}
