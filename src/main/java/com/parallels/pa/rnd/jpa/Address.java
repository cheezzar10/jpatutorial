package com.parallels.pa.rnd.jpa;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Address {
	@Column(name = "country")
	private String country;
	
	@Column(name = "city")
	private String city;
	
	@Column(name = "street")
	private String street;
	
	@Column(name = "bld")
	private String building;
	
	public Address() {
		
	}
	
	public Address(String country, String city) {
		this(country, city, "N/A", "N/A");
	}

	public Address(String country, String city, String street, String building) {
		this.country = country;
		this.city = city;
		this.street = street;
		this.building = building;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getBuilding() {
		return building;
	}

	public void setBuilding(String building) {
		this.building = building;
	}
	
	public String toString() {
		return String.format("address: {%s, %s}", country, city);
	}
}
