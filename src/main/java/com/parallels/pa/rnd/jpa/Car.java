package com.parallels.pa.rnd.jpa;

import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "car", uniqueConstraints = @UniqueConstraint(columnNames = {"maker", "model"}))
@Cacheable
@org.hibernate.annotations.Cache(region = "jpa", usage = CacheConcurrencyStrategy.READ_WRITE)
// override field results ( result set contains two feilds id - one should be named as car_id, another as owner_id )
@SqlResultSetMapping(name = "Car.carAndOwner", entities = { 
	@EntityResult(entityClass = com.parallels.pa.rnd.jpa.Car.class, fields = {
		@FieldResult(name = "id", column = "car_id"),
		@FieldResult(name = "maker", column = "maker"),
		@FieldResult(name = "model", column = "model"),
		@FieldResult(name = "engine", column = "engine_id"),
		@FieldResult(name = "owner", column = "car_owner_id")
	}),
	@EntityResult(entityClass = com.parallels.pa.rnd.jpa.Owner.class, fields = {
		@FieldResult(name = "id", column = "owner_id")
	}) 
})
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
	@JoinColumn(name = "engine_id")
	private Engine engine;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", foreignKey = @ForeignKey(name = "car_owner_fk"))
	private Owner owner;
	
	@Convert(converter = BodyStyleConverter.class)
	// @Type(type = "character")
	private BodyStyle bodyStyle;
	
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
		if (productionStats != null) {
			productionStats.setCar(this);
		}
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

	public BodyStyle getBodyStyle() {
		return bodyStyle;
	}

	public void setBodyStyle(BodyStyle bodyStyle) {
		this.bodyStyle = bodyStyle;
	}
}