package com.sd31.sunday.controller;

import com.sd31.sunday.DTO.DataDTO;
import com.sd31.sunday.DTO.RequestDTO;
import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.GioHang;
import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.model.KichCo;
import com.sd31.sunday.repository.ChiTietHoaDonRepository;
import com.sd31.sunday.repository.ChiTietSanPhamRepository;
import com.sd31.sunday.repository.GioHangRepository;
import com.sd31.sunday.service.SanPhamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private GioHangRepository gioHangRepository;


    @Autowired
    private ChiTietHoaDonRepository chiTietHoaDonRepository; // Inject ChiTietHoaDonRepository


    // API lấy danh sách kích cỡ theo productId
    @GetMapping("/sizes")
    public ResponseEntity<?> getSizesByProductId(@RequestParam("productId") Integer productId) {
        System.out.println("API /api/sizes được gọi với productId: " + productId);
        List<String> sizes = sanPhamService.getSizesByProductId(productId);
        System.out.println("Dữ liệu kích cỡ trả về từ service: " + sizes);
        if (sizes != null && !sizes.isEmpty()) {
            System.out.println("API /api/sizes trả về thành công: " + sizes);
            return ResponseEntity.ok(sizes);
        } else {
            System.out.println("API /api/sizes trả về NOT_FOUND (không tìm thấy kích cỡ cho productId: " + productId + ")");
            return ResponseEntity.notFound().build();
        }
    }

    // API lấy danh sách màu sắc theo productId
    @GetMapping("/colors")
    public ResponseEntity<?> getColorsByProductId(@RequestParam("productId") Integer productId) {
        System.out.println("API /api/colors được gọi với productId: " + productId);
        List<String> colors = sanPhamService.getColorsByProductId(productId);
        System.out.println("Dữ liệu màu sắc trả về từ service: " + colors);
        if (colors != null && !colors.isEmpty()) {
            System.out.println("API /api/colors trả về thành công: " + colors);
            return ResponseEntity.ok(colors);
        } else {
            System.out.println("API /api/colors trả về NOT_FOUND (không tìm thấy màu sắc cho productId: " + productId + ")");
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/kich-co")
    public ResponseEntity<List<DataDTO>> getData(@RequestParam("sanPhamId") Integer sanPhamId,
                                                 @RequestParam("mauSacId") Integer mausacId) {
        List<KichCo> datas = sanPhamService.getKichCoBySanPhamAndMauSac(sanPhamId, mausacId);
        List<DataDTO> list = new ArrayList<>();
        for (KichCo item : datas) {
            ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.getChiTietSanPhamBySanPhamAndMauSacAndKichCo(sanPhamId, mausacId, item.getKichCoId());
            if (chiTietSanPham != null) {
                // Calculate available stock using the service method
                int availableStock = sanPhamService.calculateAvailableStock(chiTietSanPham);
                int physicalStock = Optional.ofNullable(chiTietSanPham.getSoLuongTon()).orElse(0);

                // Create DataDTO with stock info
                DataDTO data = new DataDTO(
                        chiTietSanPham.getChiTietSanPhamId(),
                        item.getTenKichCo(),
                        chiTietSanPham.getGiaBan(),
                        availableStock,
                        physicalStock // Include physical stock
                );
                list.add(data);
            } else {
                System.err.printf("Warning: ChiTietSanPham not found for SanPhamId=%d, MauSacId=%d, KichCoId=%d%n", sanPhamId, mausacId, item.getKichCoId());
                // Optionally add a DTO with stock 0 if needed for frontend consistency
                // list.add(new DataDTO(null, item.getTenKichCo(), BigDecimal.ZERO, 0, 0));
            }
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/stock-info/{id}")
    public ResponseEntity<Map<String, Integer>> getStockInfo(@PathVariable("id") Integer chiTietSanPhamId) {
        Map<String, Integer> stockData = sanPhamService.getStockInfo(chiTietSanPhamId);
        if (stockData.isEmpty() && !chiTietSanPhamRepository.existsById(chiTietSanPhamId)) {
            // If map is empty AND the ID doesn't exist, return 404
            return ResponseEntity.notFound().build();
        }
        // Otherwise, return the data (might contain 0s if ID exists but no stock)
        return ResponseEntity.ok(stockData);
    }

    @PostMapping("/gio-hang/them")
    public ResponseEntity<String> addToCart(@RequestBody RequestDTO requestDTO, HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
        if (khachHang == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
        }

        Optional<ChiTietSanPham> chiTietSanPhamOpt = chiTietSanPhamRepository.findById(requestDTO.getId());
        if (chiTietSanPhamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sản phẩm không tồn tại.");
        }
        ChiTietSanPham chiTietSanPham = chiTietSanPhamOpt.get();

        // --- Basic Input Validation ---
//        final int MAX_QUANTITY = 10;
        if (requestDTO.getNumber() <= 0) {
            return ResponseEntity.badRequest().body("Số lượng phải lớn hơn 0.");
        }
//        if (requestDTO.getNumber() > MAX_QUANTITY) {
//            return ResponseEntity.badRequest().body("Số lượng tối đa cho phép là " + MAX_QUANTITY + " sản phẩm cho mỗi lần thêm.");
//        }


        // --- Calculate Available Stock using the Service method ---
        int availableStock = sanPhamService.calculateAvailableStock(chiTietSanPham);
        int physicalStock = Optional.ofNullable(chiTietSanPham.getSoLuongTon()).orElse(0); // Get physical stock for messaging

        System.out.printf("Add To Cart Check - CTSP ID %d: Physical=%d, Available=%d, Requested=%d%n",
                chiTietSanPham.getChiTietSanPhamId(), physicalStock, availableStock, requestDTO.getNumber());

        // --- Check Available Stock ---
        if (availableStock <= 0) {
            // Differentiate message based on physical stock
            if (physicalStock <= 0) {
                return ResponseEntity.badRequest().body("Sản phẩm đã hết hàng.");
            } else {
                return ResponseEntity.badRequest().body("Sản phẩm tạm hết hàng (đã được đặt trước).");
            }
        }
        if (requestDTO.getNumber() > availableStock) {
            return ResponseEntity.badRequest().body("Số lượng trong kho không đủ (chỉ còn " + availableStock + " sản phẩm khả dụng).");
        }

        // --- Price Validation ---
        // ... (existing price validation logic seems okay) ...
        final BigDecimal MAX_TOTAL_VALUE = new BigDecimal("100000000");
        BigDecimal giaBan = chiTietSanPham.getGiaBan();
        if (giaBan == null) {
            return ResponseEntity.badRequest().body("Không thể xác định giá sản phẩm.");
        }
        BigDecimal requestedQuantityBigDecimal = new BigDecimal(requestDTO.getNumber());
        BigDecimal requestedValue = giaBan.multiply(requestedQuantityBigDecimal);
        if (requestedValue.compareTo(MAX_TOTAL_VALUE) > 0) {
            return ResponseEntity.badRequest().body("Giá trị của sản phẩm thêm vào vượt quá giới hạn cho phép.");
        }


        // --- Cart Logic ---
        GioHang existingGioHang = gioHangRepository.findByTaiKhoanIdAndChiTietSanPham_Id(khachHang.getKhachHangId(), chiTietSanPham.getChiTietSanPhamId());

        if (existingGioHang != null) {
            int newQuantityInCart = existingGioHang.getSoLuong() + requestDTO.getNumber();

            // Re-check available stock for the *total* quantity in cart
            if (newQuantityInCart > availableStock) {
                return ResponseEntity.badRequest().body("Không thể cập nhật giỏ hàng, số lượng trong kho không đủ (chỉ còn " + availableStock + " sản phẩm khả dụng).");
            }
//            if (newQuantityInCart > MAX_QUANTITY) { // Check total cart quantity limit
//                return ResponseEntity.badRequest().body("Tổng số lượng trong giỏ hàng cho sản phẩm này không được vượt quá " + MAX_QUANTITY + ".");
//            }

            // Re-check total value for the *updated* cart item
            BigDecimal newQuantityBigDecimal = new BigDecimal(newQuantityInCart);
            BigDecimal newTotalValueInCartItem = giaBan.multiply(newQuantityBigDecimal);
            if (newTotalValueInCartItem.compareTo(MAX_TOTAL_VALUE) > 0) {
                return ResponseEntity.badRequest().body("Tổng giá trị cho sản phẩm này trong giỏ hàng vượt quá giới hạn.");
            }

            existingGioHang.setSoLuong(newQuantityInCart);
            gioHangRepository.save(existingGioHang);
            System.out.printf("Updated cart for User %d, CTSP ID %d, New Quantity %d%n", khachHang.getKhachHangId(), chiTietSanPham.getChiTietSanPhamId(), newQuantityInCart);
            // Return a specific message indicating update success
            return ResponseEntity.ok("Số lượng sản phẩm trong giỏ hàng đã được cập nhật.");

        } else {
            // Add new item (stock/value checks already done above)
            GioHang gioHang = new GioHang();
            gioHang.setChiTietSanPham(chiTietSanPham);
            gioHang.setSoLuong(requestDTO.getNumber());
            gioHang.setTaiKhoanId(khachHang.getKhachHangId());
            gioHangRepository.save(gioHang);
            System.out.printf("Added to cart for User %d, CTSP ID %d, Quantity %d%n", khachHang.getKhachHangId(), chiTietSanPham.getChiTietSanPhamId(), requestDTO.getNumber());
            return ResponseEntity.ok("Thêm vào giỏ hàng thành công!");
        }
    }
}