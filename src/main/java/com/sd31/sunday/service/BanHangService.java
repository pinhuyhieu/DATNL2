package com.sd31.sunday.service;

import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.KhachHang;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BanHangService {
    @PersistenceContext
    private EntityManager entityManager;
    public List<ChiTietSanPham> findByTenSanPham(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String queryString = "SELECT DISTINCT ctsp FROM ChiTietSanPham ctsp WHERE " +
                "LOWER(ctsp.sanPham.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                "OR LOWER(ctsp.sanPham.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))";

        TypedQuery<ChiTietSanPham> query = entityManager.createQuery(queryString, ChiTietSanPham.class);
        query.setParameter("keyword", keyword.trim());
        return query.getResultList();
    }
    public List<KhachHang> findBysdt(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String queryString = "SELECT kh FROM KhachHang kh WHERE " +
//                "LOWER(kh.makhachhang) LIKE LOWER(CONCAT('%', :keyword, '%'))  OR " +
                "LOWER(kh.soDienThoai) LIKE LOWER(CONCAT('%', :keyword, '%'))";
        TypedQuery<KhachHang> query = entityManager.createQuery(queryString, KhachHang.class);
        query.setParameter("keyword", keyword.trim());
        return query.getResultList();
    }
}
