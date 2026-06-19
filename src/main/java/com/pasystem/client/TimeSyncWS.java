package com.pasystem.client;

import com.pasystem.client.FakeClock;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class TimeSyncWS implements WebSocket.Listener {
	private WebSocket webSocket;
	private FakeClock clock = new FakeClock();	

	@Override
	public void onOpen(WebSocket webSocket) {
		this.webSocket = webSocket;
		System.out.println("[TimeSyncWS] Connected");
		webSocket.request(1);	
	}

	@Override	
	public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
		clock.set_time(data.toString());
		
		webSocket.request(1);
		return null;	
	}
	
	
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		System.out.println("[TimeSyncWS] Disconnected, " + reason);
		return null;
	}
}
