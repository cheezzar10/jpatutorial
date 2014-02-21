package com.parallels.pa.rnd.jpa;

import java.util.*;
import javax.persistence.*;

@Entity
@Table(name = "engine")
public class Engine {
	@Id
	@GeneratedValue
	private Integer id;

	@Column(length = 32)
	private String maker;

	@Column(length = 64)
	private String model;

	@OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "engine")
	private List<EngineProperty> properties;

	public Engine() {

	}

	public Engine(String maker, String model) {
		this.maker = maker;
		this.model = model;
	}

	public Integer getId() {
		return id;
	}
	
	public String getMaker() {
		return maker;
	}
	
	public void setMaker(String maker) {
		this.maker = maker;
	}
	
	public String getModel() {
		return model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}

	public List<EngineProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<EngineProperty> properties) {
		this.properties = properties;
	}
}