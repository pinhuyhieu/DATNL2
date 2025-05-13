package com.sd31.sunday.service;

import com.sd31.sunday.model.MauSac;
import com.sd31.sunday.repository.MauSacRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MauSacService {

    @Autowired
    private MauSacRepository mauSacRepository;

    private static final Logger logger = LoggerFactory.getLogger(MauSacService.class);

    public List<MauSac> getAllMauSac() {
        return mauSacRepository.findAll();
    }

    public List<MauSac> getMauSacByTrangThai(String trangThai) {
        return mauSacRepository.findByTrangThai(trangThai);
    }

    public void saveMauSac(MauSac mauSac) {
        if (mauSacRepository.findByTenMauSacIgnoreCase(mauSac.getTenMauSac()).isPresent()) {
            throw new DataIntegrityViolationException("Tên màu sắc đã tồn tại. Vui lòng chọn tên khác.");
        }
        mauSacRepository.save(mauSac);
    }

    public Optional<MauSac> getMauSacById(Integer id) {
        return mauSacRepository.findById(id);
    }

    public void updateMauSac(MauSac mauSac) {
        if (mauSacRepository.existsByTenMauSacIgnoreCaseAndMauSacIdNot(mauSac.getTenMauSac(), mauSac.getMauSacId())) {
            throw new DataIntegrityViolationException("Tên màu sắc đã tồn tại. Vui lòng chọn tên khác.");
        }
        mauSacRepository.save(mauSac);
    }

    public void toggleTrangThai(Integer id) {
        MauSac mauSac = mauSacRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy màu sắc với ID: " + id));

        // Correctly toggle the status
        mauSac.setTrangThai(mauSac.getTrangThai().equals("Hoạt động") ? "Không hoạt động" : "Hoạt động");
        mauSacRepository.save(mauSac);
    }

    public void deleteMauSac(Integer id) {
        logger.info("Bắt đầu xóa MauSac với ID: {}", id);

        Optional<MauSac> mauSacOptional = mauSacRepository.findById(id);
        if (!mauSacOptional.isPresent()) {
            logger.warn("Không tìm thấy MauSac với ID: {} để xóa", id);
            return;
        }
    }

    // Phương thức kiểm tra trùng tên màu sắc
    private boolean isTenMauSacDuplicate(String tenMauSac, Integer mauSacId) {
        Optional<MauSac> existingMauSac = mauSacRepository.findByTenMauSac(tenMauSac);
        if (existingMauSac.isPresent()) {
            if (mauSacId == null || !existingMauSac.get().getMauSacId().equals(mauSacId)) {
                return true;
            }
        }
        return false;
    }
}
