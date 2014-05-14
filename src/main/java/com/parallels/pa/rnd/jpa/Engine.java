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

	@Column(length = 1, nullable = false)
	// @org.hibernate.annotations.Type(type = "yes_no")
	private boolean diesel;

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

	public void addProperty(String name, String value) {
		if (properties == null) {
			properties = new LinkedList<>();
		}
		EngineProperty engineProp = new EngineProperty(this, name);
		engineProp.setValue(value);
		properties.add(engineProp);
	}

	public boolean isDiesel() {
		return diesel;
	}

	public void setDiesel(boolean diesel) {
		this.diesel = diesel;
	}
}