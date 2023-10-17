package com.parallels.pa.rnd.jpa.util;

import java.util.Random;

public class RND {
	private final Random random = new Random();

	public byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];
		random.nextBytes(bytes);
		return bytes;
	}
}
