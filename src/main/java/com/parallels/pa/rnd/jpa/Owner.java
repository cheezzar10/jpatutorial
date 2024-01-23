package com.parallels.pa.rnd.jpa;

import org.hibernate.annotations.NaturalId;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import javax.persistence.*;

@Entity
@Table(name = "owner")
public class Owner {
	@Id
	@GeneratedValue
	private Integer id;

	@NaturalId
	@org.hibernate.annotations.Type(type = "com.parallels.pa.rnd.jpa.type.StringUuidType")
	@Column(columnDefinition = "uuid")
	private String uid;

	@Column(name = "first_name", length = 32)
	private String firstName;

	@Column(name = "last_name", length = 32)
	private String lastName;

	@Column(name = "birth_date")
	private Date birthDate;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "owner")
	private Set<Car> cars = new HashSet<>();

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private Set<Garage> garages = new HashSet<>();

	public Owner() {

	}

	public Owner(String firstName, String lastName, Date birthDate) {
		this.uid = UUID.randomUUID().toString();
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthDate = birthDate;
	}

	public Owner(String firstName, String lastName, LocalDate birthDate) {
		this(firstName, lastName, Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
	}

	public Integer getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
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

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
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
	
	@Override
	public String toString() { 
		return "Owner: { id=" + id + ", firstName='" + firstName + "', lastName='" + lastName + "' }"; 
	}
}