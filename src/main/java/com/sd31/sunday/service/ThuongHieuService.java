package com.sd31.sunday.service;

import com.sd31.sunday.model.ThuongHieu;
import com.sd31.sunday.repository.ThuongHieuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ThuongHieuService {

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    private static final Logger logger = LoggerFactory.getLogger(ThuongHieuService.class);

    public List<ThuongHieu> getAllThuongHieu() {
        return thuongHieuRepository.findAll();
    }

    public List<ThuongHieu> getAllThuongHieuByTrangThai() {
        return thuongHieuRepository.findByTrangThai("Hoạt động");
    }

    public List<ThuongHieu> getThuongHieuByTrangThai(String trangThai) {
        return thuongHieuRepository.findByTrangThai(trangThai);
    }

    public void saveThuongHieu(ThuongHieu thuongHieu) {
        if (thuongHieuRepository.findByTenThuongHieuIgnoreCase(thuongHieu.getTenThuongHieu()).isPresent()) {
            throw new DataIntegrityViolationException("Tên thương hiệu đã tồn tại. Vui lòng chọn tên khác.");
        }
        thuongHieuRepository.save(thuongHieu);
    }

    public Optional<ThuongHieu> getThuongHieuById(Integer id) {
        return thuongHieuRepository.findById(id);
    }

    public void updateThuongHieu(ThuongHieu thuongHieu) {
        if (thuongHieuRepository.existsByTenThuongHieuIgnoreCaseAndThuongHieuIdNot(thuongHieu.getTenThuongHieu(), thuongHieu.getThuongHieuId())) {
            throw new DataIntegrityViolationException("Tên thương hiệu đã tồn tại. Vui lòng chọn tên khác.");
        }
        thuongHieuRepository.save(thuongHieu);
    }

    public void toggleTrangThai(Integer id) {
        ThuongHieu thuongHieu = thuongHieuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu với ID: " + id));

        // Correctly toggle the status
        thuongHieu.setTrangThai(thuongHieu.getTrangThai().equals("Hoạt động") ? "Không hoạt động" : "Hoạt động");
        thuongHieuRepository.save(thuongHieu);
    }

    public void deleteThuongHieu(Integer id) {
        logger.info("Bắt đầu xóa ThuongHieu với ID: {}", id);

        Optional<ThuongHieu> thuongHieuOptional = thuongHieuRepository.findById(id);
        if (!thuongHieuOptional.isPresent()) {
            logger.warn("Không tìm thấy ThuongHieu với ID: {} để xóa", id);
            return;
        }
    }

    // Phương thức kiểm tra trùng tên thương hiệu
    private boolean isTenThuongHieuDuplicate(String tenThuongHieu, Integer thuongHieuId) {
        Optional<ThuongHieu> existingThuongHieu = thuongHieuRepository.findByTenThuongHieu(tenThuongHieu);
        if (existingThuongHieu.isPresent()) {
            if (thuongHieuId == null || !existingThuongHieu.get().getThuongHieuId().equals(thuongHieuId)) {
                return true;
            }
        }
        return false;
    }
}