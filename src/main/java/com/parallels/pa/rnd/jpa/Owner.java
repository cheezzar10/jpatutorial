package com.parallels.pa.rnd.jpa;

import java.util.*;
import javax.persistence.*;

@Entity
@Table(name = "owner")
public class Owner {
	@Id
	@GeneratedValue
	private Integer id;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private Set<Car> cars = new HashSet<>();

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
}