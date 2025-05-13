package com.sd31.sunday.service;

import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.repository.KichCoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KichCoService {

    @Autowired
    private KichCoRepository kichCoRepository;

    private static final Logger logger = LoggerFactory.getLogger(KichCoService.class);

    public List<KichCo> getAllKichCo() {
        return kichCoRepository.findAll();
    }

    public List<KichCo> getKichCoByTrangThai(String trangThai) {
        return kichCoRepository.findByTrangThai(trangThai);
    }

    public void saveKichCo(KichCo kichCo) {
        if (kichCoRepository.findByTenKichCoIgnoreCase(kichCo.getTenKichCo()).isPresent()) {
            throw new DataIntegrityViolationException("Tên kích cỡ đã tồn tại. Vui lòng chọn tên khác.");
        }
        kichCoRepository.save(kichCo);
    }

    public Optional<KichCo> getKichCoById(Integer id) {
        return kichCoRepository.findById(id);
    }

    public void updateKichCo(KichCo kichCo) {
        if (kichCoRepository.existsByTenKichCoIgnoreCaseAndKichCoIdNot(kichCo.getTenKichCo(), kichCo.getKichCoId())) {
            throw new DataIntegrityViolationException("Tên kích cỡ đã tồn tại. Vui lòng chọn tên khác.");
        }
        kichCoRepository.save(kichCo);
    }

    public void toggleTrangThai(Integer id) {
        KichCo kichCo = kichCoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kích cỡ với ID: " + id));

        // Correctly toggle the status
        kichCo.setTrangThai(kichCo.getTrangThai().equals("Hoạt động") ? "Không hoạt động" : "Hoạt động");
        kichCoRepository.save(kichCo);
    }

    public void deleteKichCo(Integer id) {
        logger.info("Bắt đầu xóa KichCo với ID: {}", id);

        Optional<KichCo> kichCoOptional = kichCoRepository.findById(id);
        if (!kichCoOptional.isPresent()) {
            logger.warn("Không tìm thấy KichCo với ID: {} để xóa", id);
            return;
        }
    }

    // Phương thức kiểm tra trùng tên kích cỡ
    private boolean isTenKichCoDuplicate(String tenKichCo, Integer kichCoId) {
        Optional<KichCo> existingKichCo = kichCoRepository.findByTenKichCo(tenKichCo);
        if (existingKichCo.isPresent()) {
            if (kichCoId == null || !existingKichCo.get().getKichCoId().equals(kichCoId)) {
                return true;
            }
        }
        return false;
    }
}