package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "chat_lieu")
@Data
public class ChatLieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_lieu_id")
    private Integer chatLieuId;

    @Column(name = "ten_chat_lieu", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String tenChatLieu;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai;

    @OneToMany(mappedBy = "chatLieu")
    private List<SanPham> sanPhams;

}