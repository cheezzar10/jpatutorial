package com.parallels.pa.rnd.jpa.boot;

import java.util.HashMap;
import java.util.Map;

public class EntityManagerFactoryEnvironment {
	public static Map<?, ?> newEnv(String... nameValuePairs) {
		Map<String, Object> props = new HashMap<>();
		
		for (int i = 0;i < nameValuePairs.length;i += 2) {
			props.put(nameValuePairs[i], nameValuePairs[i + 1]);
		}
		
		return props;
	}
}
