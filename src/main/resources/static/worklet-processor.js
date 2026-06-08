class MicProcessor extends AudioWorkletProcessor {
	constructor() {
		super();
		this.frameSize = 960; // 20ms @ 48kHz
		this.buffer = new Int16Array(this.frameSize * 2);
		this.index = 0;
	}

	process(inputs) {
		const input = inputs[0];
		if (!input || !input[0]) return true;

		const left = input[0];
		const right = input[1] || input[0];

		for (let i = 0; i < left.length; i++) {
			let l = Math.max(-1, Math.min(1, left[i]));
			let r = Math.max(-1, Math.min(1, right[i]));

			this.buffer[this.index * 2] = l * 0x7fff;
			this.buffer[this.index * 2 + 1] = r * 0x7fff;
			this.index++;

			if (this.index >= 960) {
				// send full stable 20ms frame
				this.port.postMessage(this.buffer);
				this.index = 0;
			}
		}

		return true;
	}
}

registerProcessor("mic-processor", MicProcessor);