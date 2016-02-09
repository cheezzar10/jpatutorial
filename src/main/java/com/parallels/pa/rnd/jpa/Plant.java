package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;

@Entity
@Table(name = "plant")
@Cacheable
@Cache(region = "jpa", usage = CacheConcurrencyStrategy.READ_WRITE)
public class Plant {
	@Id
	private Integer id;
	
	@Column(length = 64)
	private String name;
	
	public Plant() {
		
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}