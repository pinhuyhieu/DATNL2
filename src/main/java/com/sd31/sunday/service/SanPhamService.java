package com.sd31.sunday.service;

import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.model.MauSac;
import com.sd31.sunday.model.SanPham;
import com.sd31.sunday.repository.ChiTietHoaDonRepository;
import com.sd31.sunday.repository.ChiTietSanPhamRepository;
import com.sd31.sunday.repository.SanPhamRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private ChiTietHoaDonRepository chiTietHoaDonRepository;

    private static final List<String> RESERVING_STATUSES = Arrays.asList(
            "Chờ xác nhận"
            // Add other statuses like "Đã xác nhận", "Đang chuẩn bị hàng" if they reserve stock
    );

    private static final String ACTIVE_STATUS = "Hoạt động"; // Use this consistently
    private static final String INACTIVE_STATUS = "Không hoạt động";


    public List<SanPham> getAllSanPhamsWithMinPriceAndImages() {
        List<SanPham> sanPhams = sanPhamRepository.findAllActiveWithDetails(    "Hoạt động");
        for (SanPham sp : sanPhams) {
            BigDecimal minPrice = sp.getChiTietSanPhams().stream()
                    .map(ChiTietSanPham::getGiaBan)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            sp.setChiTietSanPhams(List.of(new ChiTietSanPham() {
                @Override
                public BigDecimal getGiaBan() {
                    return minPrice;
                }
            }));
        }
        return sanPhams;
    }


    // Get ACTIVE colors for a product
    public List<MauSac> getActiveMauSacBySanPhamId(Integer sanPhamId) {
        return chiTietSanPhamRepository.findActiveMauSacBySanPhamId(sanPhamId, ACTIVE_STATUS);
    }

    // Get ACTIVE sizes for a product and color
    public List<KichCo> getActiveKichCoBySanPhamAndMauSac(Integer sanPhamId, Integer mauSacId) {
        return chiTietSanPhamRepository.findActiveKichCoBySanPhamIdAndMauSac(sanPhamId, mauSacId, ACTIVE_STATUS);
    }

    // Get the specific ACTIVE variant detail
    public Optional<ChiTietSanPham> getActiveChiTietSanPham(Integer sanPhamId, Integer mauSacId, Integer kichCoId) {
        return chiTietSanPhamRepository.findActiveChiTietSanPhamBySanPhamAndMauSacAndKichCo(sanPhamId, mauSacId, kichCoId, ACTIVE_STATUS);
    }

    public int calculateAvailableStock(ChiTietSanPham ctsp) {
        if (ctsp == null || ctsp.getChiTietSanPhamId() == null) {
            return 0;
        }
        // Ensure physical stock is not null
        int physicalStock = Optional.ofNullable(ctsp.getSoLuongTon()).orElse(0);

        // Get reserved quantity (ensure it's not null)
        Integer reservedQuantityResult = chiTietHoaDonRepository.findReservedQuantityByChiTietSanPhamIdAndStatuses(
                ctsp.getChiTietSanPhamId(), RESERVING_STATUSES);
        int reservedQuantity = Optional.ofNullable(reservedQuantityResult).orElse(0);

        // Calculate available stock, ensuring it's not negative
        return Math.max(0, physicalStock - reservedQuantity);
    }

    public Map<String, Integer> getStockInfo(Integer chiTietSanPhamId) {
        Map<String, Integer> stockData = new HashMap<>();
        Optional<ChiTietSanPham> ctspOpt = chiTietSanPhamRepository.findById(chiTietSanPhamId);

        if (ctspOpt.isPresent()) {
            ChiTietSanPham ctsp = ctspOpt.get();
            int physicalStock = Optional.ofNullable(ctsp.getSoLuongTon()).orElse(0);
            int availableStock = calculateAvailableStock(ctsp); // Reuse calculation logic
            stockData.put("physicalStock", physicalStock);
            stockData.put("availableStock", availableStock);
        } else {
            // Handle case where ChiTietSanPham is not found
            stockData.put("physicalStock", 0);
            stockData.put("availableStock", 0);
        }
        return stockData;
    }

    public SanPham getSanPhamById(Integer id) {
        return sanPhamRepository.findById(id).orElse(null);
    }

    public List<MauSac> getMauSacBySanPhamId(Integer sanPhamId) {
        return chiTietSanPhamRepository.findMauSacBySanPhamId(sanPhamId);
    }

    public List<KichCo> getKichCoBySanPhamAndMauSac(Integer sanPhamId, Integer mauSacId) {
        return chiTietSanPhamRepository
                .findKichCoBySanPhamIdAndMauSacAndTrangThai(sanPhamId, mauSacId, "Hoạt động");
    }


    public List<SanPham> getAllSanPhams() {
        return sanPhamRepository.findAll();
    }

    public Page<SanPham> getFilteredProducts(
            Pageable pageable,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer danhMucId,
            Integer thuongHieuId,
            Integer chatLieuId,
            Integer kieuDangId,
            String trangThai
    ) {
        Specification<SanPham> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("trangThai"), trangThai)); // Always filter by "Hoạt động"

            if (danhMucId != null) {
                predicates.add(criteriaBuilder.equal(root.get("danhMuc").get("danhMucId"), danhMucId));
            }
            if (thuongHieuId != null) {
                predicates.add(criteriaBuilder.equal(root.get("thuongHieu").get("thuongHieuId"), thuongHieuId));
            }
            if (chatLieuId != null) {
                predicates.add(criteriaBuilder.equal(root.get("chatLieu").get("chatLieuId"), chatLieuId));
            }
            if (kieuDangId != null) {
                predicates.add(criteriaBuilder.equal(root.get("kieuDang").get("kieuDangId"), kieuDangId));
            }

            // Price filtering needs to be adjusted to work with ChiTietSanPham's giaBan
            // We will filter based on the minimum price available for each product
            if (minPrice != null) {
                // This is a simplification. Ideally, you'd filter based on min price across all ChiTietSanPhams.
                // For now, we assume you want to filter products where *at least one* ChiTietSanPham has giaBan >= minPrice
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.join("chiTietSanPhams").get("giaBan"), minPrice));
            }
            if (maxPrice != null) {
                // Similar simplification for maxPrice
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.join("chiTietSanPhams").get("giaBan"), maxPrice));
            }


            // Ensure we only fetch distinct SanPham to avoid duplicates due to joins
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return sanPhamRepository.findAll(spec, pageable);
    }

    // Generate unique product code
    private String generateMaSanPham() {
        // Format: VN + 4-digit number + random combination of characters
        DecimalFormat df = new DecimalFormat("0000");
        Random random = new Random();

        // Get the count of existing products to create sequential base number
        long count = sanPhamRepository.count() + 1;

        // Generate random alphanumeric characters (H2P3 part)
        char letter1 = (char) (random.nextInt(26) + 'A');
        int digit1 = random.nextInt(10);
        char letter2 = (char) (random.nextInt(26) + 'A');
        int digit2 = random.nextInt(10);

        String code = "VN" + df.format(count) + letter1 + digit1 + letter2 + digit2;

        // Ensure the code is unique
        while (sanPhamRepository.existsByMaSanPham(code)) {
            count++;
            letter1 = (char) (random.nextInt(26) + 'A');
            digit1 = random.nextInt(10);
            letter2 = (char) (random.nextInt(26) + 'A');
            digit2 = random.nextInt(10);
            code = "VN" + df.format(count) + letter1 + digit1 + letter2 + digit2;
        }

        return code;
    }


    @Transactional
    public SanPham save(SanPham sanPham) {
        if (sanPham.getSanPhamId() == null) {
            // Thêm sản phẩm mới
            sanPham.setMaSanPham(generateMaSanPham());
            sanPham.setNgayThem(new Date());
            if (sanPham.getTrangThai() == null) {
                sanPham.setTrangThai("Hoạt động"); // Giá trị mặc định cho sản phẩm mới
            }
        } else {
            // Cập nhật sản phẩm đã có
            SanPham existingProduct = sanPhamRepository.findById(sanPham.getSanPhamId()).orElse(null);
            if (existingProduct != null) {
                sanPham.setNgayThem(existingProduct.getNgayThem()); // Giữ nguyên ngày tạo
                sanPham.setMaSanPham(existingProduct.getMaSanPham()); // Giữ nguyên mã sản phẩm
                if (sanPham.getTrangThai() == null) {
                    sanPham.setTrangThai(existingProduct.getTrangThai()); // Giữ nguyên trạng thái hiện tại
                }
            }
        }
        return sanPhamRepository.save(sanPham);
    }



    @Transactional
    public void deleteById(Integer id) {
        // First check if product exists
        Optional<SanPham> sanPham = sanPhamRepository.findById(id);
        if (sanPham.isPresent()) {
            // Soft delete
            sanPham.get().setTrangThai("Không hoạt động");
            sanPhamRepository.save(sanPham.get());
        }
    }

    // Utility methods
    public boolean existsByMaSanPham(String maSanPham) {
        return sanPhamRepository.existsByMaSanPham(maSanPham);
    }

    public boolean existsByTenSanPham(String tenSanPham) {
        return sanPhamRepository.existsByTenSanPham(tenSanPham);
    }

    public SanPham updateTrangThai(Integer id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Toggle status
        if ("Hoạt động".equals(sanPham.getTrangThai())) {
            sanPham.setTrangThai("Không hoạt động");
        } else {
            sanPham.setTrangThai("Hoạt động");
        }

        return sanPhamRepository.save(sanPham);
    }

    // Business logic methods
    public List<SanPham> searchSanPham(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSanPhams();
        }
        return sanPhamRepository.searchByKeyword(keyword);
    }

    // Advanced filter method
    public List<SanPham> filterSanPham(Integer danhMucId, Integer thuongHieuId,
                                       Integer chatLieuId, Integer kieuDangId, String trangThai) {
        return sanPhamRepository.filterProducts(danhMucId, thuongHieuId, chatLieuId, kieuDangId, trangThai);
    }

    public boolean validateSanPham(SanPham sanPham) {
        // Chỉ cần kiểm tra xem có lỗi nào không, chi tiết lỗi sẽ nằm trong getValidationErrors
        return getValidationErrors(sanPham).isEmpty();
    }

    // Phương thức này sẽ trả về tất cả lỗi cùng lúc
    public Map<String, String> getValidationErrors(SanPham sanPham) {
        Map<String, String> errors = new HashMap<>();
        if (sanPham.getTenSanPham() == null || sanPham.getTenSanPham().trim().isEmpty()) {
            errors.put("tenSanPham", "Tên sản phẩm không được để trống");
        }
        if (sanPham.getDanhMuc() == null || sanPham.getDanhMuc().getDanhMucId() == null) {
            errors.put("danhMuc", "Vui lòng chọn danh mục");
        }
        if (sanPham.getThuongHieu() == null || sanPham.getThuongHieu().getThuongHieuId() == null) {
            errors.put("thuongHieu", "Vui lòng chọn thương hiệu");
        }
        if (sanPham.getChatLieu() == null || sanPham.getChatLieu().getChatLieuId() == null) {
            errors.put("chatLieu", "Vui lòng chọn chất liệu");
        }
        if (sanPham.getKieuDang() == null || sanPham.getKieuDang().getKieuDangId() == null) {
            errors.put("kieuDang", "Vui lòng chọn kiểu dáng");
        }
        return errors;
    }

    public List<SanPham> searchByKeyword(String keyword, String trangThai) {
        return sanPhamRepository.searchByKeywordHome(keyword, trangThai); // Gọi repository method với trangThai
    }



    public List<String> getSizesByProductId(Integer productId) {
        List<KichCo> kichCos = chiTietSanPhamRepository
                .findDistinctKichCoBySanPhamIdAndTrangThai(productId, "Hoạt động");

        if (kichCos != null && !kichCos.isEmpty()) {
            return kichCos.stream()
                    .map(KichCo::getTenKichCo)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    public List<String> getColorsByProductId(Integer productId) {
        List<MauSac> mauSacs = chiTietSanPhamRepository
                .findDistinctMauSacBySanPhamIdAndTrangThai(productId, "Hoạt động");

        if (mauSacs != null && !mauSacs.isEmpty()) {
            return mauSacs.stream()
                    .map(MauSac::getTenMauSac)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    public ChiTietSanPham setAvailableStockForOne(ChiTietSanPham ctsp) {
        if (ctsp != null) {
            ctsp.setAvailableStock(calculateAvailableStock(ctsp));
        }
        return ctsp;
    }

    public List<ChiTietSanPham> setAvailableStockForList(List<ChiTietSanPham> chiTietSanPhams) {
        if (chiTietSanPhams != null) {
            for (ChiTietSanPham ctsp : chiTietSanPhams) {
                setAvailableStockForOne(ctsp); // Reuse the single calculation logic
            }
        }
        return chiTietSanPhams;
    }
}