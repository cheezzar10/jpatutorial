package com.parallels.pa.rnd.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "country")
public class Country {
	@Id
	@GeneratedValue
	private Integer id;
	
	@Column(name = "code", length = 4, nullable = false)
	private String code;
	
	@Column(length = 64)
	private String name;
	
	public Country() {
		
	}
	
	public Country(String code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public Country(Integer id, String code, String name) {
		this.id = id;
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}
	
	public String toString() {
		return String.format("{ code: %s, name: %s }", code, name);
	}
}
