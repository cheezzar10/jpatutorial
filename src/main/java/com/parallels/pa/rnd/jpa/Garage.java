package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "garage")
public class Garage {
	@Id
	@GeneratedValue
	private Integer id;

	@Column(name = "address", length = 64)
	@NaturalId
	private String address;

	@Column(name = "capacity")
	private int capacity;

	public Garage() {

	}

	public Garage(String address, int capacity) {
		this.address = address;
		this.capacity = capacity;
	}

	public Integer getId() {
		return id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public String toString() {
		return "Garage: { id=" + id + ", address='" + address + "', capacity='" + capacity + "' }";
	}
}