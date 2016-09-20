package com.parallels.pa.rnd.jpa;

import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "owner")
public class Owner {
	@Id
	@GeneratedValue
	private Integer id;

	@Column(name = "first_name", length = 32)
	private String firstName;

	@Column(name = "last_name", length = 32)
	private String lastName;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "owner")
	private Set<Car> cars = new HashSet<>();

	@OneToMany(fetch = FetchType.EAGER)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "owner_id")
	private Set<Garage> garages = new HashSet<>();

	public Owner() {

	}

	public Owner(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Integer getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Set<Car> getCars() {
		return cars;
	}

	public Set<Garage> getGarages() {
		return garages;
	}

	public void setGarages(Set<Garage> garages) {
		this.garages = garages;
	}
}