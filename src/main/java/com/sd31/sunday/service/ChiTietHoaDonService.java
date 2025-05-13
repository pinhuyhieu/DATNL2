package com.sd31.sunday.service;

import com.sd31.sunday.model.ChiTietHoaDon;
import com.sd31.sunday.model.HoaDon;
import com.sd31.sunday.repository.ChiTietHoaDonRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietHoaDonService {

    @Autowired
    private ChiTietHoaDonRepository chiTietHoaDonRepository;

    @Transactional
    public ChiTietHoaDon saveChiTietHoaDon(ChiTietHoaDon chiTietHoaDon) {
        return chiTietHoaDonRepository.save(chiTietHoaDon);
    }

    public ChiTietHoaDon getChiTietHoaDonById(Integer id) {
        return chiTietHoaDonRepository.findById(id).orElse(null);
    }
    @Transactional
    public void deleteById(Integer id) {
        chiTietHoaDonRepository.deleteById(id);
    }

    public List<ChiTietHoaDon> getChiTietHoaDonsByHoaDonId(Integer hoaDonId) {
        HoaDon hoaDon = new HoaDon();
        hoaDon.setHoaDonId(hoaDonId);
        return chiTietHoaDonRepository.findByHoaDon_HoaDonId(hoaDonId); // Call the repository method
    }

    public ChiTietHoaDon findByHoaDonIdAndChiTietSanPhamId(Integer hoaDonId, Integer chiTietSanPhamId) {

        return chiTietHoaDonRepository.findByHoaDon_HoaDonIdAndChiTietSanPham_ChiTietSanPhamId(hoaDonId, chiTietSanPhamId);
    }
}