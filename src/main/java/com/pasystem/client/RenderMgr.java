package com.pasystem.client;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.util.concurrent.TimeUnit;



import java.io.*;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class RenderMgr {

	

	private static Path root = FileSystems.getDefault().getRootDirectories().iterator().next();

	private static Path ptyApiSocket = root.resolve("tmp").resolve("pty_api.sock");
	private static Path ptyDisplaySocket = root.resolve("tmp").resolve("pty_display.sock");

	private static boolean instanceCreated = false;
	//assumes the pty_api.sh is RUNNING.	


	private static String writeString(String stringInput, Path socketPath) {
		try {
			UnixDomainSocketAddress address =
				UnixDomainSocketAddress.of(socketPath);

			try (SocketChannel socket = SocketChannel.open(address)) {

				ByteBuffer output =
					StandardCharsets.UTF_8.encode(stringInput + "\n");

				while (output.hasRemaining()) {
					socket.write(output);
				}

				socket.configureBlocking(false);

				ByteBuffer input = ByteBuffer.allocate(4096);

				long deadline = System.nanoTime() +
					TimeUnit.MILLISECONDS.toNanos(100);

				while (System.nanoTime() < deadline) {
					int n = socket.read(input);

					if (n > 0) {
						input.flip();

						return StandardCharsets.UTF_8
							.decode(input)
							.toString()
							.strip();
					}

					if (n == -1) {
						return null;
					}

					Thread.yield();
				}

				return null;
			}

		} catch (Exception e) {
			System.err.println(
				"Socket error (" + socketPath + "): " + e
			);

			return null;
		}
	}

/*	
	public class Dims {
		public int width;
		public int height;	
	}
*/	
	public static class Image {
		
		public Image(SixelImage image, int xPercent, int uPercent) {
			this.image = image;			
			this.xPercent = xPercent;
			this.yPercent = yPercent;
		}	
		
		public SixelImage image;
		public int xPercent;
		public int yPercent;
	}
	
	private static List<Integer> parseANSI(String response) {
		List<Integer> values = new ArrayList<>();

		int start = response.indexOf('[') + 1;
		int end = response.indexOf('t', start);

		if (start <= 0 || end == -1)
			return values;

		String[] parts = response.substring(start, end).split(";");

		for (int i = 1; i < parts.length; i++) {
			values.add(Integer.parseInt(parts[i]));
		}

		return values;
	}
	
	private static List<Image> imagesList = new ArrayList<>();
	private static int currentX;
	private static int currentY;
		

	public RenderMgr() {
		// handles redraw logic on image screen resize;    
		
		if (instanceCreated) {
		    System.out.println("[RenderMgr] Only one instance of RenderMgr allowed, then it is meant to be used as a static utility");
			return;
		}        
		
		instanceCreated = true;
		
		new Thread(() -> {
		    int lastObservedX = -1;
		    int lastObservedY = -1;
		    long lastChangeTime = 0;
		    final long DEBOUNCE_DELAY_MS = 1000; // 1 second wait

		    while (true) {
		        try {
		            Thread.sleep(200);
		        } catch (Exception e) {
		            System.out.println("[RenderMgr] Sleep error, " + e);                
		        } 
		    
		        if (ptyApiSocket.toFile().exists()) {
		            String terminalSize = writeString("ANSI:\033[14t", ptyApiSocket);
		            if (terminalSize == null) { 
		                continue;        
		            }
		            List<Integer> newRes = parseANSI(terminalSize);
		            if (newRes.size() < 2) continue;
		            
		            int newX = newRes.get(0);
		            int newY = newRes.get(1);                

					// Check if resolution is currently fluctuating
					if (newX != lastObservedX || newY != lastObservedY) {
						lastObservedX = newX;
						lastObservedY = newY;
						lastChangeTime = System.currentTimeMillis(); // Reset debounce timer
					} 
					// If it has stabilized for 1 second
					else if ((System.currentTimeMillis() - lastChangeTime) >= DEBOUNCE_DELAY_MS) {
						// Prevent rendering from 0,0 on startup
						if (currentX == 0 || currentY == 0) {
							currentX = lastObservedX;
							currentY = lastObservedY;
							System.out.println("[RenderMgr] Initial terminal resolution captured: " + currentX + ", " + currentY);
						}
						else if (currentX != lastObservedX || currentY != lastObservedY) {
							System.out.println("[RenderMgr] Terminal resolution stabilized, updating to " + lastObservedX + ", " + lastObservedY);
							renderScreen(currentX, currentY, lastObservedX, lastObservedY);
							currentX = lastObservedX;
							currentY = lastObservedY;
						}
					}
		            
		        } else {
		            System.out.println("[RenderMgr] Socket doesnt exist!");        
		        }
		    }
		}).start();
	}
	
	
	private static void renderScreen(int oldX, int oldY, int newX, int newY) {
		if (oldX <= 0 || oldY <= 0) return;
		// Clear the screen and hide the cursor        
		writeString("\033[2J\033[H\033[?25l", ptyDisplaySocket);

		
		for (int i = 0; i < imagesList.size(); i++) {
		    Image currentImage = imagesList.get(i);
		    
		    // Move the cursor to the correct X and Y
		    writeString("\033[" + (int) (newY * (currentImage.yPercent/100.0)) + ";" + (int) (newX * (currentImage.xPercent/100.0)) + "H" , ptyDisplaySocket);    
		    
		    double xRatio = (double) newX / oldX;
		    double yRatio = (double) newY / oldY;

		    SixelImage.Size imageDims = currentImage.image.getSize();
			System.out.println("The only call to setSize calc:" + ((int) (imageDims.width * xRatio)) + ", " + ((int) (imageDims.height * yRatio)));
		    currentImage.image.setSize((int) (imageDims.width * xRatio), (int) (imageDims.height * yRatio));
		    
		    // Retry logic for Sixel generation
		    boolean success = false;
		    int attempts = 0;
		    int maxAttempts = 3;

		    while (!success && attempts < maxAttempts) {
		        try {
		            writeString(currentImage.image.getSixelString(), ptyDisplaySocket);
		            success = true; // If it succeeds, exit the loop
		        } catch (Exception ex) {
		            attempts++;
		            System.err.println("[RenderMgr] Sixel encode failed (Attempt " + attempts + "/" + maxAttempts + "): " + ex.getMessage());
		            
		            if (attempts < maxAttempts) {
		                try {
		                    Thread.sleep(2000); // Wait 2 seconds before trying again
		                } catch (InterruptedException ie) {
		                    Thread.currentThread().interrupt();
		                    System.err.println("[RenderMgr] Retry sleep interrupted.");
		                    break;
		                }
		            } else {
		                System.err.println("[RenderMgr] Max retry attempts reached. Skipping image.");
		            }
		        }
		    }
		}   
	}


	public static void renderImage(Path image, int xPercent, int yPercent) {
		
		SixelImage renderImage = new SixelImage(image);		
		
		imagesList.add(new Image(renderImage, xPercent, yPercent));		
		
		writeString("\033[" + (int) (currentY * (yPercent/100.0)) + ";" + (int) (currentX * (xPercent/100.0)) + "H" , ptyDisplaySocket);	
		
		writeString(renderImage.getSixelString(), ptyDisplaySocket);

	
	}


	public static void renderImage(SixelImage renderImage, int xPercent, int yPercent) {
        imagesList.add(new Image(renderImage, xPercent, yPercent));        
        
        writeString("\033[" + (int) (currentY * (yPercent / 100.0)) + ";" + (int) (currentX * (xPercent / 100.0)) + "H", ptyDisplaySocket);        
        
        writeString(renderImage.getSixelString(), ptyDisplaySocket);
    }

	public static void renderImageNoHistory(SixelImage renderImage, int xPercent, int yPercent) {
        writeString("\033[" + (int) (currentY * (yPercent / 100.0)) + ";" + (int) (currentX * (xPercent / 100.0)) + "H", ptyDisplaySocket);        
        
        writeString(renderImage.getSixelString(), ptyDisplaySocket);
    }


	public static void renderString(String text, String bgHexCode, String textHexCode, Path fontPath, int padding, int xPercent, int yPercent) {
        SixelImage renderImage = new SixelImage(text, bgHexCode, textHexCode, fontPath, padding);
        
        imagesList.add(new Image(renderImage, xPercent, yPercent));        
        
        writeString("\033[" + (int) (currentY * (yPercent/100.0)) + ";" + (int) (currentX * (xPercent/100.0)) + "H", ptyDisplaySocket);        
        
        writeString(renderImage.getSixelString(), ptyDisplaySocket);
    }
	
	

	public static void clearScreen() {

		imagesList.clear();
		writeString("\033[2J\033[H\033[?25l", ptyDisplaySocket);

			
	}
	
	


}


