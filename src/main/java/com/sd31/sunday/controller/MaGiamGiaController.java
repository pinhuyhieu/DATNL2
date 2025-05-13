package com.sd31.sunday.controller;

import com.sd31.sunday.model.MaGiamGia;
import com.sd31.sunday.service.MaGiamGiaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/magiam")
public class MaGiamGiaController {

    @Autowired
    private MaGiamGiaService maGiamGiaService;

    @GetMapping
    public String index(Model model,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String trangThai,
                       @RequestParam(required = false) String loaiGiamGia) {
        List<MaGiamGia> maGiamGias;
        model.addAttribute("activePage", "maGiam");

        // Lọc mã giảm giá theo điều kiện
        if (keyword != null && !keyword.isEmpty()) {
            maGiamGias = maGiamGiaService.findByTenMaGiamGiaContainingIgnoreCase(keyword);
        } else if (trangThai != null && !trangThai.isEmpty() && loaiGiamGia != null && !loaiGiamGia.isEmpty()) {
            maGiamGias = maGiamGiaService.findByTrangThaiAndLoaiGiamGia(trangThai, loaiGiamGia);
        } else if (trangThai != null && !trangThai.isEmpty()) {
            maGiamGias = maGiamGiaService.findByTrangThai(trangThai);
        } else if (loaiGiamGia != null && !loaiGiamGia.isEmpty()) {
            maGiamGias = maGiamGiaService.findByLoaiGiamGia(loaiGiamGia);
        } else {
            maGiamGias = maGiamGiaService.getAllMaGiamGia();
        }

        model.addAttribute("maGiamGias", maGiamGias);
        model.addAttribute("maGiamGia", new MaGiamGia());
        model.addAttribute("isEdit", false);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTrangThai", trangThai);
        model.addAttribute("selectedLoaiGiamGia", loaiGiamGia);
        return "admin/ma-giam-gia";
    }

    @PostMapping("/add")
    public String addMaGiamGia(@ModelAttribute("maGiamGia") @Valid MaGiamGia maGiamGia,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        validateMaGiamGia(maGiamGia, result, true);

        if (result.hasErrors()) {
            return returnWithError(model, maGiamGia, false);
        }

        try {
            maGiamGiaService.saveMaGiamGia(maGiamGia);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mã giảm giá thành công!");
            return "redirect:/admin/magiam";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên mã giảm giá đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, maGiamGia, false);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        MaGiamGia maGiamGia = maGiamGiaService.getMaGiamGiaById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá với ID: " + id));
        
        model.addAttribute("maGiamGia", maGiamGia);
        model.addAttribute("activePage", "maGiam");
        model.addAttribute("isEdit", true);
        model.addAttribute("maGiamGias", maGiamGiaService.getAllMaGiamGia());
        return "admin/ma-giam-gia";
    }

    @PostMapping("/update")
    public String updateMaGiamGia(@ModelAttribute("maGiamGia") @Valid MaGiamGia maGiamGia,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        validateMaGiamGia(maGiamGia, result, false);

        if (result.hasErrors()) {
            return returnWithError(model, maGiamGia, true);
        }

        try {
            maGiamGiaService.updateMaGiamGia(maGiamGia);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật mã giảm giá thành công!");
            return "redirect:/admin/magiam";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Tên mã giảm giá đã tồn tại. Vui lòng chọn tên khác.");
            return returnWithError(model, maGiamGia, true);
        }
    }

    @PostMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<String> toggleTrangThai(@PathVariable("id") Integer id) {
        try {
            maGiamGiaService.toggleTrangThai(id);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }

    @GetMapping("/filter")
    public String filterMaGiamGia(@RequestParam(value = "trangThai", required = false) String trangThai,
                                 @RequestParam(value = "loaiGiamGia", required = false) String loaiGiamGia,
                                 Model model) {
        List<MaGiamGia> maGiamGias;
        if (trangThai != null && !trangThai.isEmpty() && loaiGiamGia != null && !loaiGiamGia.isEmpty()) {
            maGiamGias = maGiamGiaService.findByTrangThaiAndLoaiGiamGia(trangThai, loaiGiamGia);
        } else if (trangThai != null && !trangThai.isEmpty()) {
            maGiamGias = maGiamGiaService.findByTrangThai(trangThai);
        } else if (loaiGiamGia != null && !loaiGiamGia.isEmpty()) {
            maGiamGias = maGiamGiaService.findByLoaiGiamGia(loaiGiamGia);
        } else {
            maGiamGias = maGiamGiaService.getAllMaGiamGia();
        }
        
        model.addAttribute("maGiamGias", maGiamGias);
        return "admin/ma-giam-gia :: maGiamGiaTable";
    }

    private String returnWithError(Model model, MaGiamGia maGiamGia, boolean isEdit) {
        model.addAttribute("maGiamGia", maGiamGia);
        model.addAttribute("activePage", "maGiam");
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("maGiamGias", maGiamGiaService.getAllMaGiamGia());
        return "admin/ma-giam-gia";
    }

    // Phương thức validate mã giảm giá
    private void validateMaGiamGia(MaGiamGia maGiamGia, BindingResult result, boolean isNew) {
        // Kiểm tra tên mã giảm giá
        if (maGiamGia.getTenMaGiamGia() == null || maGiamGia.getTenMaGiamGia().trim().isEmpty()) {
            result.addError(new FieldError("maGiamGia", "tenMaGiamGia", "Tên mã giảm giá không được để trống"));
        } else if (maGiamGia.getTenMaGiamGia().length() > 20) {
            result.addError(new FieldError("maGiamGia", "tenMaGiamGia", "Tên mã giảm giá không được vượt quá 20 ký tự"));
        } else if (!maGiamGia.getTenMaGiamGia().matches("^[A-Z0-9]+$")) {
            result.addError(new FieldError("maGiamGia", "tenMaGiamGia", "Tên mã giảm giá chỉ được chứa chữ in hoa và số"));
        } else if (isNew && maGiamGiaService.existsByTenMaGiamGia(maGiamGia.getTenMaGiamGia())) {
            result.addError(new FieldError("maGiamGia", "tenMaGiamGia", "Tên mã giảm giá đã tồn tại"));
        }

        // Kiểm tra loại giảm giá
        if (maGiamGia.getLoaiGiamGia() == null || maGiamGia.getLoaiGiamGia().trim().isEmpty()) {
            result.addError(new FieldError("maGiamGia", "loaiGiamGia", "Loại giảm giá không được để trống"));
        } else if (!maGiamGia.getLoaiGiamGia().equals("Phần trăm") && !maGiamGia.getLoaiGiamGia().equals("Số tiền cố định")) {
            result.addError(new FieldError("maGiamGia", "loaiGiamGia", "Loại giảm giá phải là 'Phần trăm' hoặc 'Số tiền cố định'"));
        }

        // Kiểm tra giá trị giảm giá
        if (maGiamGia.getGiaTriGiamGia() == null) {
            result.addError(new FieldError("maGiamGia", "giaTriGiamGia", "Giá trị giảm giá không được để trống"));
        } else if (maGiamGia.getLoaiGiamGia() != null) {
            if (maGiamGia.getLoaiGiamGia().equals("Phần trăm")) {
                if (maGiamGia.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0 ||
                        maGiamGia.getGiaTriGiamGia().compareTo(new BigDecimal("50")) > 0) {
                    result.addError(new FieldError("maGiamGia", "giaTriGiamGia",
                            "Phần trăm giảm giá phải từ 1% đến 50%"));
                }
            } else if (maGiamGia.getLoaiGiamGia().equals("Số tiền cố định")) {
                if (maGiamGia.getGiaTriGiamGia().compareTo(new BigDecimal("10000")) < 0) {
                    result.addError(new FieldError("maGiamGia", "giaTriGiamGia",
                            "Giá trị giảm giá tối thiểu là 10,000đ"));
                } else if (maGiamGia.getGiaTriGiamGia().compareTo(new BigDecimal("1000000")) > 0) {
                    result.addError(new FieldError("maGiamGia", "giaTriGiamGia",
                            "Giá trị giảm giá tối đa là 1,000,000đ"));
                }
                if (maGiamGia.getGiaTriToiThieuDonHang() != null &&
                        maGiamGia.getGiaTriGiamGia().compareTo(maGiamGia.getGiaTriToiThieuDonHang()) >= 0) {
                    result.addError(new FieldError("maGiamGia", "giaTriGiamGia",
                            "Giá trị giảm giá phải nhỏ hơn giá trị tối thiểu đơn hàng"));
                }
            }
        }

        // Kiểm tra ngày bắt đầu và kết thúc
        LocalDateTime now = LocalDateTime.now();
        if (maGiamGia.getNgayBatDau() == null) {
            result.addError(new FieldError("maGiamGia", "ngayBatDau", "Ngày bắt đầu không được để trống"));
        } else if (maGiamGia.getNgayBatDau().isBefore(now)) {
            result.addError(new FieldError("maGiamGia", "ngayBatDau", "Ngày bắt đầu phải sau thời điểm hiện tại"));
        }

        if (maGiamGia.getNgayKetThuc() == null) {
            result.addError(new FieldError("maGiamGia", "ngayKetThuc", "Ngày kết thúc không được để trống"));
        } else if (maGiamGia.getNgayBatDau() != null) {
            if (maGiamGia.getNgayKetThuc().isBefore(maGiamGia.getNgayBatDau())) {
                result.addError(new FieldError("maGiamGia", "ngayKetThuc", "Ngày kết thúc phải sau ngày bắt đầu"));
            }
            LocalDateTime maxEndDate = maGiamGia.getNgayBatDau().plusMonths(3);
            if (maGiamGia.getNgayKetThuc().isAfter(maxEndDate)) {
                result.addError(new FieldError("maGiamGia", "ngayKetThuc", 
                    "Thời gian hiệu lực của mã giảm giá không được vượt quá 3 tháng"));
            }
        }

        // Kiểm tra số lượng
        if (maGiamGia.getSoLuong() == null) {
            result.addError(new FieldError("maGiamGia", "soLuong", "Số lượng không được để trống"));
        } else if (maGiamGia.getSoLuong() < 10) {
            result.addError(new FieldError("maGiamGia", "soLuong", "Số lượng mã giảm giá tối thiểu là 10"));
        } else if (maGiamGia.getSoLuong() > 1000) {
            result.addError(new FieldError("maGiamGia", "soLuong", "Số lượng mã giảm giá tối đa là 1000"));
        }

        // Kiểm tra giá trị tối thiểu đơn hàng
        if (maGiamGia.getGiaTriToiThieuDonHang() == null) {
            result.addError(new FieldError("maGiamGia", "giaTriToiThieuDonHang", "Giá trị tối thiểu đơn hàng không được để trống"));
        } else {
            if (maGiamGia.getGiaTriToiThieuDonHang().compareTo(new BigDecimal("50000")) < 0) {
                result.addError(new FieldError("maGiamGia", "giaTriToiThieuDonHang", 
                    "Giá trị tối thiểu đơn hàng phải từ 50,000đ"));
            } else if (maGiamGia.getGiaTriToiThieuDonHang().compareTo(new BigDecimal("5000000")) > 0) {
                result.addError(new FieldError("maGiamGia", "giaTriToiThieuDonHang", 
                    "Giá trị tối thiểu đơn hàng không được vượt quá 5,000,000đ"));
            }
        }
    }
}