package com.pasystem.client;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
public class ByteMgr {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /**
     * Processes an incoming byte fragment.
     * * @param last True if this is the final fragment of the message, false otherwise.
     * @param chunk The raw byte fragment received from the network.
     * @return The complete, assembled byte array if 'last' is true; null otherwise.
     */
	public static byte[] to_bytes(ByteBuffer buffer) {
        byte[] data = new byte[buffer.remaining()];
        buffer.slice().get(data);
        return data;
    }

    public synchronized byte[] byte_combine(byte[] chunk, boolean last) {
        if (chunk == null || chunk.length == 0) {
            return null;
        }

        try {
            // Append the incoming chunk to our temporary buffer
            buffer.write(chunk);
        } catch (Exception e) {
            System.err.println("[ByteMgr] Error writing to buffer: " + e.getMessage());
            return null;
        }

        // If this is the last fragment, compile and return the full array
        if (last) {
            byte[] fullBytes = buffer.toByteArray();
            buffer.reset(); // Wipe the buffer clean for the next incoming sequence
            return fullBytes;
        }

        // More pieces are still on the way
        return null;
    }
}
