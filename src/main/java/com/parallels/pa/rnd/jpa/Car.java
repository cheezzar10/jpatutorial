package com.parallels.pa.rnd.jpa;

import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "car", uniqueConstraints = @UniqueConstraint(columnNames = {"maker", "model"}))
public class Car {
	@Id
	@GeneratedValue
	private Integer id;
	
	@Column(length = 32)
	private String maker;
	
	@Column(length = 64)
	private String model;
	
	@ElementCollection
	@CollectionTable(name = "options", joinColumns = @JoinColumn(name="car_id"))
	@MapKeyColumn(name = "name")
	@Column(name = "value")
	private Map<String, String> options;

	@PrimaryKeyJoinColumn
	@OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	private ProductionStatistics productionStats;

	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, optional = false)
	private Engine engine;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	@ForeignKey(name = "car_owner_fk")
	private Owner owner;
	
	public Car() {
		
	}
	
	public Car(String maker, String model) {
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
	
	public Map<String, String> getOptions() {
		return options;
	}
	
	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

	public ProductionStatistics getProductionStats() {
		return productionStats;
	}

	public void setProductionStats(ProductionStatistics productionStats) {
		this.productionStats = productionStats;
		productionStats.setCar(this);
	}

	public Engine getEngine() {
		return engine;
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}
}