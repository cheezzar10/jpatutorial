package com.parallels.pa.rnd.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private Integer id;

    @Column(name = "member_id")
    private int memberId;

    @Column(name = "display_name", length = 256)
    private String displayName;

    public User () {

    }

    public User(int memberId, String displayName) {
        this.memberId = memberId;
        this.displayName = displayName;
    }

    public Integer getId() {
        return id;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", memberId=" + memberId +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
