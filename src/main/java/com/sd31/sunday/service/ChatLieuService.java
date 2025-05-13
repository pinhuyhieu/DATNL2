package com.sd31.sunday.service;

import com.sd31.sunday.model.ChatLieu;
import com.sd31.sunday.repository.ChatLieuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatLieuService {

    @Autowired
    private ChatLieuRepository chatLieuRepository;

    private static final Logger logger = LoggerFactory.getLogger(ChatLieuService.class);

    public List<ChatLieu> getAllChatLieu() {
        return chatLieuRepository.findAll();
    }

    public List<ChatLieu> getAllChatLieuByTrangThai() {
        return chatLieuRepository.findByTrangThai("Hoạt động");
    }

    public List<ChatLieu> getChatLieuByTrangThai(String trangThai) {
        return chatLieuRepository.findByTrangThai(trangThai);
    }

    public void saveChatLieu(ChatLieu chatLieu) {
        if (chatLieuRepository.findByTenChatLieuIgnoreCase(chatLieu.getTenChatLieu()).isPresent()) {
            throw new DataIntegrityViolationException("Tên chất liệu đã tồn tại. Vui lòng chọn tên khác.");
        }
        chatLieuRepository.save(chatLieu);
    }

    public Optional<ChatLieu> getChatLieuById(Integer id) {
        return chatLieuRepository.findById(id);
    }

    public void updateChatLieu(ChatLieu chatLieu) {
        if (chatLieuRepository.existsByTenChatLieuIgnoreCaseAndChatLieuIdNot(chatLieu.getTenChatLieu(), chatLieu.getChatLieuId())) {
            throw new DataIntegrityViolationException("Tên chất liệu đã tồn tại. Vui lòng chọn tên khác.");
        }
        chatLieuRepository.save(chatLieu);
    }

    public void toggleTrangThai(Integer id) {
        ChatLieu chatLieu = chatLieuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chất liệu với ID: " + id));

        // Correctly toggle the status
        chatLieu.setTrangThai(chatLieu.getTrangThai().equals("Hoạt động") ? "Không hoạt động" : "Hoạt động");
        chatLieuRepository.save(chatLieu);
    }

    public void deleteChatLieu(Integer id) {
        logger.info("Bắt đầu xóa ChatLieu với ID: {}", id);

        Optional<ChatLieu> chatLieuOptional = chatLieuRepository.findById(id);
        if (!chatLieuOptional.isPresent()) {
            logger.warn("Không tìm thấy ChatLieu với ID: {} để xóa", id);
            return;
        }
    }

    // Phương thức kiểm tra trùng tên chất liệu
    private boolean isTenChatLieuDuplicate(String tenChatLieu, Integer chatLieuId) {
        Optional<ChatLieu> existingChatLieu = chatLieuRepository.findByTenChatLieu(tenChatLieu);
        if (existingChatLieu.isPresent()) {
            if (chatLieuId == null || !existingChatLieu.get().getChatLieuId().equals(chatLieuId)) {
                return true;
            }
        }
        return false;
    }
}