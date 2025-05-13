package com.sd31.sunday.repository;


import com.sd31.sunday.model.ChatLieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatLieuRepository extends JpaRepository<ChatLieu, Integer> {
    Optional<ChatLieu> findByTenChatLieu(String tenChatLieu);

    @Query("SELECT cl FROM ChatLieu cl WHERE (:trangThai is null or cl.trangThai = :trangThai)")
    List<ChatLieu> findByTrangThai(@Param("trangThai") String trangThai);

    @Query("SELECT COUNT(cl) > 0 FROM ChatLieu cl WHERE LOWER(cl.tenChatLieu) = LOWER(:tenChatLieu) AND cl.chatLieuId <> :chatLieuId")
    boolean existsByTenChatLieuIgnoreCaseAndChatLieuIdNot(@Param("tenChatLieu") String tenChatLieu, @Param("chatLieuId") Integer chatLieuId);

    Optional<ChatLieu> findByTenChatLieuIgnoreCase(String tenChatLieu);
}
