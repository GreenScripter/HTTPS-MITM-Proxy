import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class ChanneledInputStream extends InputStream {
	
	volatile byte[] cache = new byte[8192];
	volatile int offset = 0;
	volatile int length = 0;
	volatile boolean closed = false;
	
	public int read() throws IOException {
		//		if (length <= 0) {
		//			parent.update();
		//		}
		while (length < 1) {
			if (closed) throw new EOFException();
			try {
				synchronized (this) {
					wait(3000);
				}
			} catch (InterruptedException e) {
			}
			
		}
		byte b = cache[offset];
		cache[offset] = 0;
		offset++;
		length--;
		return b & 0xFF;
	}
	
	public void close() {
		closed = true;
	}
	
	public void write(byte[] b, int off, int len) {
		if (len + length + offset <= cache.length) {
			for (int i = 0; i < len; i++) {
				cache[offset + length + i] = b[off + i];
			}
			length += len;
		} else if (len + length <= cache.length) {
			for (int i = 0; i < length; i++) {
				cache[i] = cache[offset + i];
			}
			offset = 0;
			for (int i = 0; i < len; i++) {
				cache[offset + length + i] = b[off + i];
			}
			length += len;
		} else {
			byte[] oldCache = cache;
			while (cache.length < len + length) {
				cache = new byte[cache.length * 2];
			}
			for (int i = 0; i < length; i++) {
				cache[i] = oldCache[offset + i];
			}
			offset = 0;
			for (int i = 0; i < len; i++) {
				cache[offset + length + i] = b[off + i];
			}
			length += len;
		}
		synchronized (this) {
			notifyAll();
		}
	}
	
}