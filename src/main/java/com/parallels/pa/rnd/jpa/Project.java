package com.parallels.pa.rnd.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "project")
public class Project {
	@Id
	@GeneratedValue
	private Integer id;
	
	@ManyToOne
	@JoinColumn(name = "parent_id")
	private Project parent;
	
	@Column(name = "group_id", length = 128, nullable = false)
	private String groupId;
	
	@Column(name = "artifact_id", length = 64, nullable = false)
	private String artifactId;
	
	@Column(length = 64, nullable = false)
	private String version;
	
	public Project() {
		
	}

	public Project(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	public Integer getId() {
		return id;
	}

	public Project getParent() {
		return parent;
	}

	public void setParent(Project parent) {
		this.parent = parent;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String toString() {
		return String.format("project#%d : { %s:%s:%s }", id, groupId, artifactId, version);
	}
}
