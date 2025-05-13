package com.sd31.sunday.service;

import com.sd31.sunday.model.DanhMuc;
import com.sd31.sunday.repository.DanhMucRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DanhMucService {

    @Autowired
    private DanhMucRepository danhMucRepository;

    private static final Logger logger = LoggerFactory.getLogger(DanhMucService.class);

    public List<DanhMuc> getAllDanhMuc() {
        return danhMucRepository.findAll();
    }

    public List<DanhMuc> getAllDanhMucByTrangThai() {
        return danhMucRepository.findByTrangThai("Hoạt động");
    }

    public List<DanhMuc> getDanhMucByTrangThai(String trangThai) {
        return danhMucRepository.findByTrangThai(trangThai);
    }

    public void saveDanhMuc(DanhMuc danhMuc) {
        if (danhMucRepository.findByTenDanhMucIgnoreCase(danhMuc.getTenDanhMuc()).isPresent()) {
            throw new DataIntegrityViolationException("Tên danh mục đã tồn tại. Vui lòng chọn tên khác.");
        }
        danhMucRepository.save(danhMuc);
    }

    public Optional<DanhMuc> getDanhMucById(Integer id) {
        return danhMucRepository.findById(id);
    }

    public void updateDanhMuc(DanhMuc danhMuc) {
        if (danhMucRepository.existsByTenDanhMucIgnoreCaseAndDanhMucIdNot(danhMuc.getTenDanhMuc(), danhMuc.getDanhMucId())) {
            throw new DataIntegrityViolationException("Tên danh mục đã tồn tại. Vui lòng chọn tên khác.");
        }
        danhMucRepository.save(danhMuc);
    }

    public void toggleTrangThai(Integer id) {
        DanhMuc danhMuc = danhMucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));

        // Correctly toggle the status
        danhMuc.setTrangThai(danhMuc.getTrangThai().equals("Hoạt động") ? "Không hoạt động" : "Hoạt động");
        danhMucRepository.save(danhMuc);
    }

    public void deleteDanhMuc(Integer id) {
        logger.info("Bắt đầu xóa DanhMuc với ID: {}", id);

        Optional<DanhMuc> danhMucOptional = danhMucRepository.findById(id);
        if (!danhMucOptional.isPresent()) {
            logger.warn("Không tìm thấy DanhMuc với ID: {} để xóa", id);
            return;
        }
    }

    // Phương thức kiểm tra trùng tên danh mục
    private boolean isTenDanhMucDuplicate(String tenDanhMuc, Integer danhMucId) {
        Optional<DanhMuc> existingDanhMuc = danhMucRepository.findByTenDanhMuc(tenDanhMuc);
        if (existingDanhMuc.isPresent()) {
            if (danhMucId == null || !existingDanhMuc.get().getDanhMucId().equals(danhMucId)) {
                return true;
            }
        }
        return false;
    }

    public List<DanhMuc> searchByTenDanhMuc(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllDanhMuc();
        }
        return danhMucRepository.findByTenDanhMucContainingIgnoreCase(keyword.trim());
    }
}