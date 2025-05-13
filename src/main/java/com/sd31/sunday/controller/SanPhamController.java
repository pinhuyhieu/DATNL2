package com.sd31.sunday.controller;

import com.sd31.sunday.model.HinhAnh;
import com.sd31.sunday.model.SanPham;
import com.sd31.sunday.repository.HinhAnhRepository;
import com.sd31.sunday.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/san-pham")
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private ThuongHieuService thuongHieuService;

    @Autowired
    private ChatLieuService chatLieuService;

    @Autowired
    private KieuDangService kieuDangService;

    @Autowired
    private HinhAnhRepository hinhAnhRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @ModelAttribute
    public void addActivePageAttribute(Model model) {
        model.addAttribute("activePage", "sanPhams");
    }

    @GetMapping
    public String showSanPhamList(Model model) {
        model.addAttribute("sanPhams", sanPhamService.getAllSanPhams());
        model.addAttribute("sanPham", new SanPham());
        loadCommonData(model);
        return "admin/product/san-pham";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        SanPham sanPham = sanPhamService.getSanPhamById(id);
        if (sanPham != null) {
            model.addAttribute("sanPham", sanPham);
            loadCommonData(model);
            return "admin/product/san-pham";
        }
        return "redirect:/admin/san-pham";
    }

    @PostMapping("/save")
    public String saveSanPham(@ModelAttribute @Valid SanPham sanPham,
                              BindingResult result,
                              @RequestParam("images") MultipartFile[] images,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Luôn thêm dữ liệu cần thiết vào model
        model.addAttribute("sanPhams", sanPhamService.getAllSanPhams());
        loadCommonData(model);

        // Gom lỗi từ @Valid
        Map<String, String> errors = new HashMap<>();
        if (result.hasErrors()) {
            result.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
        }

        // Gom lỗi từ kiểm tra thủ công trong Service (bao gồm check trống)
        Map<String, String> customErrors = sanPhamService.getValidationErrors(sanPham);
        errors.putAll(customErrors);

        // Kiểm tra tên sản phẩm trùng lặp (cho trường hợp thêm mới)
        if (sanPham.getSanPhamId() == null && sanPhamService.existsByTenSanPham(sanPham.getTenSanPham())) {
            errors.put("tenSanPham", "Tên sản phẩm đã tồn tại");
        }

        // Validate cho images (giữ nguyên phần validate ảnh từ code mới)
        if (images == null || images.length == 0 || Arrays.stream(images).allMatch(MultipartFile::isEmpty)) {
            errors.put("images", "Vui lòng chọn ít nhất một hình ảnh.");
        } else if (images.length > 5) {
            errors.put("images", "Bạn chỉ được tải lên tối đa 5 hình ảnh.");
        } else {
            for (int i = 0; i < images.length; i++) {
                MultipartFile image = images[i];
                if (!image.isEmpty()) {
                    if (image.getSize() > 5 * 1024 * 1024) {
                        errors.put("images_" + i, "Hình ảnh thứ " + (i + 1) + " vượt quá kích thước tối đa 5MB.");
                    }
                    String contentType = image.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        errors.put("images_" + i, "Hình ảnh thứ " + (i + 1) + " không phải là định dạng hình ảnh hợp lệ.");
                    }
                }
            }
        }

        // Nếu có lỗi, hiển thị tất cả cùng lúc
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return "admin/product/san-pham";
        }

        // Nếu không có lỗi, lưu sản phẩm
        try {
            // Lưu sản phẩm trước
            SanPham savedProduct = sanPhamService.save(sanPham);

            // Lưu hình ảnh
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        String imageUrl = fileStorageService.saveFile(image);
                        HinhAnh hinhAnh = new HinhAnh();
                        hinhAnh.setHinhAnhUrl(imageUrl);
                        hinhAnh.setSanPham(savedProduct);
                        hinhAnhRepository.save(hinhAnh);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được lưu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi lưu sản phẩm: " + e.getMessage());
        }

        return "redirect:/admin/san-pham";
    }





    @GetMapping("/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            SanPham updatedSanPham = sanPhamService.updateTrangThai(id);
            response.put("success", true);
            response.put("message", "Đã cập nhật trạng thái sản phẩm thành công!");
            response.put("newStatus", updatedSanPham.getTrangThai());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/search")
    public String searchSanPham(@RequestParam(required = false) String keyword, Model model) {
        List<SanPham> searchResults = sanPhamService.searchSanPham(keyword);
        model.addAttribute("sanPhams", searchResults);
        model.addAttribute("sanPham", new SanPham());
        model.addAttribute("searchKeyword", keyword);
        loadCommonData(model);
        return "admin/product/san-pham";
    }

    @GetMapping("/filter")
    public String filterSanPham(
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer thuongHieuId,
            @RequestParam(required = false) Integer chatLieuId,
            @RequestParam(required = false) Integer kieuDangId,
            @RequestParam(required = false) String trangThai,
            Model model) {
        List<SanPham> filteredSanPhams = sanPhamService.filterSanPham(
                danhMucId, thuongHieuId, chatLieuId, kieuDangId, trangThai);
        model.addAttribute("sanPhams", filteredSanPhams);
        model.addAttribute("sanPham", new SanPham());
        model.addAttribute("selectedDanhMucId", danhMucId);
        model.addAttribute("selectedThuongHieuId", thuongHieuId);
        model.addAttribute("selectedChatLieuId", chatLieuId);
        model.addAttribute("selectedKieuDangId", kieuDangId);
        model.addAttribute("selectedTrangThai", trangThai);
        loadCommonData(model);
        return "admin/product/san-pham";
    }

    private void loadCommonData(Model model) {
        model.addAttribute("danhMucs", danhMucService.getAllDanhMucByTrangThai());
        model.addAttribute("thuongHieus", thuongHieuService.getAllThuongHieuByTrangThai());
        model.addAttribute("chatLieus", chatLieuService.getAllChatLieuByTrangThai());
        model.addAttribute("kieuDangs", kieuDangService.getAllKieuDangByTrangThai());
    }

    // Helper method to map validation errors
    private Map<String, String> getValidationErrors(SanPham sanPham) {
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
}