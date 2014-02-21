package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

@Entity
@IdClass(EnginePropertyId.class)
@Table(name = "engine_property")
public class EngineProperty {
	@Id
	@ManyToOne
	@JoinColumn(name = "eng_id")
	private Engine engine;

	@Id
	@Column(length = 32, nullable = false)
	private String name;

	@Column(length = 128)
	private String value;

	public EngineProperty() {
	}

	public EngineProperty(Engine engine, String name) {
		this.engine = engine;
		this.name = name;
	}

	public Engine getEngine() {
		return engine;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}