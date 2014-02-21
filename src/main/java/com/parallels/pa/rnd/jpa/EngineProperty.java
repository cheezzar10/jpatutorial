package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

@Embeddable
public class EngineProperty {
	@Column(length = 32)
	private String name;

	@Column(length = 128)
	private String value;

	public EngineProperty() {
	}

	public EngineProperty(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}