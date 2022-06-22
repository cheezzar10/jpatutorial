package com.parallels.pa.rnd.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLogRecord implements Serializable {
    @Id
    @GeneratedValue(generator = "cached-sequence-generator")
    private Integer id;

    @Column(nullable = false)
    private Timestamp timestamp;

    @Column(length = 256)
    private String message;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "member_id")
    private User user;

    public AuditLogRecord() {
    }

    public AuditLogRecord(String message, User user) {
        this(Timestamp.valueOf(LocalDateTime.now()), message, user);
    }

    public AuditLogRecord(Timestamp timestamp, String message, User user) {
        this.timestamp = timestamp;
        this.message = message;
        this.user = user;
    }

    public Integer getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "AuditLogRecord{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", user=" + user +
                '}';
    }
}
