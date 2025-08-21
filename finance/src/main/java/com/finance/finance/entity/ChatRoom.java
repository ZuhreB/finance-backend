package com.finance.finance.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String name; // örn: "Genel Sohbet"

    private boolean isPrivate = false;

    // getter & setter
}