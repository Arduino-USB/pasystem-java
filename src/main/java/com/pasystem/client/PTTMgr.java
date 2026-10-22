package com.pasystem.client;

import com.pasystem.client.NetworkMgr;
import com.pasystem.client.GPIOMgr;
import com.pasystem.client.ByteMgr;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class PTTMgr implements WebSocket.Listener {
    private WebSocket webSocket;
    private volatile boolean running = true;
	
	private static boolean micOverride = false;
	

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        System.out.println("[PTTMgr] Connected");
        webSocket.request(1);

        // Background thread to poll PTT state and stream raw audio bytes when pressed
        new Thread(() -> {
            while (running) {
                try {
                    if (GPIOMgr.is_down("PTT") | micOverride) {
                        // Read raw audio chunk from your microphone source
                        byte[] chunk = MicMgr.pull(); // Adjust method name if needed for your mic manager
                        
                        if (chunk != null && chunk.length > 0 && this.webSocket != null) {
                            this.webSocket.sendBinary(ByteBuffer.wrap(chunk), true);
                        }
                    } else {
                        // Brief sleep to prevent busy-waiting CPU spin when not talking
                        Thread.sleep(20);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.out.println("[PTTMgr] Error in PTT loop: " + e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        // Doesn't receive anything, just keep the pipeline open
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        // Doesn't receive anything, just keep the pipeline open
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        System.out.println("[PTTMgr] Disconnected, " + reason);
        running = false;
        return null;
    }

	
	public static void micOn() {
		System.out.println("[PTTMgr] Setting mic to OVERRIDE");
		micOverride = true;
	}


	public static void micOff() {	
		System.out.println("[PTTMgr] Setting mic to NORMAL");
		micOverride = false;
	}

	public static boolean micStatus() {	
		return micOverride;
	}


	
}
