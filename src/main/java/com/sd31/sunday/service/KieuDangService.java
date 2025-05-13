package com.sd31.sunday.service;

import com.sd31.sunday.model.KieuDang;
import com.sd31.sunday.repository.KieuDangRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KieuDangService {

    @Autowired
    private KieuDangRepository kieuDangRepository;

    private static final Logger logger = LoggerFactory.getLogger(KieuDangService.class);

    public List<KieuDang> getAllKieuDang() {
        return kieuDangRepository.findAll();
    }

    public List<KieuDang> getAllKieuDangByTrangThai() {
        return kieuDangRepository.findByTrangThai("Hoạt động");
    }

    public List<KieuDang> getKieuDangByTrangThai(String trangThai) {
        return kieuDangRepository.findByTrangThai(trangThai);
    }

    public void saveKieuDang(KieuDang kieuDang) {
        if (kieuDangRepository.findByTenKieuDangIgnoreCase(kieuDang.getTenKieuDang()).isPresent()) {
            throw new DataIntegrityViolationException("Tên kiểu dáng đã tồn tại. Vui lòng chọn tên khác.");
        }
        kieuDangRepository.save(kieuDang);
    }

    public Optional<KieuDang> getKieuDangById(Integer id) {
        return kieuDangRepository.findById(id);
    }

    public void updateKieuDang(KieuDang kieuDang) {
        if (kieuDangRepository.existsByTenKieuDangIgnoreCaseAndKieuDangIdNot(kieuDang.getTenKieuDang(), kieuDang.getKieuDangId())) {
            throw new DataIntegrityViolationException("Tên kiểu dáng đã tồn tại. Vui lòng chọn tên khác.");
        }
        kieuDangRepository.save(kieuDang);
    }

    public void toggleTrangThai(Integer id) {
        KieuDang kieuDang = kieuDangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kiểu dáng với ID: " + id));

        // Correctly toggle the status
        kieuDang.setTrangThai(kieuDang.getTrangThai().equals("Hoạt động") ? "Không hoạt động" : "Hoạt động");
        kieuDangRepository.save(kieuDang);
    }

    public void deleteKieuDang(Integer id) {
        logger.info("Bắt đầu xóa KieuDang với ID: {}", id);

        Optional<KieuDang> kieuDangOptional = kieuDangRepository.findById(id);
        if (!kieuDangOptional.isPresent()) {
            logger.warn("Không tìm thấy KieuDang với ID: {} để xóa", id);
            return;
        }
    }

    // Phương thức kiểm tra trùng tên kiểu dáng
    private boolean isTenKieuDangDuplicate(String tenKieuDang, Integer kieuDangId) {
        Optional<KieuDang> existingKieuDang = kieuDangRepository.findByTenKieuDang(tenKieuDang);
        if (existingKieuDang.isPresent()) {
            if (kieuDangId == null || !existingKieuDang.get().getKieuDangId().equals(kieuDangId)) {
                return true;
            }
        }
        return false;
    }
}