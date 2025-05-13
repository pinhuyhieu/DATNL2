package com.sd31.sunday.repository;

import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.model.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Integer> {

    @Query("SELECT ctsp FROM ChiTietSanPham ctsp " +
            "JOIN FETCH ctsp.sanPham sp " +
            "JOIN FETCH ctsp.mauSac ms " +
            "JOIN FETCH ctsp.kichCo kc " +
            "WHERE ctsp.trangThai = :ctspStatus AND sp.trangThai = :spStatus") // Use named parameters
    List<ChiTietSanPham> findAllWithDetailsByStatus( // Renamed for better clarity (optional)
                                                     @Param("ctspStatus") String ctspStatus,
                                                     @Param("spStatus") String spStatus
    );

    @Query("SELECT DISTINCT c.mauSac FROM ChiTietSanPham c WHERE c.sanPham.sanPhamId = :sanPhamId")
    List<MauSac> findMauSacBySanPhamId(@Param("sanPhamId") Integer sanPhamId);

    @Query("SELECT DISTINCT c.kichCo FROM ChiTietSanPham c " +
            "WHERE c.sanPham.sanPhamId = :sanPhamId " +
            "AND c.mauSac.mauSacId = :mauSacId " +
            "AND c.trangThai = :trangThai")
    List<KichCo> findKichCoBySanPhamIdAndMauSacAndTrangThai(@Param("sanPhamId") Integer sanPhamId,
                                                            @Param("mauSacId") Integer mauSacId,
                                                            @Param("trangThai") String trangThai);


    @Query("SELECT c FROM ChiTietSanPham c WHERE c.sanPham.sanPhamId = :sanPhamId AND c.mauSac.mauSacId = :mauSacId AND c.kichCo.kichCoId = :kichCoId")
    ChiTietSanPham getChiTietSanPhamBySanPhamAndMauSacAndKichCo(@Param("sanPhamId") Integer sanPhamId, @Param("mauSacId") Integer mauSacId,@Param("kichCoId") Integer kichCoId);

    boolean existsBySanPham_SanPhamIdAndMauSac_MauSacIdAndKichCo_KichCoId(Integer sanPhamId, Integer mauSacId, Integer kichCoId);
    boolean existsBySanPham_SanPhamIdAndMauSac_MauSacIdAndKichCo_KichCoIdAndChiTietSanPhamIdNot(Integer sanPhamId, Integer mauSacId, Integer kichCoId, Integer chiTietSanPhamId);
    List<ChiTietSanPham> findBySanPham_SanPhamId(Integer sanPhamId);

    @Query("SELECT DISTINCT ctsp.kichCo FROM ChiTietSanPham ctsp " +
            "WHERE ctsp.sanPham.sanPhamId = :sanPhamId AND ctsp.trangThai = :trangThai")
    List<KichCo> findDistinctKichCoBySanPhamIdAndTrangThai(@Param("sanPhamId") Integer sanPhamId,
                                                           @Param("trangThai") String trangThai);



    @Query("SELECT DISTINCT ctsp.mauSac FROM ChiTietSanPham ctsp " +
            "WHERE ctsp.sanPham.sanPhamId = :sanPhamId AND ctsp.trangThai = :trangThai")
    List<MauSac> findDistinctMauSacBySanPhamIdAndTrangThai(@Param("sanPhamId") Integer sanPhamId,
                                                           @Param("trangThai") String trangThai);


    // Find DISTINCT MauSac associated with ACTIVE ChiTietSanPham for a given SanPham
    @Query("SELECT DISTINCT c.mauSac FROM ChiTietSanPham c WHERE c.sanPham.sanPhamId = :sanPhamId AND c.trangThai = :status")
    List<MauSac> findActiveMauSacBySanPhamId(@Param("sanPhamId") Integer sanPhamId, @Param("status") String status);

    // Find DISTINCT KichCo associated with ACTIVE ChiTietSanPham for a given SanPham and MauSac
    @Query("SELECT DISTINCT c.kichCo FROM ChiTietSanPham c WHERE c.sanPham.sanPhamId = :sanPhamId AND c.mauSac.mauSacId = :mauSacId AND c.trangThai = :status")
    List<KichCo> findActiveKichCoBySanPhamIdAndMauSac(@Param("sanPhamId") Integer sanPhamId, @Param("mauSacId") Integer mauSacId, @Param("status") String status);

    // Find the specific ACTIVE ChiTietSanPham for a given SanPham, MauSac, and KichCo
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.sanPham.sanPhamId = :sanPhamId AND c.mauSac.mauSacId = :mauSacId AND c.kichCo.kichCoId = :kichCoId AND c.trangThai = :status")
    Optional<ChiTietSanPham> findActiveChiTietSanPhamBySanPhamAndMauSacAndKichCo(
            @Param("sanPhamId") Integer sanPhamId,
            @Param("mauSacId") Integer mauSacId,
            @Param("kichCoId") Integer kichCoId,
            @Param("status") String status
    );






}
