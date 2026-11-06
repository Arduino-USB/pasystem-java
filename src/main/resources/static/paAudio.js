/* /static/paAudio.js - Background Audio & Alert Engine */
(function() {
    let audioContextInitialized = false;
    let audioCtx = null;
    let pcmWorkletNode = null;
    let audioIoWs = null;
    let speakWs = null;
    let activeAlertsMap = new Map();

    // Define helpers globally
    window.getAudioCtx = () => audioCtx;
    window.getSpeakWs = () => speakWs;
    window.sendIPSelection = (targetIPs) => {
        if (speakWs && speakWs.readyState === WebSocket.OPEN) {
            console.log("[paAudio Client] Sending target IPs update:", targetIPs);
            speakWs.send("__IPS__ " + targetIPs.join(" "));
        } else {
            console.warn("[paAudio Client] /speak WebSocket is not open yet.");
        }
    };

    async function checkMicrophonePermissionState() {
        try {
            // Check browser permission state cleanly without triggering a prompt
            if (navigator.permissions && navigator.permissions.query) {
                const permissionStatus = await navigator.permissions.query({ name: 'microphone' });
                if (permissionStatus.state === 'denied' || permissionStatus.state === 'prompt') {
                    return false; // Requires prompt or is denied, so show popup first
                }
                if (permissionStatus.state === 'already-granted' || permissionStatus.state === 'granted') {
                    return true;
                }
            }
            return false; // Default to showing popup if state cannot be verified safely
        } catch (e) {
            return false;
        }
    }

    async function initAudioEngine() {
        console.log("[paAudio Client] Initializing Audio Engine...");
        try {
            if (!audioCtx || audioCtx.state === 'closed') {
                audioCtx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 48000 });
            }
            
            if (audioCtx.state === 'suspended') {
                console.log("[paAudio Client] AudioContext is suspended. Attempting to resume...");
                const resumePromise = audioCtx.resume();
                await Promise.race([
                    resumePromise,
                    new Promise(resolve => setTimeout(resolve, 100))
                ]);
            }

            if (audioCtx.state === 'running') {
                if (!pcmWorkletNode) {
                    await audioCtx.audioWorklet.addModule('/static/pcm-processor.js');
                    pcmWorkletNode = new AudioWorkletNode(audioCtx, 'pcm-processor');
                    pcmWorkletNode.connect(audioCtx.destination);
                }
                audioContextInitialized = true;
                console.log("[paAudio Client] Audio Engine successfully running.");
                return true;
            } else {
                console.warn("[paAudio Client] AudioEngine state is still:", audioCtx.state);
                audioContextInitialized = false;
                return false;
            }
        } catch (e) {
            console.error("[paAudio Client] Failed to initialize Audio Engine:", e);
            audioContextInitialized = false;
            return false;
        }
    }

    function initWebSockets() {
        const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
        const host = window.location.host;

        function connectAudioIo() {
            console.log("[paAudio Client] Connecting to /audio_io WebSocket...");
            audioIoWs = new WebSocket(protocol + host + '/audio_io');
            audioIoWs.binaryType = 'arraybuffer';

            audioIoWs.onopen = () => {
                console.log("[paAudio Client] Connected to /audio_io successfully.");
                audioIoWs.send("GET_AUDIO_STREAM");
            };
            audioIoWs.onmessage = (event) => {
                if (typeof event.data === 'string') {
                    try { renderSpeakers(JSON.parse(event.data)); } catch(e) {}
                } else {
                    playRawBytes(event.data);
                }
            };
            audioIoWs.onclose = () => { setTimeout(connectAudioIo, 2000); };
            audioIoWs.onerror = () => { audioIoWs.close(); };
        }

        function connectSpeak() {
            console.log("[paAudio Client] Connecting to /speak WebSocket...");
            speakWs = new WebSocket(protocol + host + '/speak');
            speakWs.onopen = () => {
                console.log("[paAudio Client] Connected to /speak successfully.");
            };
            speakWs.onclose = () => { setTimeout(connectSpeak, 2000); };
            speakWs.onerror = () => { speakWs.close(); };
        }

        connectAudioIo();
        connectSpeak();
    }

    function playRawBytes(arrayBuffer) {
        if (audioCtx && audioCtx.state === 'suspended') {
            audioCtx.resume();
        }
        if (!audioContextInitialized || !audioCtx || !pcmWorkletNode) {
            if (audioCtx && audioCtx.state === 'running' && !pcmWorkletNode) {
                initAudioEngine();
            }
            return;
        }
        pcmWorkletNode.port.postMessage(arrayBuffer, [arrayBuffer]);
    }

    function renderSpeakers(speakersList) {
        const container = document.getElementById('speakerContainer');
        if (!container) return;
        container.innerHTML = '';
        speakersList.forEach(speaker => {
            const tag = document.createElement('div');
            tag.className = 'speakerTag';
            tag.innerHTML = `<img src="/static/speaker.svg" alt="spk"> <span>${speaker.name || speaker.ip}</span>`;
            container.appendChild(tag);
        });
    }

    async function checkAlerts() {
        try {
            const res = await fetch('/get_all_alerts');
            const data = await res.json();
            data.forEach(item => {
                const ip = item.ip;
                item.alerts.forEach(alertStr => {
                    const uniqueKey = `${ip}_${alertStr}`;
                    if (!activeAlertsMap.has(uniqueKey)) {
                        triggerAlertResponse(ip, alertStr, uniqueKey);
                    }
                });
            });
        } catch(e) {}
    }

    function triggerAlertResponse(ip, alertStr, uniqueKey) {
        console.log(`[paAudio Client] Triggering alert response for ${alertStr} from IP: ${ip}`);
        let audio = new Audio(`/static/${alertStr}.mp3`);
        audio.loop = true;
        audio.play().catch(() => {
            audio = new Audio(`/stored/${alertStr}.mp3`);
            audio.loop = true;
            audio.play().catch(() => {});
        });

        const btnConfig = {};
        btnConfig["DISARM"] = async () => {
            audio.pause();
            activeAlertsMap.delete(uniqueKey);
            try {
                await fetch('/remove_client_alert', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ ip: ip, alert: alertStr })
                });
            } catch(e) {}
        };

        const modalHandle = showNotification(
            `Alert Raised`,
            `Device IP: ${ip} has triggered code: "${alertStr}"`,
            btnConfig,
            true
        );

        activeAlertsMap.set(uniqueKey, { audio: audio, modal: modalHandle });
    }

    // Helper function to display the popup if user intervention is required
    function showAudioOverlay(customErrorMessage = null) {
        let overlay = document.getElementById('audioOverlay');
        
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'audioOverlay';
            overlay.style.cssText = 'display: flex; position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0, 0, 0, 0.85); z-index: 999999; justify-content: center; align-items: center; flex-direction: column; font-family: sans-serif; color: white;';
            overlay.innerHTML = `
                <div style="background: #1e1e1e; padding: 30px; border-radius: 12px; text-align: center; box-shadow: 0 10px 25px rgba(0,0,0,0.5); max-width: 400px; border: 1px solid #333;">
                    <h2 style="margin-top: 0; color: #fff;">Audio Initialization Required</h2>
                    <p style="color: #bbb; font-size: 14px; line-height: 1.5;">The browser requires a user interaction to start the Kiosk audio engine and microphone stream.</p>
                    <button id="enableAudioBtn" style="background: #007bff; color: white; border: none; padding: 12px 24px; font-size: 16px; font-weight: bold; border-radius: 6px; cursor: pointer; margin-top: 15px; width: 100%;">Enable Sound</button>
                    <p id="permErrorMsg" style="color: #ff4d4d; font-size: 13px; display: none; margin-top: 10px;"></p>
                </div>
            `;
            document.body.appendChild(overlay);
        } else {
            overlay.style.display = 'flex';
        }

        const errorMsg = document.getElementById('permErrorMsg');
        if (errorMsg) {
            if (customErrorMessage) {
                errorMsg.innerText = customErrorMessage;
                errorMsg.style.display = 'block';
            } else {
                errorMsg.style.display = 'none';
            }
        }

        const enableBtn = document.getElementById('enableAudioBtn');
        if (enableBtn) {
            enableBtn.onclick = async () => {
                try {
                    // 1. Request microphone permission safely inside the click gesture
                    const tempStream = await navigator.mediaDevices.getUserMedia({ audio: true });
                    tempStream.getTracks().forEach(t => t.stop());

                    // 2. Initialize and start the audio engine
                    const audioSuccess = await initAudioEngine();
                    if (!audioSuccess) {
                        showAudioOverlay("AudioContext failed to start. Please try again.");
                        return;
                    }

                    // Success! Hide popup and start background services
                    overlay.style.display = 'none';
                    console.log("[paAudio Client] User gesture captured. Engine active & WebSockets starting.");
                    initWebSockets();
                    setInterval(checkAlerts, 1000);
                } catch(e) {
                    console.error("[paAudio Client] Microphone permission denied:", e);
                    showAudioOverlay("Microphone permission was denied. Please allow access to proceed.");
                }
            };
        }
    }

    // DOM Load: Try starting audio engine automatically first. Show popup ONLY if it fails or mic permissions aren't pre-granted.
    document.addEventListener('DOMContentLoaded', async () => {
        const micAlreadyGranted = await checkMicrophonePermissionState();
        const audioSuccess = await initAudioEngine();

        if (micAlreadyGranted && audioSuccess) {
            console.log("[paAudio Client] Pre-granted permissions & Audio Engine running automatically. Starting WebSockets...");
            initWebSockets();
            setInterval(checkAlerts, 1000);
        } else {
            console.warn("[paAudio Client] Automatic audio or mic check pending user gesture. Displaying popup.");
            let reason = "Audio setup required.";
            if (!micAlreadyGranted) {
                reason = "Microphone permission is required for PTT.";
            } else if (!audioSuccess) {
                reason = "AudioContext is suspended by browser policy.";
            }
            showAudioOverlay(reason);
        }
    });
})();
