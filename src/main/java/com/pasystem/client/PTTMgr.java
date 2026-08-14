package com.pasystem.client;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletionStage;

public class PTTMgr implements WebSocket.Listener {

	private WebSocket webSocket;

	private volatile boolean open = false;

	// LATCHED push-to-talk state
	private volatile boolean pttActive = false;

	// Audio being recorded and sent to server
	private final BlockingQueue<byte[]> audioQueue =
			new LinkedBlockingQueue<>(100);

	// Audio received from server and waiting to be played
	private final BlockingQueue<byte[]> receivedAudioQueue =
			new LinkedBlockingQueue<>(100);


	@Override
	public void onOpen(WebSocket webSocket) {
		this.webSocket = webSocket;
		this.open = true;

		System.out.println("[PTTMgr] Connected");

		startPTTWatcher();
		startMic();
		startSender();
		startReceiver();
	}


	/**
	 * WATCH GPIO PTT BUTTON
	 *
	 * Keeps track of the current latched PTT state.
	 */
	private void startPTTWatcher() {
		new Thread(() -> {

			boolean lastState = false;

			while (open) {
				try {

					boolean currentState = GPIOMgr.is_down("PTT");

					if (currentState && !lastState) {
						pttActive = true;
						System.out.println("[PTTMgr] PTT ON");
					}

					if (!currentState && lastState) {
						pttActive = false;
						System.out.println("[PTTMgr] PTT OFF");
					}

					lastState = currentState;

					Thread.sleep(5);

				} catch (Exception e) {
					System.out.println(
						"[PTTMgr][PTTWatcher] " + e
					);
				}
			}

		}, "ptt-gpio-thread").start();
	}


	/**
	 * MIC THREAD
	 *
	 * Only records audio while PTT is active.
	 */
	private void startMic() {
		new Thread(() -> {

			while (open) {
				try {

					if (!pttActive) {
						Thread.sleep(20);
						continue;
					}

					byte[] chunk = MicMgr.pull();

					if (chunk == null) {
						continue;
					}

					if (!audioQueue.offer(chunk)) {
						audioQueue.poll();
						audioQueue.offer(chunk);
					}

				} catch (Exception e) {
					System.out.println(
						"[PTTMgr][Mic] " + e
					);
				}
			}

		}, "ptt-mic-thread").start();
	}


	/**
	 * NETWORK SENDER
	 *
	 * Takes microphone audio from the queue and sends it
	 * to the server.
	 */
	private void startSender() {
		new Thread(() -> {

			while (open && webSocket != null) {
				try {

					if (!pttActive) {
						Thread.sleep(10);
						continue;
					}

					byte[] audio = audioQueue.take();

					webSocket.sendBinary(
						ByteBuffer.wrap(audio),
						true
					);

				} catch (Exception e) {
					System.out.println(
						"[PTTMgr][Sender] " + e
					);

					open = false;
				}
			}

		}, "ptt-sender-thread").start();
	}


	/**
	 * RECEIVED AUDIO THREAD
	 *
	 * This is the client-side equivalent of the server's
	 * GlobalSoundQueue.poll(session) + sendMessage().
	 *
	 * WebSocket callbacks put received audio into the queue.
	 * This thread removes it and gives it to SpeakerMgr.
	 */
	private void startReceiver() {
		new Thread(() -> {

			System.out.println("[PTTMgr] Starting audio receiver");

			while (open) {
				try {

					byte[] audio = receivedAudioQueue.take();

					/*
					 * Send received audio to the speaker.
					 *
					 * Change SpeakerMgr.push(audio) to whatever
					 * method your SpeakerMgr uses for PCM playback.
					 */
					SpeakerMgr.push(audio);

				} catch (Exception e) {
					System.out.println(
						"[PTTMgr][Receiver] " + e
					);
				}
			}

			System.out.println("[PTTMgr] Audio receiver stopped");

		}, "ptt-receiver-thread").start();
	}


	/**
	 * RECEIVED BINARY AUDIO
	 *
	 * WebSocket calls this whenever the server sends a
	 * BinaryMessage.
	 */
	@Override
	public CompletionStage<?> onBinary(
			WebSocket webSocket,
			ByteBuffer data,
			boolean last) {

		try {

			/*
			 * The server sends complete audio chunks as a
			 * single BinaryMessage, so we only need to process
			 * completed messages.
			 */
			if (!last) {
				System.out.println(
					"[PTTMgr] Received fragmented audio packet"
				);

				return WebSocket.Listener.super.onBinary(
					webSocket,
					data,
					last
				);
			}

			byte[] audio = new byte[data.remaining()];
			data.get(audio);

			/*
			 * Keep the callback lightweight.
			 *
			 * Do NOT play the audio directly from the
			 * WebSocket callback.
			 */
			if (!receivedAudioQueue.offer(audio)) {

				/*
				 * Queue is full.
				 *
				 * Drop the oldest audio packet rather than
				 * allowing latency to grow indefinitely.
				 */
				receivedAudioQueue.poll();
				receivedAudioQueue.offer(audio);
			}

		} catch (Exception e) {
			System.out.println(
				"[PTTMgr][Receive] " + e
			);
		}

		return WebSocket.Listener.super.onBinary(
			webSocket,
			data,
			last
		);
	}


	/**
	 * RECEIVED TEXT MESSAGE
	 *
	 * The server can send control information such as the
	 * current IP list.
	 */
	@Override
	public CompletionStage<?> onText(
			WebSocket webSocket,
			CharSequence data,
			boolean last) {

		try {

			if (last) {
				System.out.println(
					"[PTTMgr] Received text: " + data
				);

				/*
				 * If this is your IP list, handle it here.
				 *
				 * Example:
				 *
				 * NetworkMgr.updateIPList(data.toString());
				 */
			}

		} catch (Exception e) {
			System.out.println(
				"[PTTMgr][Text] " + e
			);
		}

		return WebSocket.Listener.super.onText(
			webSocket,
			data,
			last
		);
	}


	/**
	 * CONNECTION CLOSED
	 */
	@Override
	public CompletionStage<?> onClose(
			WebSocket webSocket,
			int statusCode,
			String reason) {

		System.out.println(
			"[PTTMgr] Connection closed: "
			+ statusCode
			+ " "
			+ reason
		);

		open = false;
		pttActive = false;
		this.webSocket = null;

		return WebSocket.Listener.super.onClose(
			webSocket,
			statusCode,
			reason
		);
	}


	/**
	 * WEBSOCKET ERROR
	 */
	@Override
	public void onError(
			WebSocket webSocket,
			Throwable error) {

		System.out.println(
			"[PTTMgr] WebSocket error: " + error
		);

		open = false;
		pttActive = false;
		this.webSocket = null;
	}
}
