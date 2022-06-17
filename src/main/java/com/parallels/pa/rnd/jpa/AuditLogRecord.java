package com.parallels.pa.rnd.jpa;

import org.hibernate.annotations.JoinFormula;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLogRecord {
    @Id
    @GeneratedValue
    private Integer id;

    @Column(nullable = false)
    private Timestamp timestamp;

    @Column(length = 256)
    private String message;

    @Column(name = "user_id")
    private Integer userId;

    @ManyToOne
    @JoinFormula("(select u.id from users u where u.member_id = user_id)")
    private User user;

    public AuditLogRecord() {
    }

    public AuditLogRecord(String message, Integer userId) {
        this(Timestamp.valueOf(LocalDateTime.now()), message, userId);
    }

    public AuditLogRecord(Timestamp timestamp, String message, Integer userId) {
        this.timestamp = timestamp;
        this.message = message;
        this.userId = userId;
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
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
