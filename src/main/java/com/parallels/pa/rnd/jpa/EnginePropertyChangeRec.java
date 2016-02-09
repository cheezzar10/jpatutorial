package com.parallels.pa.rnd.jpa;

import javax.persistence.*;

@Entity
@IdClass(EnginePropertyChangeRecId.class)
@Table(name = "engine_prop_change_rec")
public class EnginePropertyChangeRec {
	@Id
	@ManyToOne
	@JoinColumns({
		@JoinColumn(name = "eng_id", referencedColumnName = "eng_id"),
		@JoinColumn(name = "eng_prop_name", referencedColumnName = "name")
	})
	private EngineProperty property;

	@Id
	@Column(name = "mod_time", nullable = false)
	private long time;

	public EnginePropertyChangeRec() {

	}

	public EnginePropertyChangeRec(EngineProperty property) {
		this.property = property;
		this.time = System.currentTimeMillis();
	}

	public EngineProperty getProperty() {
		return property;
	}

	public long getTime() {
		return time;
	}
}