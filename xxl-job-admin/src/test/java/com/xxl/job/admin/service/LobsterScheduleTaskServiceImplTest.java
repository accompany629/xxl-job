package com.xxl.job.admin.service;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.mapper.LobsterScheduleTaskMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.LobsterScheduleTask;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.dto.lobster.CreateLobsterScheduleRequest;
import com.xxl.job.admin.model.dto.lobster.UpdateLobsterScheduleRequest;
import com.xxl.job.admin.service.impl.LobsterScheduleTaskServiceImpl;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LobsterScheduleTaskServiceImplTest {

	private LobsterScheduleTaskServiceImpl service;
	@Mock
	private LobsterScheduleTaskMapper lobsterScheduleTaskMapper;
	@Mock
	private XxlJobService xxlJobService;
	@Mock
	private XxlJobInfoMapper xxlJobInfoMapper;

	private LoginInfo loginInfo;

	@BeforeEach
	public void setUp() {
		service = new LobsterScheduleTaskServiceImpl();
		ReflectionTestUtils.setField(service, "lobsterScheduleTaskMapper", lobsterScheduleTaskMapper);
		ReflectionTestUtils.setField(service, "xxlJobService", xxlJobService);
		ReflectionTestUtils.setField(service, "xxlJobInfoMapper", xxlJobInfoMapper);
		ReflectionTestUtils.setField(service, "lobsterJobGroup", 1);
		ReflectionTestUtils.setField(service, "lobsterJobAuthor", "lobster-schedule");

		loginInfo = new LoginInfo("7", "token");
		loginInfo.setUserName("tester");
		loginInfo.setRoleList(List.of(Consts.ADMIN_ROLE));
	}

	@Test
	public void createTaskShouldCreateBusinessTaskAndXxlJob() {
		CreateLobsterScheduleRequest request = new CreateLobsterScheduleRequest();
		request.setLobsterId(3L);
		request.setTaskName("feed lobster");
		request.setMessageContent("hello");
		request.setScheduleType("CRON");
		request.setCronExpr("0 0 12 * * ? *");

		doAnswer(invocation -> {
			LobsterScheduleTask task = invocation.getArgument(0);
			task.setId(11L);
			return 1;
		}).when(lobsterScheduleTaskMapper).save(any(LobsterScheduleTask.class));
		when(xxlJobService.add(any(XxlJobInfo.class), eq(loginInfo))).thenReturn(Response.ofSuccess("99"));
		when(xxlJobService.start(99, loginInfo)).thenReturn(Response.ofSuccess());
		LobsterScheduleTask saved = task(11L, 7L, 99);
		when(lobsterScheduleTaskMapper.loadById(11L)).thenReturn(saved);

		assertTrue(service.createTask(7L, loginInfo, request).isSuccess());

		ArgumentCaptor<XxlJobInfo> jobCaptor = ArgumentCaptor.forClass(XxlJobInfo.class);
		verify(xxlJobService).add(jobCaptor.capture(), eq(loginInfo));
		assertEquals("lobsterMessageJobHandler", jobCaptor.getValue().getExecutorHandler());
		assertEquals("11", jobCaptor.getValue().getExecutorParam());
		verify(lobsterScheduleTaskMapper).updateXxlJobId(11L, 99);
		verify(xxlJobService).start(99, loginInfo);
	}

	@Test
	public void listByUserIdShouldOnlyLoadCurrentUserTasks() {
		when(lobsterScheduleTaskMapper.listByUserId(7L)).thenReturn(List.of(task(1L, 7L, 2)));

		assertEquals(1, service.listByUserId(7L).getData().size());

		verify(lobsterScheduleTaskMapper).listByUserId(7L);
		verify(lobsterScheduleTaskMapper, never()).listByUserId(8L);
	}

	@Test
	public void updateTaskShouldRejectTaskFromAnotherUser() {
		UpdateLobsterScheduleRequest request = new UpdateLobsterScheduleRequest();
		request.setMessageContent("new message");
		when(lobsterScheduleTaskMapper.loadByIdAndUserId(1L, 7L)).thenReturn(null);

		assertFalse(service.updateTask(1L, 7L, loginInfo, request).isSuccess());

		verify(lobsterScheduleTaskMapper, never()).update(any(LobsterScheduleTask.class));
		verify(xxlJobService, never()).update(any(XxlJobInfo.class), eq(loginInfo));
	}

	@Test
	public void updateTaskShouldSyncXxlJobWhenCronChanged() {
		LobsterScheduleTask exists = task(1L, 7L, 9);
		exists.setScheduleType("CRON");
		exists.setCronExpr("0 0 12 * * ? *");
		when(lobsterScheduleTaskMapper.loadByIdAndUserId(1L, 7L)).thenReturn(exists);
		when(lobsterScheduleTaskMapper.update(any(LobsterScheduleTask.class))).thenReturn(1);
		XxlJobInfo jobInfo = new XxlJobInfo();
		jobInfo.setId(9);
		when(xxlJobInfoMapper.loadById(9)).thenReturn(jobInfo);
		when(xxlJobService.update(any(XxlJobInfo.class), eq(loginInfo))).thenReturn(Response.ofSuccess());

		UpdateLobsterScheduleRequest request = new UpdateLobsterScheduleRequest();
		request.setCronExpr("0 30 12 * * ? *");
		assertTrue(service.updateTask(1L, 7L, loginInfo, request).isSuccess());

		ArgumentCaptor<XxlJobInfo> jobCaptor = ArgumentCaptor.forClass(XxlJobInfo.class);
		verify(xxlJobService).update(jobCaptor.capture(), eq(loginInfo));
		assertEquals("0 30 12 * * ? *", jobCaptor.getValue().getScheduleConf());
	}

	private LobsterScheduleTask task(long id, long userId, Integer xxlJobId) {
		LobsterScheduleTask task = new LobsterScheduleTask();
		task.setId(id);
		task.setUserId(userId);
		task.setLobsterId(3L);
		task.setTaskName("task");
		task.setMessageContent("message");
		task.setScheduleType("CRON");
		task.setCronExpr("0 0 12 * * ? *");
		task.setTimezone("Asia/Shanghai");
		task.setXxlJobId(xxlJobId);
		task.setStatus(1);
		task.setDeleted(0);
		task.setCreatedAt(new Date());
		task.setUpdatedAt(new Date());
		return task;
	}
}
