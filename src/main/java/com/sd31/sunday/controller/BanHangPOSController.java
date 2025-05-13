package com.sd31.sunday.controller;

import com.sd31.sunday.DTO.HoaDonCTDTO;
import com.sd31.sunday.model.*;
import com.sd31.sunday.repository.*;
import com.sd31.sunday.service.BanHangService;
import com.sd31.sunday.service.SanPhamService; // Import SanPhamService
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.hibernate.LazyInitializationException; // Import LazyInitializationException
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/pos")
public class BanHangPOSController {

    // Autowired Repositories
    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;
    @Autowired
    private ChiTietHoaDonRepository hoaDonCTRepository;
    @Autowired
    private MaGiamGiaRepository voucherRepository;
    @Autowired
    private NhanVienLoginRepository nhanVienRespository;
    @Autowired
    private KhachHangRepository khachHangRepository;

    // Autowired Services
    @Autowired
    private BanHangService banHangService; // For searching? Ensure its role is clear
    @Autowired
    private SanPhamService sanPhamService; // For stock calculations

    // Instance variable to hold the ID of the currently active invoice tab
    private Integer idhd; // Current HoaDon ID being worked on

    // --- Helper Methods ---

    /**
     * Safely calculates and retrieves the available stock for a given ChiTietSanPham ID.
     * Considers physical stock minus reserved quantities in specified order statuses.
     *
     * @param chiTietSanPhamId The ID of the ChiTietSanPham.
     * @return The calculated available stock, or 0 if not found or ID is null.
     */
    private int getAvailableStock(Integer chiTietSanPhamId) {
        if (chiTietSanPhamId == null) {
            return 0;
        }
        // Find the product detail first
        Optional<ChiTietSanPham> ctspOpt = sanPhamChiTietRepository.findById(chiTietSanPhamId);
        // If found, use the service to calculate; otherwise, return 0
        return ctspOpt.map(sanPhamService::calculateAvailableStock).orElse(0);
    }

    /**
     * Prepares the common data needed for the main POS view model.
     * Includes invoices, vouchers, products (with available stock), customers.
     * Also populates data for the currently selected invoice if `idhd` is set.
     *
     * @param model The Spring UI Model.
     * @return The name of the view template ("banhang/banhangoffline").
     */
    private String prepareViewModel(Model model) {
        String role = "nhanvien"; // Assuming role determination logic might be added later
        model.addAttribute("role", role);

        // Fetch base data
        List<HoaDon> hoaDonList = hoaDonRepository.findHoaDonsByTrangThai("Chờ thanh toán");
        List<MaGiamGia> activeVouchers = voucherRepository.findByNgayKetThucAfterAndSoLuongGreaterThanAndTrangThai(
                LocalDateTime.now(), 0, "Hoạt động"
        );
        String activeStatus = "Hoạt động"; // Define the status string
        List<ChiTietSanPham> allProductDetails = sanPhamChiTietRepository.findAllWithDetailsByStatus(activeStatus, activeStatus);
        List<KhachHang> activeCustomers = khachHangRepository.timkhachhang(); // Method to get active customers

        // Calculate and set Available Stock for all products
        allProductDetails = sanPhamService.setAvailableStockForList(allProductDetails);

        // Set image URLs for products
        allProductDetails.forEach(sp -> {
            SanPham sanPham = sp.getSanPham();
            if (sanPham != null && sanPham.getHinhAnhs() != null && !sanPham.getHinhAnhs().isEmpty()) {
                HinhAnh anhDaiDien = sanPham.getHinhAnhs().get(0);
                sanPham.setImageUrl(anhDaiDien.getHinhAnhUrl());
            } else if (sanPham != null) {
                sanPham.setImageUrl("/images/placeholder.png"); // Default placeholder
            }
        });

        // Add general data to model
        model.addAttribute("hoaDons", hoaDonList);
        model.addAttribute("lsvoucher", activeVouchers); // Renamed for clarity
        model.addAttribute("lsspct", allProductDetails); // List with availableStock set
        model.addAttribute("listkh", activeCustomers); // Renamed for clarity

        // Add data specific to the selected invoice (if any)
        if (this.idhd != null) {
            populateSelectedInvoiceData(model, this.idhd);
        } else {
            // Provide defaults for when no invoice is selected
            model.addAttribute("hoaDonCTList", Collections.emptyList());
            model.addAttribute("idhoadon", null);
            model.addAttribute("mahd", "N/A");
            model.addAttribute("tongTien", "0 ₫");
            model.addAttribute("tongtien", BigDecimal.ZERO);
            model.addAttribute("thanhtien", "0 ₫");
            model.addAttribute("thanhTien", BigDecimal.ZERO);
        }

        return "banhang/banhangoffline";
    }

    /**
     * Populates the model with details of a specific invoice (cart items, totals).
     *
     * @param model    The Spring UI Model.
     * @param hoaDonId The ID of the invoice to load details for.
     */
    private void populateSelectedInvoiceData(Model model, int hoaDonId) {
        Optional<HoaDon> hoaDonOpt = hoaDonRepository.findById(hoaDonId);
        if (hoaDonOpt.isPresent()) {
            HoaDon currentHoaDon = hoaDonOpt.get();
            List<ChiTietHoaDon> hoaDonCTList = hoaDonCTRepository.findhoadonct(hoaDonId); // Fetch cart items

            // Calculate totals
            BigDecimal currentTotal = tongtien(hoaDonId); // Use helper for sum
            currentTotal = (currentTotal == null) ? BigDecimal.ZERO : currentTotal;

            // Format for display
            DecimalFormat decimalFormat = new DecimalFormat("#,###");
            String totalFormatted = decimalFormat.format(currentTotal) + " ₫";
            // Initially, thanhTien is the same as tongTien (before voucher)
            String finalTotalFormatted = totalFormatted;
            BigDecimal finalTotal = currentTotal;

            // Add to model
            model.addAttribute("hoaDonCTList", hoaDonCTList);
            model.addAttribute("idhoadon", hoaDonId);
            model.addAttribute("mahd", currentHoaDon.getMaHoaDon());
            model.addAttribute("tongTien", totalFormatted);   // Formatted subtotal
            model.addAttribute("tongtien", currentTotal);     // Raw subtotal
            model.addAttribute("thanhtien", finalTotalFormatted); // Formatted final total (updated by voucher)
            model.addAttribute("thanhTien", finalTotal);      // Raw final total (updated by voucher)

        } else {
            // Handle case where the selected invoice ID is somehow invalid
            model.addAttribute("hoaDonCTList", Collections.emptyList());
            model.addAttribute("idhoadon", hoaDonId); // Still show the ID attempted
            model.addAttribute("mahd", "Lỗi!");
            model.addAttribute("tongTien", "0 ₫");
            model.addAttribute("tongtien", BigDecimal.ZERO);
            model.addAttribute("thanhtien", "0 ₫");
            model.addAttribute("thanhTien", BigDecimal.ZERO);
            // Optionally add an error message to the model here
            model.addAttribute("errorMessage", "Không tìm thấy hóa đơn ID: " + hoaDonId);
        }
    }

    /**
     * Calculates the total amount for all items currently in a given invoice.
     *
     * @param hoaDonId The ID of the HoaDon.
     * @return The sum of 'tongTien' for all ChiTietHoaDon in the HoaDon, or BigDecimal.ZERO if none.
     */
    BigDecimal tongtien(int hoaDonId) {
        BigDecimal total = hoaDonCTRepository.TongTienByHoaDonId(hoaDonId);
        return (total == null) ? BigDecimal.ZERO : total;
    }

    /**
     * Retrieves the currently logged-in NhanVien from the HTTP session.
     *
     * @return The logged-in NhanVien object, or null if not found or session doesn't exist.
     */
    private NhanVien getCurrentNhanVien() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr != null) {
            HttpSession session = attr.getRequest().getSession(false); // Don't create if not exists
            if (session != null) {
                Object user = session.getAttribute("loggedInUser");
                if (user instanceof NhanVien) {
                    return (NhanVien) user;
                }
            }
        }
        System.err.println("WARNING: Could not retrieve NhanVien from session.");
        return null;
    }

    // --- Controller Endpoints ---

    @GetMapping("/hienthi")
    public String show(Model model) {
        // Clear selected invoice ID when landing on the main page without specific ID
        // this.idhd = null; // Optional: Uncomment if you want selection cleared on refresh
        return prepareViewModel(model);
    }

    @GetMapping()
    public String hienthi(Model model) {
        // Also ensure selection might be cleared if needed
        // this.idhd = null; // Optional: Uncomment if you want selection cleared on refresh
        return prepareViewModel(model);
    }

    @GetMapping("/search")
    public ResponseEntity<List<String>> searchSanPham(@RequestParam("keyword") String keyword) {
        List<String> results = new ArrayList<>();
        try {
            List<ChiTietSanPham> foundProducts = banHangService.findByTenSanPham(keyword); // Assuming BanHangService has this method

            // Calculate available stock for search results
            foundProducts = sanPhamService.setAvailableStockForList(foundProducts);

            results = foundProducts.stream()
                    // Filter based on available stock > 0
                    .filter(sp -> sp != null && sp.getSanPham() != null && sp.getMauSac() != null
                            && sp.getKichCo() != null && sp.getAvailableStock() > 0)
                    .map(sp -> {
                        String imageUrl = "/images/placeholder.png"; // Default
                        if (sp.getSanPham().getHinhAnhs() != null && !sp.getSanPham().getHinhAnhs().isEmpty()) {
                            imageUrl = sp.getSanPham().getHinhAnhs().get(0).getHinhAnhUrl();
                        }
                        // Format string including available stock for frontend display
                        return String.format("%s - %s - %s - %s - %s - %s - %d (%d có sẵn) - %s",
                                sp.getChiTietSanPhamId(),
                                sp.getSanPham().getTenSanPham(),
                                sp.getMauSac().getTenMauSac(),
                                sp.getKichCo().getTenKichCo(),
                                sp.getGiaBan().toPlainString(), // Use plain string for price
                                sp.getSanPham().getChatLieu().getTenChatLieu(),
                                sp.getSoLuongTon(),          // Physical stock
                                sp.getAvailableStock(),     // Available stock
                                imageUrl
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error during product search: " + e.getMessage());
            e.printStackTrace();
            // Return empty list on error, or could return an error response
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/searchkh")
    public ResponseEntity<List<String>> searchkhachhang(@RequestParam("keyword") String keyword) {
        List<String> results = new ArrayList<>();
        try {
            // Assuming BanHangService has findBysdt, or use khachHangRepository directly
            results = banHangService.findBysdt(keyword).stream()
                    .filter(kh -> kh.getTen() != null && kh.getMaKhachHang() != null && kh.getSoDienThoai() != null)
                    .map(sp -> String.format("%s - %s - %s - %s ",
                            sp.getKhachHangId(),
                            sp.getMaKhachHang(),
                            sp.getTen(),
                            sp.getSoDienThoai()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error during customer search: " + e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/muahang/{id}")
    public String muahang(@PathVariable(value = "id", required = true) int idhoadon, Model model, RedirectAttributes redirectAttributes) {
        // Validate if the invoice ID actually exists and is 'Chờ thanh toán'
        Optional<HoaDon> hoaDonOpt = hoaDonRepository.findById(idhoadon);
        if (hoaDonOpt.isEmpty() || !"Chờ thanh toán".equals(hoaDonOpt.get().getTrangThai())) {
            redirectAttributes.addFlashAttribute("loi", "Hóa đơn không hợp lệ hoặc không tìm thấy.");
            this.idhd = null; // Clear invalid selection
            return "redirect:/admin/pos"; // Redirect to main page
        }

        this.idhd = idhoadon; // Set the current invoice ID
        // Prepare the view model, which will include details for this idhd
        prepareViewModel(model);
        // No need to redirect, just return the view name.
        // The view will re-render with the selected invoice highlighted.
        return "banhang/banhangoffline";
    }


    @GetMapping("/hoadon")
    public ResponseEntity<List<HoaDon>> getEmptyHoaDons() {
        // This seems to return active "Chờ thanh toán" invoices, not empty ones?
        // Renaming or clarifying the purpose might be good.
        List<HoaDon> hoaDons = hoaDonRepository.findHoaDonsByTrangThai("Chờ thanh toán");
        return ResponseEntity.ok(hoaDons);
    }


    @PostMapping("/taohd")
    public ResponseEntity<Map<String, String>> taohoadon() {
        Map<String, String> response = new HashMap<>();
        try {
            // Check limit (optional, based on requirements)
            long currentWaitingInvoices = hoaDonRepository.countByTrangThai("Chờ thanh toán");
            if (currentWaitingInvoices >= 5) { // Example limit
                response.put("error", "Đã đạt số lượng hóa đơn chờ tối đa (5).");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate unique invoice code
            DecimalFormat df = new DecimalFormat("0000");
            Random random = new Random();
            long count = hoaDonRepository.count() + 1; // Simple count, might have gaps if HĐ deleted
            char letter1 = (char) (random.nextInt(26) + 'A');
            int digit1 = random.nextInt(10);
            char letter2 = (char) (random.nextInt(26) + 'A');
            int digit2 = random.nextInt(10);
            String code = "HD" + df.format(count) + letter1 + digit1 + letter2 + digit2;
            // Ensure uniqueness (basic check)
            while (hoaDonRepository.existsByMaHoaDon(code)) {
                count++; // Increment counter and try again
                code = "HD" + df.format(count) + letter1 + digit1 + letter2 + digit2; // Regenerate if collision
            }

            // Create and save the new HoaDon
            HoaDon newhd = new HoaDon();
            newhd.setMaHoaDon(code);
            newhd.setTongTien(BigDecimal.ZERO); // Initial total is zero
            newhd.setTrangThai("Chờ thanh toán");
            newhd.setKenhBanHang("POS");
            newhd.setNgayTaoHoaDon(LocalDateTime.now());
            // Set NhanVien if required immediately (might be better to set at checkout)
            // NhanVien nv = getCurrentNhanVien();
            // if (nv != null) newhd.setNhanVien(nv);

            HoaDon savedHd = hoaDonRepository.save(newhd);

            // Prepare successful response
            response.put("mahoadon", savedHd.getMaHoaDon());
            response.put("id", String.valueOf(savedHd.getHoaDonId())); // Send back ID too
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error creating new invoice: " + e.getMessage());
            e.printStackTrace();
            response.put("error", "Lỗi hệ thống khi tạo hóa đơn.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/taohdct")
    public ResponseEntity<String> createHoaDonCT(@RequestBody HoaDonCTDTO dto, Model model) {
        // 1. Basic Validation
        if (this.idhd == null) {
            return ResponseEntity.badRequest().body("Chưa chọn hóa đơn!");
        }
        if (dto == null || dto.getIdSanPhamCT() == null || dto.getGia() == null) {
            return ResponseEntity.badRequest().body("Dữ liệu sản phẩm không hợp lệ.");
        }
        // Typically adding one item at a time from POS modal
        int requestedQuantityToAdd = (dto.getSoLuong() == null || dto.getSoLuong() < 1) ? 1 : dto.getSoLuong();


        // 2. Check Available Stock
        int availableStock = getAvailableStock(dto.getIdSanPhamCT());

        // 3. Find existing item in the current cart (idhd)
        ChiTietHoaDon existingHoaDonCT = hoaDonCTRepository.findByHoaDonIdAndSanPhamId(this.idhd, dto.getIdSanPhamCT());
        int quantityAlreadyInCart = (existingHoaDonCT != null) ? existingHoaDonCT.getSoLuong() : 0;
        int newTotalQuantityInCart = quantityAlreadyInCart + requestedQuantityToAdd;

        // 4. Compare requested total quantity with available stock
        if (newTotalQuantityInCart > availableStock) {
            Optional<ChiTietSanPham> ctspOpt = sanPhamChiTietRepository.findById(dto.getIdSanPhamCT());
            String productName = ctspOpt.map(ctsp -> ctsp.getSanPham().getTenSanPham() + " " + ctsp.getMauSac().getTenMauSac() + "/" + ctsp.getKichCo().getTenKichCo()).orElse("Sản phẩm");
            return ResponseEntity.badRequest().body(String.format("Không đủ hàng! %s chỉ còn %d sản phẩm có sẵn (bạn đang có %d trong giỏ).", productName, availableStock, quantityAlreadyInCart));
        }

        // 5. Proceed with adding/updating
        try {
            if (existingHoaDonCT != null) {
                // Update existing item's quantity and total
                existingHoaDonCT.setSoLuong(newTotalQuantityInCart);
                BigDecimal totalPrice = existingHoaDonCT.getGia().multiply(BigDecimal.valueOf(newTotalQuantityInCart));
                existingHoaDonCT.setTongTien(totalPrice);
                hoaDonCTRepository.save(existingHoaDonCT);
                System.out.println("Updated HDCT ID: " + existingHoaDonCT.getChiTietHoaDonId() + " for HD ID: " + this.idhd + ", New Qty: " + newTotalQuantityInCart);
            } else {
                // Add new item to the cart
                ChiTietHoaDon hoaDonCT = new ChiTietHoaDon();
                hoaDonCT.setHoaDon(new HoaDon(this.idhd)); // Link to current HoaDon
                hoaDonCT.setChiTietSanPham(new ChiTietSanPham(dto.getIdSanPhamCT())); // Link to product detail
                hoaDonCT.setGia(dto.getGia()); // Unit price
                hoaDonCT.setSoLuong(requestedQuantityToAdd); // Quantity being added now
                BigDecimal thanhTien = dto.getGia().multiply(BigDecimal.valueOf(requestedQuantityToAdd));
                hoaDonCT.setTongTien(thanhTien); // Total for this line item
                ChiTietHoaDon savedHdct = hoaDonCTRepository.save(hoaDonCT);
                System.out.println("Created new HDCT ID: " + savedHdct.getChiTietHoaDonId() + " for HD ID: " + this.idhd + ", Qty: " + requestedQuantityToAdd);
            }
            return ResponseEntity.ok("Thêm vào giỏ hàng thành công!");
        } catch (Exception e) {
            System.err.println("Error saving HDCT for HD ID " + this.idhd + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống khi cập nhật giỏ hàng.");
        }
    }


    @PostMapping("/update")
    public ResponseEntity<String> updateProduct(@RequestBody HoaDonCTDTO dto, Model model) {
        // 1. Basic Validation
        if (dto == null || dto.getIdHoadon() == null || dto.getIdSanPhamCT() == null || dto.getSoLuong() == null ) {
            return ResponseEntity.badRequest().body("Dữ liệu cập nhật không hợp lệ.");
        }
        if (dto.getSoLuong() <= 0) {
            return ResponseEntity.badRequest().body("Số lượng phải lớn hơn 0.");
        }
        // Ensure the update is for the currently selected invoice if needed,
        // although the DTO carries the ChiTietHoaDon ID (idHoadon field in DTO)
        if (this.idhd == null) {
            return ResponseEntity.badRequest().body("Chưa chọn hóa đơn để cập nhật.");
        }

        // 2. Find the Cart Item by its specific ID (passed as idHoadon in DTO)
        Optional<ChiTietHoaDon> hoaDonCTOpt = hoaDonCTRepository.findById(dto.getIdHoadon());

        if (hoaDonCTOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy sản phẩm trong giỏ hàng để cập nhật.");
        }
        ChiTietHoaDon hoaDonCT = hoaDonCTOpt.get();

        // Optional: Verify it belongs to the currently active invoice 'idhd'
        if (!hoaDonCT.getHoaDon().getHoaDonId().equals(this.idhd)) {
            return ResponseEntity.badRequest().body("Lỗi: Sản phẩm không thuộc hóa đơn đang chọn.");
        }
        // Verify product ID matches
        if (!hoaDonCT.getChiTietSanPham().getChiTietSanPhamId().equals(dto.getIdSanPhamCT())){
            return ResponseEntity.badRequest().body("Lỗi dữ liệu sản phẩm không khớp.");
        }

        // 3. Check Available Stock
        int availableStock = getAvailableStock(dto.getIdSanPhamCT());
        int requestedQuantity = dto.getSoLuong();

        // 4. Compare requested quantity with available stock
        if (requestedQuantity > availableStock) {
            Optional<ChiTietSanPham> ctspOpt = sanPhamChiTietRepository.findById(dto.getIdSanPhamCT());
            String productName = ctspOpt.map(ctsp -> ctsp.getSanPham().getTenSanPham() + " " + ctsp.getMauSac().getTenMauSac() + "/" + ctsp.getKichCo().getTenKichCo()).orElse("Sản phẩm");
            return ResponseEntity.badRequest().body(String.format("Số lượng vượt quá mức có sẵn! %s chỉ còn %d sản phẩm.", productName, availableStock));
        }

        // 5. Proceed with update
        try {
            hoaDonCT.setSoLuong(requestedQuantity);
            // Recalculate total price based on quantity and the unit price stored in HDCT
            BigDecimal unitPrice = hoaDonCT.getGia();
            BigDecimal newTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQuantity));
            hoaDonCT.setTongTien(newTotal); // Update total based on recalculated value

            hoaDonCTRepository.save(hoaDonCT);
            System.out.println("Updated quantity for HDCT ID: " + hoaDonCT.getChiTietHoaDonId() + " to " + requestedQuantity);
            // Can return new cart total if frontend needs it without full reload
            // BigDecimal cartTotal = tongtien(this.idhd);
            // return ResponseEntity.ok(Map.of("message", "Cập nhật thành công", "newCartTotal", cartTotal));
            return ResponseEntity.ok("Cập nhật số lượng thành công!");

        } catch (Exception e) {
            System.err.println("Error updating HDCT ID " + dto.getIdHoadon() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống khi cập nhật số lượng.");
        }
    }

    @GetMapping("/chonkh")
    public ResponseEntity<Map<String, String>> chonkh(@RequestParam(value = "id", required = true) Integer id) {
        Map<String, String> response = new HashMap<>();
        Optional<KhachHang> khOpt = khachHangRepository.findById(id);
        if (khOpt.isPresent()) {
            KhachHang kh = khOpt.get();
            response.put("tenkh", kh.getTen());
            response.put("id", String.valueOf(kh.getKhachHangId()));
            response.put("sdt", kh.getSoDienThoai());
            // Send email as 'diachi' if needed, handle null
            response.put("diachi", kh.getEmail() != null ? kh.getEmail() : "Trống");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Không tìm thấy khách hàng");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/selectvc")
    public ResponseEntity<Map<String, Object>> chonVoucher(@RequestParam(value = "id", required = true) String voucherCode) {
        Map<String, Object> response = new HashMap<>();

        if (this.idhd == null) {
            response.put("error", "Vui lòng chọn hóa đơn trước khi áp dụng voucher.");
            return ResponseEntity.badRequest().body(response);
        }

        BigDecimal currentTotal = tongtien(this.idhd);
        if (currentTotal.compareTo(BigDecimal.ZERO) <= 0) {
            response.put("error", "Hóa đơn chưa có sản phẩm, không thể áp dụng voucher.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<MaGiamGia> checkvoucher = voucherRepository.findByTenMaGiamGia(voucherCode);

        if (checkvoucher.isPresent()) {
            MaGiamGia voucher = checkvoucher.get();
            DecimalFormat decimalFormat = new DecimalFormat("#,###");

            // Validate Voucher Status & Quantity
            if (!"Hoạt động".equalsIgnoreCase(voucher.getTrangThai()) || voucher.getSoLuong() == null || voucher.getSoLuong() < 1 || (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(LocalDateTime.now()))) {
                response.put("error", "Voucher không hợp lệ, đã hết hạn hoặc hết lượt sử dụng.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate Minimum Order Value
            BigDecimal giaTriToiThieu = voucher.getGiaTriToiThieuDonHang() != null ? voucher.getGiaTriToiThieuDonHang() : BigDecimal.ZERO;
            if (currentTotal.compareTo(giaTriToiThieu) < 0) {
                response.put("error", "Tổng tiền (" + decimalFormat.format(currentTotal) + " ₫) chưa đạt giá trị tối thiểu (" + decimalFormat.format(giaTriToiThieu) + " ₫) để áp dụng voucher này.");
                return ResponseEntity.badRequest().body(response);
            }

            // Calculate Discount
            BigDecimal discountAmount = BigDecimal.ZERO;
            BigDecimal finalTotal = currentTotal;
            String discountDisplay = "0"; // For display like "10%" or "50,000 ₫"

            BigDecimal giaTriGiam = voucher.getGiaTriGiamGia() != null ? voucher.getGiaTriGiamGia() : BigDecimal.ZERO;

            if ("Phần trăm".equalsIgnoreCase(voucher.getLoaiGiamGia())) {
                discountAmount = currentTotal.multiply(giaTriGiam).divide(new BigDecimal(100), RoundingMode.HALF_UP);
                // Optional: Max discount amount for percentage vouchers
                // BigDecimal maxDiscount = voucher.getGiaTriGiamToiDa(); // Assuming field exists
                // if (maxDiscount != null && discountAmount.compareTo(maxDiscount) > 0) {
                //     discountAmount = maxDiscount;
                // }
                finalTotal = currentTotal.subtract(discountAmount);
                discountDisplay = giaTriGiam + "%";
            } else { // Fixed Amount
                discountAmount = giaTriGiam;
                finalTotal = currentTotal.subtract(discountAmount);
                discountDisplay = decimalFormat.format(giaTriGiam) + " ₫";
            }

            // Ensure final total is not negative
            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
                // Adjust discount amount if it made total negative
                discountAmount = currentTotal;
            }

            // Prepare response data
            response.put("thanhtien", decimalFormat.format(finalTotal) + " ₫"); // Formatted final total
            response.put("thanhTien", finalTotal.toPlainString()); // Raw final total
            response.put("tt", currentTotal.toPlainString());       // Raw original total
            response.put("idvoucher", String.valueOf(voucher.getMaGiamGiaId()));
            response.put("mucgiam", discountDisplay);              // Discount value/percentage display
            response.put("giamgiaRaw", discountAmount.toPlainString()); // Raw discount amount

            return ResponseEntity.ok(response);

        } else {
            response.put("error", "Mã voucher không tồn tại hoặc không hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }
    }



    @PostMapping("/delete")
    @Transactional // Ensure atomicity
    public ResponseEntity<Map<String, Object>> deleteHoaDonCT(@RequestParam Integer idHoaDonCT) {
        Map<String, Object> response = new HashMap<>();
        if (this.idhd == null) {
            response.put("error", "Chưa chọn hóa đơn.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Optional<ChiTietHoaDon> hoaDonCTOpt = hoaDonCTRepository.findById(idHoaDonCT);

            if (hoaDonCTOpt.isEmpty()) {
                response.put("error", "Không tìm thấy chi tiết hóa đơn để xóa.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            ChiTietHoaDon hoaDonCT = hoaDonCTOpt.get();

            // Verify it belongs to the current invoice 'idhd'
            if (!hoaDonCT.getHoaDon().getHoaDonId().equals(this.idhd)) {
                response.put("error", "Lỗi: Sản phẩm không thuộc hóa đơn đang chọn.");
                return ResponseEntity.badRequest().body(response);
            }

            hoaDonCTRepository.delete(hoaDonCT);
            System.out.println("Deleted HDCT ID: " + idHoaDonCT + " from HD ID: " + this.idhd);

            // Recalculate total after deletion
            BigDecimal newTotal = tongtien(this.idhd);

            response.put("success", "Xóa sản phẩm thành công.");
            response.put("newCartTotal", newTotal != null ? newTotal : BigDecimal.ZERO); // Send back new total

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error deleting HDCT ID " + idHoaDonCT + ": " + e.getMessage());
            e.printStackTrace();
            response.put("error", "Có lỗi xảy ra khi xóa sản phẩm.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/thanhtoan")
    @Transactional // Ensure the entire checkout process is atomic
    public String thanhtoan(
            @RequestParam(value = "idhoadon", required = true) Integer idhoadon, // Make required
            @RequestParam(value = "idkh", required = false) Integer idkh, // May not be present for Khach le
            @RequestParam(value = "idvoucher", required = false) Integer idvoucher,
            @RequestParam(value = "thanhtien", defaultValue = "0") BigDecimal thanhtien, // Final total from client
            @RequestParam(value = "tongtien", defaultValue = "0") BigDecimal tongtien, // Subtotal from client
            @RequestParam("sdt") String sdt,
            @RequestParam(value = "diachicuthe", defaultValue = "Trống") String diachicuthe,
            @RequestParam(value = "ghichu", defaultValue = "trống") String ghichu,
            @RequestParam("tenkh") String tenkh,
            @RequestParam("mucgiam") String giagiam, // Display value of discount
            // @RequestParam(value = "checkbox-tt", defaultValue = "1") String checkbox, // Assuming checkbox logic removed or handled differently
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        System.out.println("--- Starting Checkout Process for HoaDon ID: " + idhoadon + " ---");

        // 1. --- Basic Invoice Validation ---
        Optional<HoaDon> hdOpt = hoaDonRepository.findById(idhoadon);
        if (hdOpt.isEmpty() || !"Chờ thanh toán".equals(hdOpt.get().getTrangThai())) {
            redirectAttributes.addFlashAttribute("loi", "Hóa đơn không hợp lệ hoặc không tìm thấy (ID: " + idhoadon + ").");
            System.err.println("Checkout Failed: Invalid HoaDon state or not found for ID: " + idhoadon);
            this.idhd = null; // Clear selection if invalid
            return "redirect:/admin/pos";
        }
        HoaDon hd = hdOpt.get();

        List<ChiTietHoaDon> lshdct = hoaDonCTRepository.findhdct(idhoadon);
        if (lshdct == null || lshdct.isEmpty()) {
            redirectAttributes.addFlashAttribute("loi", "Hóa đơn (ID: " + idhoadon + ") trống, không có sản phẩm để thanh toán.");
            System.err.println("Checkout Failed: Empty cart for HD ID: " + idhoadon);
            return "redirect:/admin/pos/muahang/" + idhoadon;
        }
        System.out.println("Step 1 OK: HoaDon valid and cart not empty.");

        // 2. --- Final Stock Availability Check ---
        List<String> stockErrors = new ArrayList<>();
        boolean stockIssue = false;
        System.out.println("Step 2: Checking final stock availability...");
        for (ChiTietHoaDon hdct : lshdct) {
            ChiTietSanPham spct = hdct.getChiTietSanPham();
            if (spct == null || spct.getChiTietSanPhamId() == null) {
                redirectAttributes.addFlashAttribute("loi", "Lỗi nghiêm trọng: Chi tiết sản phẩm không hợp lệ trong hóa đơn.");
                System.err.println("Checkout Failed: Invalid SPCT found in HD ID: " + idhoadon);
                return "redirect:/admin/pos/muahang/" + idhoadon; // Critical error
            }
            int availableStock = getAvailableStock(spct.getChiTietSanPhamId());
            int quantityInCart = hdct.getSoLuong();

            System.out.printf("   - Checking SPCT ID: %d, Name: %s %s/%s. Need: %d, Available: %d%n",
                    spct.getChiTietSanPhamId(), spct.getSanPham().getTenSanPham(), spct.getMauSac().getTenMauSac(), spct.getKichCo().getTenKichCo(),
                    quantityInCart, availableStock);

            if (quantityInCart > availableStock) {
                stockIssue = true;
                String productName = String.format("%s (%s/%s)", spct.getSanPham().getTenSanPham(), spct.getMauSac().getTenMauSac(), spct.getKichCo().getTenKichCo());
                stockErrors.add(String.format("%s (cần %d, chỉ còn %d có sẵn)", productName, quantityInCart, availableStock));
            }
        }

        if (stockIssue) {
            String errorMessage = "Không đủ hàng: " + String.join("; ", stockErrors) + ". Vui lòng giảm số lượng hoặc xóa khỏi giỏ hàng.";
            redirectAttributes.addFlashAttribute("loi", errorMessage);
            System.err.println("Checkout Failed: Stock issue detected for HD ID: " + idhoadon + ". Errors: " + errorMessage);
            return "redirect:/admin/pos/muahang/" + idhoadon;
        }
        System.out.println("Step 2 OK: Stock available for all items.");

        // 3. --- Customer Handling ---
        System.out.println("Step 3: Handling Customer Info...");
        try {
            KhachHang selectedKh = null;
            if (idkh != null && idkh > 0) { // If a customer was explicitly selected via ID
                selectedKh = khachHangRepository.findById(idkh).orElse(null);
                if (selectedKh == null) {
                    redirectAttributes.addFlashAttribute("loi", "Khách hàng đã chọn (ID: "+idkh+") không còn tồn tại.");
                    System.err.println("Checkout Failed: Selected KH ID " + idkh + " not found.");
                    return "redirect:/admin/pos/muahang/" + idhoadon;
                }
                // Optional: Verify if phone/name from form matches selectedKh if needed
                System.out.println("   - Using existing customer: ID=" + selectedKh.getKhachHangId() + ", Name=" + selectedKh.getTen());
            } else if (!sdt.isBlank()) { // If phone number is provided (potentially new or existing)
                if (!sdt.matches("^0[0-9]{9}$")) {
                    redirectAttributes.addFlashAttribute("loi", "Số điện thoại '" + sdt + "' sai định dạng.");
                    System.err.println("Checkout Failed: Invalid phone format: " + sdt);
                    return "redirect:/admin/pos/muahang/" + idhoadon;
                }
                selectedKh = khachHangRepository.findBySoDienThoai(sdt);
                if (selectedKh != null) { // Found existing customer by phone
                    // Check if name matches (allow mismatch if name is "Khách lẻ" or empty)
                    if (!tenkh.equalsIgnoreCase(selectedKh.getTen()) && !"Khách lẻ".equalsIgnoreCase(selectedKh.getTen()) && !selectedKh.getTen().isBlank()) {
                        redirectAttributes.addFlashAttribute("loi", "SĐT " + sdt + " đã được đăng ký cho KH '" + selectedKh.getTen() + "'. Vui lòng kiểm tra lại.");
                        System.err.println("Checkout Failed: Phone " + sdt + " belongs to KH " + selectedKh.getTen() + ", but form name is " + tenkh);
                        return "redirect:/admin/pos/muahang/" + idhoadon;
                    }
                    System.out.println("   - Found existing customer by phone: ID=" + selectedKh.getKhachHangId() + ", Name=" + selectedKh.getTen());
                } else { // Create new customer
                    if (tenkh.isBlank() || "Khách lẻ".equalsIgnoreCase(tenkh)) {
                        redirectAttributes.addFlashAttribute("loi", "Vui lòng nhập tên khách hàng khi tạo mới bằng SĐT.");
                        System.err.println("Checkout Failed: Attempting to create new KH with phone " + sdt + " but no valid name provided.");
                        return "redirect:/admin/pos/muahang/" + idhoadon;
                    }
                    KhachHang khachHangMoi = new KhachHang();
                    String maKhachHang = "KH" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
                    // Ensure uniqueness (basic check)
                    while (khachHangRepository.existsByMaKhachHang(maKhachHang)) {
                        maKhachHang = "KH" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
                    }
                    khachHangMoi.setMaKhachHang(maKhachHang);
                    khachHangMoi.setTen(tenkh);
                    khachHangMoi.setSoDienThoai(sdt);
                    khachHangMoi.setTrangThai("Hoạt động");
                    // Add email/address if collected
                    // khachHangMoi.setEmail(diachicuthe); // If diachicuthe is email? Clarify field purpose
                    selectedKh = khachHangRepository.save(khachHangMoi);
                    khachHangRepository.flush(); // Ensure save happens before proceeding
                    System.out.println("   - Created new customer: ID=" + selectedKh.getKhachHangId() + ", Name=" + selectedKh.getTen() + ", Phone=" + sdt);
                }
            } else {
                // Case: Khách lẻ (no ID, no phone) - selectedKh remains null
                System.out.println("   - Proceeding as 'Khách lẻ'.");
            }
            hd.setKhachHang(selectedKh); // Assign the found/created/null customer to the invoice
        } catch (Exception e) {
            System.err.println("Checkout Failed: Error during customer handling: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("loi", "Lỗi khi xử lý thông tin khách hàng: " + e.getMessage());
            return "redirect:/admin/pos/muahang/" + idhoadon;
        }
        System.out.println("Step 3 OK: Customer assigned (ID: " + (hd.getKhachHang() != null ? hd.getKhachHang().getKhachHangId() : "null") + ")");


        // 4. --- Voucher Handling ---
        System.out.println("Step 4: Handling Voucher Info...");
        MaGiamGia vc = null;
        if (idvoucher != null && idvoucher > 0) {
            Optional<MaGiamGia> vcOpt = voucherRepository.findById(idvoucher);
            if (vcOpt.isEmpty() || vcOpt.get().getSoLuong() <= 0 || !"Hoạt động".equals(vcOpt.get().getTrangThai()) || (vcOpt.get().getNgayKetThuc() != null && vcOpt.get().getNgayKetThuc().isBefore(LocalDateTime.now()))) {
                redirectAttributes.addFlashAttribute("loi", "Voucher đã chọn không hợp lệ hoặc đã hết số lượng.");
                System.err.println("Checkout Failed: Invalid voucher selected (ID: " + idvoucher + ")");
                return "redirect:/admin/pos/muahang/" + idhoadon;
            }
            vc = vcOpt.get();

            // Verify minimum spend against calculated total *before* discount
            BigDecimal actualTotalBeforeDiscount = BigDecimal.ZERO;
            for (ChiTietHoaDon ct : lshdct) {
                actualTotalBeforeDiscount = actualTotalBeforeDiscount.add(ct.getGia().multiply(BigDecimal.valueOf(ct.getSoLuong())));
            }
            if (actualTotalBeforeDiscount.compareTo(vc.getGiaTriToiThieuDonHang()) < 0) {
                redirectAttributes.addFlashAttribute("loi", "Tổng tiền hàng chưa đạt giá trị tối thiểu (" + vc.getGiaTriToiThieuDonHang() + " ₫) để dùng voucher này.");
                System.err.println("Checkout Failed: Voucher minimum spend not met for voucher ID " + idvoucher);
                return "redirect:/admin/pos/muahang/" + idhoadon;
            }


            // Optional: Check if voucher already used by this customer (if customer exists)
            if (hd.getKhachHang() != null) {
                boolean checkVoucherUsed = hoaDonRepository.checkmavoucher(vc.getMaGiamGiaId(), hd.getKhachHang().getKhachHangId());
                if (checkVoucherUsed) {
                    redirectAttributes.addFlashAttribute("loi", "Voucher này đã được sử dụng bởi khách hàng '" + hd.getKhachHang().getTen() + "'.");
                    System.err.println("Checkout Failed: Voucher ID " + idvoucher + " already used by KH ID " + hd.getKhachHang().getKhachHangId());
                    return "redirect:/admin/pos/muahang/" + idhoadon;
                }
            }

            // Decrement voucher quantity
            vc.setSoLuong(vc.getSoLuong() - 1);
            voucherRepository.save(vc);
            hd.setMaGiamGia(vc); // Assign voucher to invoice
            System.out.println("   - Applied voucher: ID=" + vc.getMaGiamGiaId() + ", Code=" + vc.getTenMaGiamGia() + ", New Quantity=" + vc.getSoLuong());
        } else {
            hd.setMaGiamGia(null); // No voucher applied
            System.out.println("   - No voucher applied.");
        }
        System.out.println("Step 4 OK: Voucher handled.");


        // 5. --- Update Physical Stock --- (Critical Step)
        System.out.println("Step 5: Updating Physical Stock...");
        try {
            for (ChiTietHoaDon hdct : lshdct) {
                ChiTietSanPham spct = hdct.getChiTietSanPham();
                int soLuongBan = hdct.getSoLuong();
                int soLuongTonHienTai = spct.getSoLuongTon();

                // Final paranoid check
                if (soLuongTonHienTai < soLuongBan) {
                    // This should NOT happen due to check #2, indicates a concurrency issue or bug
                    System.err.println("CRITICAL ERROR during stock update: SPCT ID " + spct.getChiTietSanPhamId() + " physical stock (" + soLuongTonHienTai + ") is less than sold quantity (" + soLuongBan + ") for HD ID " + idhoadon);
                    redirectAttributes.addFlashAttribute("loi", "Lỗi hệ thống nghiêm trọng khi cập nhật kho. Vui lòng thử lại hoặc liên hệ hỗ trợ.");
                    // Consider rolling back manually or letting @Transactional handle it
                    throw new RuntimeException("Inconsistent stock detected during final update for SPCT ID " + spct.getChiTietSanPhamId());
                }

                spct.setSoLuongTon(soLuongTonHienTai - soLuongBan);
                sanPhamChiTietRepository.save(spct);
                System.out.printf("   - Updated SPCT ID: %d. Old Stock: %d, Sold: %d, New Stock: %d%n",
                        spct.getChiTietSanPhamId(), soLuongTonHienTai, soLuongBan, spct.getSoLuongTon());
            }
        } catch (Exception e) {
            System.err.println("Checkout Failed: Error during physical stock update: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("loi", "Lỗi khi cập nhật số lượng tồn kho. Vui lòng thử lại.");
            // Let @Transactional handle rollback
            throw e; // Re-throw to ensure rollback
        }
        System.out.println("Step 5 OK: Physical stock updated.");


        // 6. --- Finalize HoaDon ---
        System.out.println("Step 6: Finalizing HoaDon...");
        NhanVien nhanVien = getCurrentNhanVien();
        if (nhanVien == null) {
            // This should ideally be handled by security context before allowing access
            redirectAttributes.addFlashAttribute("loi", "Lỗi: Không tìm thấy thông tin nhân viên đăng nhập. Vui lòng đăng nhập lại.");
            System.err.println("Checkout Failed: Could not get current NhanVien.");
            // Let @Transactional handle rollback if needed
            throw new IllegalStateException("Cannot complete checkout without a valid staff member logged in.");
        }

        // Recalculate final total server-side based on items and applied voucher
        BigDecimal finalCalculatedTotal = BigDecimal.ZERO;
        for (ChiTietHoaDon ct : lshdct) {
            finalCalculatedTotal = finalCalculatedTotal.add(ct.getTongTien()); // Sum of line item totals
        }
        // Apply discount if voucher exists
        if (hd.getMaGiamGia() != null) {
            // Recalculate discount based on rules HERE to ensure accuracy
            BigDecimal discountAmount = BigDecimal.ZERO;
            MaGiamGia appliedVoucher = hd.getMaGiamGia();
            BigDecimal totalBeforeDiscount = BigDecimal.ZERO; // Calculate based on unit price * qty
            for(ChiTietHoaDon ct : lshdct) { totalBeforeDiscount = totalBeforeDiscount.add(ct.getGia().multiply(BigDecimal.valueOf(ct.getSoLuong()))); }

            if ("Phần trăm".equalsIgnoreCase(appliedVoucher.getLoaiGiamGia())) {
                discountAmount = totalBeforeDiscount.multiply(appliedVoucher.getGiaTriGiamGia()).divide(new BigDecimal(100), RoundingMode.HALF_UP);
            } else {
                discountAmount = appliedVoucher.getGiaTriGiamGia();
            }
            // Ensure discount doesn't exceed total
            discountAmount = discountAmount.min(totalBeforeDiscount);
            finalCalculatedTotal = totalBeforeDiscount.subtract(discountAmount);
            if (finalCalculatedTotal.compareTo(BigDecimal.ZERO) < 0) { finalCalculatedTotal = BigDecimal.ZERO;}
            System.out.println("   - Server-side calculated discount: " + discountAmount);
        }
        System.out.println("   - Server-side calculated final total: " + finalCalculatedTotal);
        // Optional: Compare server calculated total with client total 'thanhtien' for consistency check
        if (thanhtien.compareTo(finalCalculatedTotal) != 0) {
            System.err.println("WARNING: Client final total (" + thanhtien + ") differs from server calculated final total (" + finalCalculatedTotal + ") for HD ID " + idhoadon + ". Using server value.");
        }


        hd.setNhanVien(nhanVien);
        // hd.setMaGiamGia(vc); // Already set
        hd.setTongTien(finalCalculatedTotal); // Use SERVER-CALCULATED final total
        hd.setKenhBanHang("POS");
        hd.setTrangThai("Đã thanh toán");
        hd.setNgayBan(LocalDateTime.now());
;


        hoaDonRepository.save(hd);
        System.out.println("Step 6 OK: HoaDon finalized and saved. ID: " + hd.getHoaDonId() + ", Status: " + hd.getTrangThai());

        // 7. --- Post-Checkout Cleanup ---
        this.idhd = null; // Clear the currently selected invoice ID
        redirectAttributes.addFlashAttribute("message", "Thanh toán thành công cho hóa đơn " + hd.getMaHoaDon() + ".");
        System.out.println("--- Checkout Process Completed Successfully for HoaDon ID: " + hd.getHoaDonId() + " ---");

        return "redirect:/admin/pos/in-hoa-don/" + hd.getHoaDonId();
    }


    @GetMapping("/in-hoa-don/{id}")
    public String showPrintableInvoice(@PathVariable("id") Integer hoaDonId, Model model, RedirectAttributes redirectAttributes) {
        System.out.println("Attempting to show invoice for printing, ID: " + hoaDonId);

        // Fetch HoaDon with necessary details eagerly to prevent LazyInitializationException
        // Option 1: Custom Repository Method (Recommended)
        // Optional<HoaDon> hoaDonOpt = hoaDonRepository.findByIdFetchingDetails(hoaDonId);

        // Option 2: Standard findById and manual initialization (shown here)
        Optional<HoaDon> hoaDonOpt = hoaDonRepository.findById(hoaDonId);

        if (hoaDonOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("loi", "Không tìm thấy hóa đơn ID: " + hoaDonId + " để in.");
            System.err.println("Invoice Print Failed: HoaDon not found for ID: " + hoaDonId);
            return "redirect:/admin/pos";
        }
        HoaDon hoaDon = hoaDonOpt.get();

        // Ensure the invoice is in a printable state (e.g., Đã thanh toán)
        if (!"Đã thanh toán".equals(hoaDon.getTrangThai())) {
            // Or maybe allow printing other statuses? Depends on requirements.
            redirectAttributes.addFlashAttribute("loi", "Chỉ có thể in hóa đơn đã thanh toán (ID: " + hoaDonId + ").");
            System.err.println("Invoice Print Failed: HoaDon ID " + hoaDonId + " status is " + hoaDon.getTrangThai());
            return "redirect:/admin/pos";
        }


        // Manually initialize lazy-loaded collections/entities needed in the template
        try {
            System.out.println("   Initializing details for HoaDon ID: " + hoaDonId);
            if (hoaDon.getChiTietHoaDons() != null) {
                System.out.println("   Initializing " + hoaDon.getChiTietHoaDons().size() + " cart items...");
                hoaDon.getChiTietHoaDons().forEach(detail -> {
                    if (detail.getChiTietSanPham() != null) {
                        // Access fields needed in the template to trigger loading
                        ChiTietSanPham ctsp = detail.getChiTietSanPham();
                        if(ctsp.getSanPham() != null) ctsp.getSanPham().getTenSanPham();
                        if(ctsp.getMauSac() != null) ctsp.getMauSac().getTenMauSac();
                        if(ctsp.getKichCo() != null) ctsp.getKichCo().getTenKichCo();
                    } else {
                        System.err.println("   Warning: Null ChiTietSanPham found for ChiTietHoaDon ID: " + detail.getChiTietHoaDonId());
                    }
                });
            } else {
                System.err.println("   Warning: ChiTietHoaDons list is null for HoaDon ID: " + hoaDonId);
            }

            // Initialize related entities
            if (hoaDon.getMaGiamGia() != null) {
                hoaDon.getMaGiamGia().getTenMaGiamGia(); // Load voucher name
                System.out.println("   Initialized Voucher: " + hoaDon.getMaGiamGia().getTenMaGiamGia());
            }
            if (hoaDon.getKhachHang() != null) {
                hoaDon.getKhachHang().getTen(); // Load customer name
                System.out.println("   Initialized Customer: " + hoaDon.getKhachHang().getTen());
            } else {
                System.out.println("   No customer associated.");
            }
            if (hoaDon.getNhanVien() != null) {
                hoaDon.getNhanVien().getTen(); // Load staff name
                System.out.println("   Initialized NhanVien: " + hoaDon.getNhanVien().getTen());
            } else {
                System.err.println("   Warning: No NhanVien associated with HoaDon ID: " + hoaDonId);
            }
            System.out.println("   Initialization complete.");

        } catch (LazyInitializationException lazyEx) {
            System.err.println("Invoice Print Failed: LazyInitializationException for HoaDon ID " + hoaDonId + ". Need Eager Fetching strategy. Error: " + lazyEx.getMessage());
            redirectAttributes.addFlashAttribute("loi", "Lỗi tải chi tiết hóa đơn. Vui lòng thử lại hoặc liên hệ hỗ trợ.");
            return "redirect:/admin/pos"; // Redirect back
        } catch (Exception e) {
            System.err.println("Invoice Print Failed: Unexpected error loading details for HoaDon ID " + hoaDonId + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("loi", "Lỗi hệ thống khi tải hóa đơn.");
            return "redirect:/admin/pos";
        }

        // Calculate display values (Subtotal and Discount) server-side for accuracy
        BigDecimal tienHangTruocGiam = BigDecimal.ZERO; // Sum of (unit price * quantity)
        if (hoaDon.getChiTietHoaDons() != null) {
            for (ChiTietHoaDon ct : hoaDon.getChiTietHoaDons()) {
                if (ct != null && ct.getGia() != null && ct.getSoLuong() != null) {
                    tienHangTruocGiam = tienHangTruocGiam.add(ct.getGia().multiply(BigDecimal.valueOf(ct.getSoLuong())));
                }
            }
        }

        // Calculate discount as the difference between subtotal and final total
        BigDecimal tongGiamGia = BigDecimal.ZERO;
        if (hoaDon.getTongTien() != null) {
            tongGiamGia = tienHangTruocGiam.subtract(hoaDon.getTongTien());
            // Ensure discount isn't negative (safety check)
            if (tongGiamGia.compareTo(BigDecimal.ZERO) < 0) {
                System.err.println("Warning: Calculated discount is negative for HD ID " + hoaDonId + ". Setting discount to 0.");
                tongGiamGia = BigDecimal.ZERO;
            }
        } else {
            System.err.println("Warning: hoaDon.tongTien is null for ID " + hoaDonId + ". Cannot reliably calculate discount for display.");
            // Might attempt to recalculate based on voucher if present, but difference is safer
        }

        System.out.println("   Calculated for display - Subtotal: " + tienHangTruocGiam + ", Discount: " + tongGiamGia + ", Final: " + hoaDon.getTongTien());

        model.addAttribute("hoaDon", hoaDon); // The fully loaded HoaDon object
        model.addAttribute("tongTienTruocGiamDisplay", tienHangTruocGiam); // Subtotal before discount
        model.addAttribute("tienGiamGiaDisplay", tongGiamGia); // Total discount amount

        System.out.println("   Rendering invoice print view.");
        return "banhang/invoice-print";
    }

}