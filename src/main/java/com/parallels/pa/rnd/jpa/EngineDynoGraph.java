package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

@Entity
@Table(name = "engine_dyno_graph")
public class EngineDynoGraph {
	@Id
	private Integer id;
	
	@Column(name = "dyno_graph")
	private byte[] dynoGraph;
	
	public EngineDynoGraph() {
		
	}
	
	public EngineDynoGraph(Engine engine) {
		this.id = engine.getId();
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
