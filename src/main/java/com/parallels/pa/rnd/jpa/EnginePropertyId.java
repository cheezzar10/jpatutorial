package com.parallels.pa.rnd.jpa;

import java.io.*;
import javax.persistence.*;

public class EnginePropertyId implements Serializable {
	private Integer engine;
	private String name;

	public EnginePropertyId() {

	}

	public EnginePropertyId(Integer engineId, String name) {
		engine = engineId;
		this.name = name;
	}

	public Integer getEngine() {
		return engine;
	}

	public String getName() {
		return name;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof EnginePropertyId)) {
			return false;
		}

		EnginePropertyId id = (EnginePropertyId)obj;
		return engine.equals(id.engine) && name.equals(id.name);
	}

	public int hashCode() {
		int hash = 17;
		hash = 31*hash + engine.hashCode();
		hash = 31*hash + name.hashCode();
		return hash;
	}
}