package com.pasystem.client;

import com.pasystem.client.NetworkMgr;
import com.pasystem.client.ByteMgr;

import java.net.http.WebSocket;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class AudioRecvWS implements WebSocket.Listener {
	private WebSocket webSocket;
	private boolean spoken_to = false;
	
	private final ByteMgr byte_mgr = new ByteMgr();
	

	@Override
	public void onOpen(WebSocket webSocket) {
		this.webSocket = webSocket;
		System.out.println("[AudioRecvWS] Connected");
		webSocket.request(1);	
	}

	@Override	
	public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
		String targets = data.toString();
		String ip = NetworkMgr.get_ip();	
		if (targets.contains(ip)) {
			System.out.println("[AudioRecvWS] Server is communating with this node");
			spoken_to = true;
		} else {
			System.out.println("[AudioRecvWS] Server is not communcating with this node");
			spoken_to = false;	
		}
		
		webSocket.request(1);
		return null;	
	}
	
	@Override
	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		byte[] chunk = ByteMgr.to_bytes(data);
        
        // Feed the chunk and the 'last' flag directly to your manager
        byte[] fullFrame = byte_mgr.byte_combine(chunk, last);
        
        // If it's not null, we have the complete, perfectly aligned audio wave!
        if (fullFrame != null && spoken_to) {
            SpeakerMgr.push(fullFrame);
        }
		webSocket.request(1);
		return null;
			
	}
	
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		System.out.println("[AudioRecvWS] Disconnected, " + reason);
		return null;
	}
}
