package com.xxl.job.executor.lobster;

import com.xxl.tool.json.GsonTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class HttpLobsterMessageClient implements LobsterMessageClient {

	@Value("${lobster.message.api-url}")
	private String apiUrl;
	@Value("${lobster.message.connect-timeout:3000}")
	private int connectTimeout;
	@Value("${lobster.message.read-timeout:5000}")
	private int readTimeout;
	@Value("${lobster.message.auth-token:}")
	private String authToken;

	@Override
	public void send(LobsterMessageRequest request) {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(apiUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(readTimeout);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			if (authToken != null && !authToken.trim().isEmpty()) {
				connection.setRequestProperty("Authorization", "Bearer " + authToken.trim());
			}

			byte[] body = GsonTool.toJson(request).getBytes(StandardCharsets.UTF_8);
			try (OutputStream outputStream = connection.getOutputStream()) {
				outputStream.write(body);
			}

			int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				throw new IllegalStateException("lobster message api failed, responseCode=" + responseCode);
			}
		} catch (Exception e) {
			throw new IllegalStateException("send lobster message failed", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
