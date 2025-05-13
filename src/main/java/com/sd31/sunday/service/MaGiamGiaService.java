package com.sd31.sunday.service;

import com.sd31.sunday.model.MaGiamGia;
import com.sd31.sunday.repository.MaGiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MaGiamGiaService {

    @Autowired
    private MaGiamGiaRepository maGiamGiaRepository;

    public List<MaGiamGia> getAllMaGiamGia() {
        return maGiamGiaRepository.findAll();
    }

    public Optional<MaGiamGia> getMaGiamGiaById(Integer id) {
        return maGiamGiaRepository.findById(id);
    }

    public MaGiamGia saveMaGiamGia(MaGiamGia maGiamGia) {
        if (maGiamGia.getMaGiamGiaId() == null) {
            maGiamGia.setTrangThai("Hoạt động");
        }
        return maGiamGiaRepository.save(maGiamGia);
    }

    public MaGiamGia updateMaGiamGia(MaGiamGia maGiamGia) {
        return maGiamGiaRepository.save(maGiamGia);
    }

    public void deleteMaGiamGia(Integer id) {
        maGiamGiaRepository.deleteById(id);
    }

    public void toggleTrangThai(Integer id) {
        MaGiamGia maGiamGia = maGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá với ID: " + id));

        if ("Hoạt động".equals(maGiamGia.getTrangThai())) {
            maGiamGia.setTrangThai("Ngừng hoạt động");
        } else {
            maGiamGia.setTrangThai("Hoạt động");
        }

        maGiamGiaRepository.save(maGiamGia);
    }

    public List<MaGiamGia> findByTenMaGiamGiaContainingIgnoreCase(String keyword) {
        return maGiamGiaRepository.findByTenMaGiamGiaContainingIgnoreCase(keyword);
    }

    public List<MaGiamGia> findByTrangThai(String trangThai) {
        return maGiamGiaRepository.findByTrangThai(trangThai);
    }

    public List<MaGiamGia> findByLoaiGiamGia(String loaiGiamGia) {
        return maGiamGiaRepository.findByLoaiGiamGia(loaiGiamGia);
    }

    public List<MaGiamGia> findByTrangThaiAndLoaiGiamGia(String trangThai, String loaiGiamGia) {
        return maGiamGiaRepository.findByTrangThaiAndLoaiGiamGia(trangThai, loaiGiamGia);
    }

    public boolean existsByTenMaGiamGia(String tenMaGiamGia) {
        return maGiamGiaRepository.existsByTenMaGiamGia(tenMaGiamGia);
    }

    public List<MaGiamGia> getValidMaGiamGiasForPos() {
        System.out.println("--- getValidMaGiamGiasForPos: Starting to fetch valid discount codes for POS"); // Log bắt đầu
        List<MaGiamGia> maGiamGiaList = maGiamGiaRepository.findAll().stream()
                .filter(mgg -> "Hoạt động".equals(mgg.getTrangThai()))
                .filter(mgg -> mgg.getNgayKetThuc().isAfter(LocalDateTime.now()))
                .filter(mgg -> mgg.getSoLuong() > 0)
                .collect(Collectors.toList());
        System.out.println("--- getValidMaGiamGiasForPos: Fetched discount codes, size = " + maGiamGiaList.size()); // Log kích thước danh sách
        if (!maGiamGiaList.isEmpty()) {
            System.out.println("--- getValidMaGiamGiasForPos: Content = " + maGiamGiaList); // Log nội dung nếu không rỗng
            for (MaGiamGia mgg : maGiamGiaList) { // In ra chi tiết từng mã giảm giá
                System.out.println("    - MaGiamGia: ID=" + mgg.getMaGiamGiaId() +
                        ", Ten='" + mgg.getTenMaGiamGia() + "'" +
                        ", TrangThai='" + mgg.getTrangThai() + "'" +
                        ", NgayKetThuc=" + mgg.getNgayKetThuc() +
                        ", SoLuong=" + mgg.getSoLuong());
            }
        }
        return maGiamGiaList;
    }
}
