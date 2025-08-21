package com.finance.finance.entity;

import jakarta.persistence.*;

import java.time.Instant;

// domain/Message.java
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hangi odada yazıldı
    @ManyToOne
    @JoinColumn(name="room_id", nullable=false)
    private ChatRoom room;

    // Kim yazdı
    @ManyToOne
    @JoinColumn(name="from_user_id", nullable=false)
    private User fromUser;

    // Özel mesaj için (nullable olabilir)
    @ManyToOne
    @JoinColumn(name="to_user_id")
    private User toUser;

    @Column(nullable=false, length=2000)
    private String content;

    private Instant ts = Instant.now();

    // getter & setter
}
