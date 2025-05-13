package com.sd31.sunday.service;

import com.sd31.sunday.model.*;
import com.sd31.sunday.repository.ChiTietSanPhamRepository;
import com.sd31.sunday.repository.HoaDonRepository;
import com.sd31.sunday.repository.LichSuTrangThaiRepository;
import com.sd31.sunday.repository.MaGiamGiaRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityNotFoundException; // <-- Import
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ChiTietHoaDonService chiTietHoaDonService;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private LichSuTrangThaiRepository lichSuTrangThaiRepository;

    @Autowired
    private MaGiamGiaRepository maGiamGiaRepository;

    public Page<HoaDon> getAllHoaDon(Pageable pageable) {
        return hoaDonRepository.findAll(pageable);
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HoaDonService.class); // Thêm logger


//    public Page<HoaDon> filterHoaDon(String maHoaDon, String trangThai, String kenhBanHang, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable) {
//        boolean hasMaHoaDon = StringUtils.hasText(maHoaDon);
//        boolean hasTrangThai = StringUtils.hasText(trangThai);
//        boolean hasKenhBanHang = StringUtils.hasText(kenhBanHang);
//        boolean hasNgayTaoTu = ngayTaoTu != null;
//        boolean hasNgayTaoDen = ngayTaoDen != null;
//
//        if (hasMaHoaDon && hasTrangThai && hasKenhBanHang && hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByMaHoaDonContainingAndTrangThaiAndKenhBanHangAndNgayTaoHoaDonBetween(
//                    maHoaDon, trangThai, kenhBanHang, ngayTaoTu, ngayTaoDen, pageable);
//        } else if (hasMaHoaDon && hasTrangThai && hasKenhBanHang) {
//            return hoaDonRepository.findByMaHoaDonContainingAndTrangThaiAndKenhBanHang(
//                    maHoaDon, trangThai, kenhBanHang, pageable);
//        } else if (hasMaHoaDon && hasKenhBanHang && hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByMaHoaDonContainingAndKenhBanHangAndNgayTaoHoaDonBetween(
//                    maHoaDon, kenhBanHang, ngayTaoTu, ngayTaoDen, pageable);
//        } else if (hasTrangThai && hasKenhBanHang && hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByTrangThaiAndKenhBanHangAndNgayTaoHoaDonBetween(
//                    trangThai, kenhBanHang, ngayTaoTu, ngayTaoDen, pageable);
//        } else if (hasMaHoaDon && hasTrangThai && hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByMaHoaDonContainingAndTrangThaiAndNgayTaoHoaDonBetween(
//                    maHoaDon, trangThai, ngayTaoTu, ngayTaoDen, pageable);
//        } else if (hasKenhBanHang && hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByKenhBanHangAndNgayTaoHoaDonBetween(
//                    kenhBanHang, ngayTaoTu, ngayTaoDen, pageable);
//        } else if (hasMaHoaDon && hasKenhBanHang) {
//            return hoaDonRepository.findByMaHoaDonContainingAndKenhBanHang(maHoaDon, kenhBanHang, pageable);
//        } else if (hasTrangThai && hasKenhBanHang) {
//            return hoaDonRepository.findByTrangThaiAndKenhBanHang(trangThai, kenhBanHang, pageable);
//        } else if (hasMaHoaDon && hasTrangThai) {
//            return hoaDonRepository.findByMaHoaDonContainingAndTrangThai(maHoaDon, trangThai, pageable);
//        } else if (hasMaHoaDon && hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByMaHoaDonContainingAndNgayTaoHoaDonBetween(maHoaDon, ngayTaoTu, ngayTaoDen, pageable);
//        } else if (hasTrangThai && hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByTrangThaiAndNgayTaoHoaDonBetween(trangThai, ngayTaoTu, ngayTaoDen, pageable);
//        } else if (hasKenhBanHang) {
//            return hoaDonRepository.findByKenhBanHang(kenhBanHang, pageable);
//        } else if (hasMaHoaDon) {
//            return hoaDonRepository.findByMaHoaDonContaining(maHoaDon, pageable);
//        } else if (hasTrangThai) {
//            return hoaDonRepository.findByTrangThai(trangThai, pageable);
//        } else if (hasNgayTaoTu && hasNgayTaoDen) {
//            return hoaDonRepository.findByNgayTaoHoaDonBetween(ngayTaoTu, ngayTaoDen, pageable);
//        } else {
//            return hoaDonRepository.findAll(pageable); // Default to all if no filters
//        }
//    }

    public Page<HoaDon> filterHoaDon(String maHoaDon,
                                     String trangThai,
                                     String kenhBanHang,
                                     LocalDateTime ngayTaoTu, // Sử dụng LocalDateTime
                                     LocalDateTime ngayTaoDen, // Sử dụng LocalDateTime
                                     Pageable pageable) {

        logger.info("Filtering HoaDon with params: maHoaDon={}, trangThai={}, kenhBanHang={}, ngayTaoTu={}, ngayTaoDen={}",
                maHoaDon, trangThai, kenhBanHang, ngayTaoTu, ngayTaoDen);

        // Sử dụng Specification để tạo query động
        Specification<HoaDon> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Điều kiện Mã Hóa Đơn (tìm kiếm gần đúng, không phân biệt hoa thường)
            if (StringUtils.hasText(maHoaDon)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("maHoaDon")), "%" + maHoaDon.toLowerCase() + "%"));
            }

            // Điều kiện Trạng Thái (tìm kiếm chính xác)
            if (StringUtils.hasText(trangThai)) {
                predicates.add(criteriaBuilder.equal(root.get("trangThai"), trangThai));
            }

            // Điều kiện Kênh Bán Hàng (tìm kiếm chính xác)
            if (StringUtils.hasText(kenhBanHang)) {
                predicates.add(criteriaBuilder.equal(root.get("kenhBanHang"), kenhBanHang));
            }

            // Điều kiện Ngày Tạo Từ (>=)
            if (ngayTaoTu != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("ngayTaoHoaDon"), ngayTaoTu));
            }

            // Điều kiện Ngày Tạo Đến (<=)
            if (ngayTaoDen != null) {
                // Đảm bảo ngayTaoDen là cuối ngày nếu cần bao gồm cả ngày đó khi chỉ có ngày
                // Controller đã xử lý việc này bằng .atTime(LocalTime.MAX)
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("ngayTaoHoaDon"), ngayTaoDen));
            }

            // Kết hợp các điều kiện bằng AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Thực thi query với Specification và Pageable
        return hoaDonRepository.findAll(spec, pageable);
    }


    // New method to get HoaDon by KhachHangId
    public List<HoaDon> getHoaDonByKhachHangId(Integer khachHangId) {
        return hoaDonRepository.findByKhachHangKhachHangId(khachHangId);
    }

    public Page<HoaDon> getHoaDonByKhachHangId(Integer khachHangId, Pageable pageable) {
        return hoaDonRepository.findByKhachHangKhachHangIdAndKenhBanHang(khachHangId, "Website", pageable);
    }


    @Transactional
    public HoaDon saveHoaDon(HoaDon hoaDon) {
        // **KHÔNG CẦN TÍNH TỔNG TIỀN Ở ĐÂY NỮA, TÍNH Ở CONTROLLER**
        return hoaDonRepository.save(hoaDon);
    }

    public long countHoaDon() {
        return hoaDonRepository.count();
    }

    public List<HoaDon> getPendingHoaDons() {

        return hoaDonRepository.findByTrangThai("Chờ xử lý");
    }

    public void deleteHoaDonById(Integer hoaDonId) {
        hoaDonRepository.deleteById(hoaDonId);
    }

    public boolean existsByMaHoaDon(String maHoaDon) {
        int count = hoaDonRepository.existsByMaHoaDonQuery(maHoaDon);
        return count > 0;
    }

    public HoaDon getHoaDonById(Integer id) {
        return hoaDonRepository.findById(id).orElse(null);
    }

    public long countHoaDonCho() {
        return hoaDonRepository.countByTrangThai("Chờ xử lý");
    }

    public List<HoaDon> getHoaDonCho() {
        return hoaDonRepository.findByTrangThai("Chờ xử lý");
    }

    public String generateMaHoaDon() {
        DecimalFormat df = new DecimalFormat("0000");
        Random random = new Random();
        long count = countHoaDon() + 1;

        char letter1 = (char) (random.nextInt(26) + 'A');
        int digit1 = random.nextInt(10);
        char letter2 = (char) (random.nextInt(26) + 'A');
        int digit2 = random.nextInt(10);

        String code = "HD" + df.format(count) + letter1 + digit1 + letter2 + digit2;

        while (existsByMaHoaDon(code)) {
            count++;
            letter1 = (char) (random.nextInt(26) + 'A');
            digit1 = random.nextInt(10);
            letter2 = (char) (random.nextInt(26) + 'A');
            digit2 = random.nextInt(10);
            code = "HD" + df.format(count) + letter1 + digit1 + letter2 + digit2;
        }
        return code;
    }


    // **PHƯƠNG THỨC MỚI ĐỂ TÍNH TỔNG TIỀN HÓA ĐƠN:**
    public BigDecimal calculateTongTienHoaDon(Integer hoaDonId) {
        List<ChiTietHoaDon> chiTietHoaDons = chiTietHoaDonService.getChiTietHoaDonsByHoaDonId(hoaDonId);
        BigDecimal tongTien = BigDecimal.ZERO;

        for (ChiTietHoaDon cthd : chiTietHoaDons) {
            tongTien = tongTien.add(cthd.getGia().multiply(BigDecimal.valueOf(cthd.getSoLuong())));
        }
        return tongTien;
    }

    public Optional<HoaDon> getOrderDetails(Integer hoaDonId) {
        return hoaDonRepository.findById(hoaDonId);
    }

    @Transactional
    public void cancelOrder(HoaDon hoaDon) {
        // Kiểm tra trạng thái có thể hủy
        if (!hoaDon.getTrangThai().equals("Chờ xác nhận") && !hoaDon.getTrangThai().equals("Đã xác nhận")) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng ở trạng thái 'Chờ xác nhận' hoặc 'Đã xác nhận'.");
        }

        // Nếu trạng thái là "Đã xác nhận", hoàn lại số lượng kho
        if (hoaDon.getTrangThai().equals("Đã xác nhận")) {
            List<ChiTietHoaDon> chiTietHoaDons = chiTietHoaDonService.getChiTietHoaDonsByHoaDonId(hoaDon.getHoaDonId());
            for (ChiTietHoaDon cthd : chiTietHoaDons) {
                ChiTietSanPham chiTietSanPham = cthd.getChiTietSanPham();
                chiTietSanPham.setSoLuongTon(chiTietSanPham.getSoLuongTon() + cthd.getSoLuong());
                chiTietSanPhamRepository.save(chiTietSanPham);
            }
        }

        // Lưu lịch sử trạng thái
        LichSuTrangThai lichSu = new LichSuTrangThai();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThaiCu(hoaDon.getTrangThai());
        lichSu.setTrangThaiMoi("Đã hủy");
        lichSu.setNgayThayDoi(LocalDateTime.now());
        lichSu.setNhanVienId(null); // Không có nhân viên
        lichSu.setGhiChu("Khách chủ động hủy");
        lichSuTrangThaiRepository.save(lichSu);

        // Cập nhật trạng thái đơn hàng
        hoaDon.setTrangThai("Đã hủy");
        hoaDonRepository.save(hoaDon);
    }

    @Transactional // Ensure atomicity
    public void cancelOrderByCustomer(Integer hoaDonId, Integer customerId) {
        logger.info("Service: Attempting cancellation for Order ID: {} by Customer ID: {}", hoaDonId, customerId);

        // 1. Find the order
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> {
                    logger.error("Service: Order not found for ID: {}", hoaDonId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
                });

        // 2. Verify Ownership
        if (!hoaDon.getKhachHang().getKhachHangId().equals(customerId)) {
            logger.warn("Service: Security violation. Customer ID: {} attempted to cancel Order ID: {} owned by Customer ID: {}",
                    customerId, hoaDonId, hoaDon.getKhachHang().getKhachHangId());
            throw new SecurityException("Bạn không có quyền hủy đơn hàng này."); // Use SecurityException for permission issues
        }

        // 3. Check Cancellable Status (Strictly "Chờ xác nhận")
        String currentStatus = hoaDon.getTrangThai();
        if (!currentStatus.equals("Chờ xác nhận")) {
            logger.warn("Service: Order ID: {} cannot be cancelled by customer. Status is '{}'", hoaDonId, currentStatus);
            throw new IllegalStateException("Đơn hàng chỉ có thể hủy khi ở trạng thái 'Chờ xác nhận'.");
        }

        // --- START: Restore Discount Code Logic ---
        MaGiamGia associatedCode = hoaDon.getMaGiamGia();
        if (associatedCode != null) {
            logger.info("Service: Order ID {} used Discount Code ID {}. Attempting restoration.", hoaDonId, associatedCode.getMaGiamGiaId());
            try {
                // Fetch the latest state of the discount code *within the transaction*
                MaGiamGia codeToUpdate = maGiamGiaRepository.findById(associatedCode.getMaGiamGiaId())
                        .orElseThrow(() -> {
                            // This case is unlikely if the relationship is valid, but good to handle
                            logger.error("Service: MaGiamGia not found with ID: {} associated with Order ID {} during restoration attempt.",
                                    associatedCode.getMaGiamGiaId(), hoaDonId);
                            // Don't stop cancellation, but log the error. Or throw if consistency is paramount.
                            return new EntityNotFoundException("Mã giảm giá liên kết với đơn hàng không tồn tại (ID: " + associatedCode.getMaGiamGiaId() + ").");
                        });

                codeToUpdate.setSoLuong(codeToUpdate.getSoLuong() + 1); // Increment quantity
                // Optional: Check if status needs update (e.g., from Hết lượt -> Hoạt động)
                // if (codeToUpdate.getTrangThai().equalsIgnoreCase("Hết lượt") && codeToUpdate.getSoLuong() > 0) {
                //     codeToUpdate.setTrangThai("Hoạt động"); // Or your active status name
                // }
                maGiamGiaRepository.save(codeToUpdate); // Save updated code
                logger.info("Service: Discount Code ID {} quantity incremented for cancelled Order ID {}. New quantity: {}",
                        codeToUpdate.getMaGiamGiaId(), hoaDonId, codeToUpdate.getSoLuong());
            } catch (Exception e) {
                // Log error but allow cancellation to proceed unless it's critical
                logger.error("Service: Failed to restore quantity for Discount Code ID {} on cancellation of Order ID {}. Error: {}",
                        associatedCode.getMaGiamGiaId(), hoaDonId, e.getMessage(), e);
                // Depending on business rules, you might want to re-throw to rollback the transaction
                // throw new RuntimeException("Lỗi khi hoàn lại mã giảm giá. Hủy đơn thất bại.", e);
            }
        } else {
            logger.info("Service: Order ID {} did not use a discount code.", hoaDonId);
        }
        // --- END: Restore Discount Code Logic ---

        // 4. Save Status History (BEFORE updating the main status)
        saveCancelHistoryByCustomer(hoaDon, currentStatus);

        // 5. Update Order Status
        hoaDon.setTrangThai("Đã hủy");
        hoaDonRepository.save(hoaDon);
        logger.info("Service: Order ID {} status updated to 'Đã hủy'.", hoaDonId);

        // No restocking needed as cancellation is only from "Chờ xác nhận"
    }

    private void saveCancelHistoryByCustomer(HoaDon hoaDon, String trangThaiCu) {
        try {
            LichSuTrangThai lichSu = new LichSuTrangThai();
            lichSu.setHoaDon(hoaDon);
            lichSu.setTrangThaiCu(trangThaiCu);
            lichSu.setTrangThaiMoi("Đã hủy");
            lichSu.setNgayThayDoi(LocalDateTime.now());
            // NhanVienId should be null for customer actions
            lichSu.setNhanVienId(null);
            lichSu.setGhiChu("Khách hàng tự hủy đơn."); // Clearer note
            lichSuTrangThaiRepository.save(lichSu);
            logger.info("Service: Saved cancellation history for Order ID: {}", hoaDon.getHoaDonId());
        } catch (Exception e) {
            logger.error("Service: Error saving cancellation history for Order ID {}: {}", hoaDon.getHoaDonId(), e.getMessage(), e);
            // Decide if this should cause the transaction to fail
        }
    }

    public Optional<HoaDon> findById(Integer orderId) {
        return hoaDonRepository.findById(orderId);
    }
}