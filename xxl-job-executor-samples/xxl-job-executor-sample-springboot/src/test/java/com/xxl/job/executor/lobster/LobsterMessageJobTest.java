package com.xxl.job.executor.lobster;

import com.xxl.job.executor.jobhandler.LobsterMessageJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LobsterMessageJobTest {

	private LobsterMessageJob job;
	private FakeRepository repository;
	private FakeClient client;

	@BeforeEach
	public void setUp() {
		job = new LobsterMessageJob();
		repository = new FakeRepository();
		client = new FakeClient();
		ReflectionTestUtils.setField(job, "lobsterScheduleTaskRepository", repository);
		ReflectionTestUtils.setField(job, "lobsterMessageClient", client);
	}

	@Test
	public void handleTaskShouldFailWhenTaskNotFound() {
		assertEquals("lobster schedule task not found", job.handleTask("404"));
	}

	@Test
	public void handleTaskShouldSendMessageAndUpdateResult() {
		LobsterScheduleTask task = new LobsterScheduleTask();
		task.setId(6L);
		task.setUserId(7L);
		task.setLobsterId(8L);
		task.setMessageContent("run");
		task.setStatus(1);
		task.setDeleted(0);
		repository.task = task;

		assertNull(job.handleTask("6"));
		assertEquals(7L, client.request.getUserId());
		assertEquals(8L, client.request.getLobsterId());
		assertEquals("run", client.request.getMessage());
		assertEquals("SUCCESS", repository.status);
	}

	private static class FakeRepository implements LobsterScheduleTaskRepository {
		private LobsterScheduleTask task;
		private String status;

		@Override
		public LobsterScheduleTask loadById(long id) {
			return task;
		}

		@Override
		public void updateLastExecuteResult(long id, String status, String message) {
			this.status = status;
		}
	}

	private static class FakeClient implements LobsterMessageClient {
		private LobsterMessageRequest request;

		@Override
		public void send(LobsterMessageRequest request) {
			this.request = request;
		}
	}
}
