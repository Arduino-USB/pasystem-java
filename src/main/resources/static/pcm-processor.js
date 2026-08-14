class PCMProcessor extends AudioWorkletProcessor {
    constructor() {
        super();
        // Queue for incoming playback bytes, or buffer for outgoing mic bytes
        this.queue = [];
        
        this.port.onmessage = (event) => {
            const ab = event.data;
            const int16 = new Int16Array(ab);
            const float32 = new Float32Array(int16.length);
            for (let i = 0; i < int16.length; i++) {
                float32[i] = int16[i] / 32768.0;
            }
            this.queue.push(float32);
        };
    }

    process(inputs, outputs, parameters) {
        const output = outputs[0][0];
        const channel = output;
        
        // Render audio smoothly block-by-block (128 samples per block natively)
        let filled = 0;
        while (this.queue.length > 0 && filled < channel.length) {
            let chunk = this.queue[0];
            let needed = channel.length - filled;
            
            if (chunk.length <= needed) {
                channel.set(chunk, filled);
                filled += chunk.length;
                this.queue.shift(); // remove processed chunk
            } else {
                channel.set(chunk.subarray(0, needed), filled);
                this.queue[0] = chunk.subarray(needed);
                filled += needed;
            }
        }
        
        // Fill remaining space with silence if buffer starves
        if (filled < channel.length) {
            channel.fill(0, filled);
        }

        return true;
    }
}

registerProcessor('pcm-processor', PCMProcessor);
