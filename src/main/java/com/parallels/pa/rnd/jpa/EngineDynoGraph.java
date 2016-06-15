package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

@Entity
@Table(name = "engine_dyno_graph")
public class EngineDynoGraph {
	@Id
	private Integer id;
	
	@Column(name = "dyno_graph")
	private byte[] dynoGraph;
	
	@MapsId
	@OneToOne
	private Engine engine;
	
	public EngineDynoGraph() {
		
	}
	
	public EngineDynoGraph(Engine engine) {
		this.engine = engine;
	}
	
	public Integer getId() {
		return id;
	}
	
	public byte[] getDynoGraph() {
		return dynoGraph;
	}
	
	public void setDynoGraph(byte[] dynoGraph) {
		this.dynoGraph = dynoGraph;
	}
}
