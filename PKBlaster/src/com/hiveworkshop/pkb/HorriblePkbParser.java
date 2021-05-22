package com.hiveworkshop.pkb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.hiveworkshop.wc3.units.objectdata.War3ID;
import com.hiveworkshop.wc3.util.CharInt;

public class HorriblePkbParser {
	private static final int MAGIC_KEY_1 = 0xc9000b11;
	private static final int MAGIC_KEY_2 = 0x01040202;
	private static final int MAGIC_KEY_3 = 0x0000e14a;
	public static final Map<Integer, Set<Integer>> MESSAGE_TYPES_TO_LENS = new TreeMap<>();
	private final List<String> strings = new ArrayList<>();
	private final List<PKBChunk> chunks = new ArrayList<>();
	private final int firstMagicIdentifier;
	private final long secondMagicIdentifier;

	public HorriblePkbParser(final ByteBuffer buffer) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		final int key1 = buffer.getInt();
		final int key2 = buffer.getInt();
		final int key3 = buffer.getInt();
		if (key1 != MAGIC_KEY_1) {
			throw new IllegalStateException("Illegal magic key1=" + key1);
		}
		if (key2 != MAGIC_KEY_2) {
			throw new IllegalStateException("Illegal magic key2=" + key2);
		}
		if (key3 != MAGIC_KEY_3) {
			throw new IllegalStateException("Illegal magic key3=" + key3);
		}
		firstMagicIdentifier = buffer.getInt();
		final int stringDataOffset = buffer.getInt();
		final int initialPosition = buffer.position();
		buffer.position(stringDataOffset);
		final int stringCount = buffer.getInt();
		for (int n = 0; n < stringCount; n++) {
			final int maybeTextureLength = buffer.get();
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < maybeTextureLength; i++) {
				sb.append((char) buffer.get());
			}
			strings.add(sb.toString());
		}
		System.out.println("Finished reading strings at: " + buffer.position());
		System.out.println("Finished with remaining: " + buffer.remaining());
		buffer.position(initialPosition);
		secondMagicIdentifier = buffer.getLong();
		System.out.println("firstmagic: " + firstMagicIdentifier);
		System.out.println("strings offset: " + stringDataOffset);
		System.out.println("secondmagic: " + secondMagicIdentifier);
		while (buffer.position() < stringDataOffset) {
			final int length = buffer.getInt();
			final byte magic32ValueByte = buffer.get(buffer.position());
			if (magic32ValueByte != 0x20) {
				throw new IllegalStateException("Not 32 bit (0x20): " + magic32ValueByte);
			}
			final int messageType = buffer.getInt(buffer.position() + 1);
			final ByteBuffer chunkContentsBuffer = ByteBuffer.allocate(length - 5);
			chunkContentsBuffer.clear();
			for (int i = 5; i < length; i++) {
				chunkContentsBuffer.put(buffer.get(buffer.position() + i));
			}
			chunks.add(new UnknownChunk(messageType, chunkContentsBuffer));
			if (messageType > 400) {
				System.out.println("length: " + length);
				System.out.println("messageType: " + messageType + "\t" + Integer.toHexString(messageType) + "\t"
						+ Integer.toBinaryString(messageType) + "\t" + new War3ID(messageType) + "\t");
			}
			Set<Integer> lens = MESSAGE_TYPES_TO_LENS.get(messageType);
			if (lens == null) {
				lens = new TreeSet<>();
				MESSAGE_TYPES_TO_LENS.put(messageType, lens);
			}
			lens.add(length);
			buffer.position(buffer.position() + length);
		}

	}

	public void setString(final int selectedIndex, final String newString) {
		strings.set(selectedIndex, newString);
	}

	public List<String> getStrings() {
		return strings;
	}

	public List<PKBChunk> getChunks() {
		return chunks;
	}

	public ByteBuffer toBuffer() {
		int byteLength = 12;
		byteLength += 4; // first magic
		byteLength += 4; // string offset location info
		byteLength += 8; // second magic
		for (final PKBChunk chunk : chunks) {
			byteLength += chunk.getByteLength() + 9;
		}
		final int stringsOffset = byteLength;
		byteLength += 4;
		for (final String string : strings) {
			byteLength += string.length() + 1;
		}
		final ByteBuffer buffer = ByteBuffer.allocate(byteLength).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(MAGIC_KEY_1);
		buffer.putInt(MAGIC_KEY_2);
		buffer.putInt(MAGIC_KEY_3);
		buffer.putInt(firstMagicIdentifier);
		buffer.putInt(stringsOffset);
		buffer.putLong(secondMagicIdentifier);
		for (final PKBChunk chunk : chunks) {
			buffer.putInt(chunk.getByteLength() + 5);
			buffer.put((byte) 0x20); // the magic 32
			buffer.putInt(chunk.getChunkType());
			chunk.write(buffer);
		}
		buffer.putInt(strings.size());
		for (final String string : strings) {
			buffer.put((byte) string.length());
			buffer.put(string.getBytes());
		}
		if (buffer.hasRemaining()) {
			throw new IllegalStateException("buffer create failed: " + buffer.remaining());
		}
		return buffer;
	}
}
