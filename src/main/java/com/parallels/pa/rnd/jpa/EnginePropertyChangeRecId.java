package com.parallels.pa.rnd.jpa;

import java.io.*;

public class EnginePropertyChangeRecId implements Serializable {
	private EnginePropertyId property;
	private long time;

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof EnginePropertyChangeRecId)) {
			return false;
		}

		EnginePropertyChangeRecId changeRec = (EnginePropertyChangeRecId)obj;
		return property.equals(changeRec.property) && time == changeRec.time;
	}

	public int hashCode() {
		int hash = 17;
		hash = 31*hash + property.hashCode();
		hash = 31*hash + (int)time;
		return hash;
	}
}