package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

@Entity
@Table(name = "production_stats")
public class ProductionStatistics {
	@Id
	private Integer id;

	@Column(nullable = false)
	private int unitsMade;

	public ProductionStatistics() {

	}

	public ProductionStatistics(int unitsMade) {
		this.unitsMade = unitsMade;
	}

	public Integer getId() {
		return id;
	}

	public int getUnitsMade() {
		return unitsMade;
	}

	public void setUnitsMade(int unitsMade) {
		this.unitsMade = unitsMade;
	}

	public void setCar(Car car) {
		this.id = car.getId();
	}
}