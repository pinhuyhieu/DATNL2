package com.sd31.sunday.service;

import com.sd31.sunday.model.LichSuTrangThai;
import com.sd31.sunday.repository.LichSuTrangThaiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LichSuTrangThaiService {

    @Autowired
    private LichSuTrangThaiRepository lichSuTrangThaiRepository;

    public LichSuTrangThai saveLichSu(LichSuTrangThai lichSu) {
        return lichSuTrangThaiRepository.save(lichSu);
    }

    public List<LichSuTrangThai> getLichSuByHoaDonId(Integer hoaDonId) {
        return lichSuTrangThaiRepository.findByHoaDon_HoaDonId(hoaDonId);
    }
}
