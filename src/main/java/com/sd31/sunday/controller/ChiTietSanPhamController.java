package com.sd31.sunday.controller;

import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.SanPham;
import com.sd31.sunday.service.ChiTietSanPhamService;
import com.sd31.sunday.service.KichCoService;
import com.sd31.sunday.service.MauSacService;
import com.sd31.sunday.service.SanPhamService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/chi-tiet-san-pham")
public class ChiTietSanPhamController {

    private static final Logger logger = LoggerFactory.getLogger(ChiTietSanPhamController.class);

    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;
    @Autowired
    private SanPhamService sanPhamService;
    @Autowired
    private MauSacService mauSacService;
    @Autowired
    private KichCoService kichCoService;

    @GetMapping("/list/{sanPhamId}")
    public String showChiTietSanPhamList(@PathVariable Integer sanPhamId, Model model) {
        SanPham sanPham = sanPhamService.getSanPhamById(sanPhamId);
        if (sanPham == null) {
            return "redirect:/admin/san-pham";
        }
        List<ChiTietSanPham> chiTietSanPhams = chiTietSanPhamService.findBySanPhamId(sanPhamId);
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("chiTietSanPhams", chiTietSanPhams);
        ChiTietSanPham ctsp = new ChiTietSanPham();
        ctsp.setSanPham(sanPham);
        model.addAttribute("ctsp", ctsp);
        model.addAttribute("trangThaiOptions", Arrays.asList("Hoạt động", "Không hoạt động")); // ** Thêm lại options trạng thái **
        loadCommonData(model);
        return "admin/product/chi-tiet-san-pham";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        ChiTietSanPham ctsp = chiTietSanPhamService.getChiTietSanPhamById(id);
        if (ctsp != null) {
            model.addAttribute("ctsp", ctsp);
            loadCommonData(model);
            model.addAttribute("sanPham", ctsp.getSanPham());
            model.addAttribute("chiTietSanPhams", chiTietSanPhamService.findBySanPhamId(ctsp.getSanPham().getSanPhamId()));
            model.addAttribute("trangThaiOptions", Arrays.asList("Hoạt động", "Không hoạt động")); // ** Thêm lại options trạng thái **
            return "admin/product/chi-tiet-san-pham";
        }
        return "redirect:/admin/chi-tiet-san-pham/list/" + (ctsp != null ? ctsp.getSanPham().getSanPhamId() : 0);
    }


    @PostMapping("/save")
    public String saveChiTietSanPham(@ModelAttribute @Valid ChiTietSanPham ctsp,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        logger.info("Bắt đầu lưu ChiTietSanPham: {}", ctsp);

        if (result.hasErrors()) {
            logger.warn("Lỗi validation khi lưu ChiTietSanPham: {}", result.getAllErrors());
            loadCommonData(model);
            model.addAttribute("sanPham", ctsp.getSanPham());
            model.addAttribute("chiTietSanPhams", chiTietSanPhamService.findBySanPhamId(ctsp.getSanPham().getSanPhamId()));
            model.addAttribute("trangThaiOptions", Arrays.asList("Hoạt động", "Không hoạt động")); // ** Thêm lại options trạng thái **
            return "admin/product/chi-tiet-san-pham";
        }

        try {
            SanPham sanPham = sanPhamService.getSanPhamById(ctsp.getSanPham().getSanPhamId());
            if (sanPham == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm cha không tồn tại!");
                return "redirect:/admin/san-pham";
            }
            ctsp.setSanPham(sanPham);


            ChiTietSanPham savedCtsp = chiTietSanPhamService.save(ctsp);
            logger.info("Lưu ChiTietSanPham thành công, ID: {}", savedCtsp.getChiTietSanPhamId());
            redirectAttributes.addFlashAttribute("successMessage", "Chi tiết sản phẩm đã được lưu thành công!");

        } catch (IllegalArgumentException e) {
            // ** Bắt exception trùng lặp **
            model.addAttribute("errorMessage", e.getMessage()); // Thêm thông báo lỗi vào model

            // ** Quan trọng: GIỮ LẠI DỮ LIỆU FORM VÀ LOAD LẠI DATA CHO FORM **
            loadCommonData(model);
            model.addAttribute("ctsp", ctsp); // ** Truyền lại đối tượng ctsp đã nhập để giữ lại dữ liệu **
            model.addAttribute("sanPham", ctsp.getSanPham());
            model.addAttribute("chiTietSanPhams", chiTietSanPhamService.findBySanPhamId(ctsp.getSanPham().getSanPhamId()));
            model.addAttribute("trangThaiOptions", Arrays.asList("Hoạt động", "Không hoạt động")); // ** Thêm lại options trạng thái **
            return "admin/product/chi-tiet-san-pham"; // Trả về form để hiển thị lỗi và giữ lại dữ liệu

        } catch (Exception e) {
            logger.error("Lỗi khi lưu ChiTietSanPham: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi lưu chi tiết sản phẩm! Vui lòng kiểm tra lại.");
        }
        return "redirect:/admin/chi-tiet-san-pham/list/" + ctsp.getSanPham().getSanPhamId();
    }

    @PostMapping("/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<String> toggleStatus(@PathVariable Integer id) {
        try {
            ChiTietSanPham ctsp = chiTietSanPhamService.getChiTietSanPhamById(id);
            if (ctsp == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chi tiết sản phẩm!");
            }

            // Đảo trạng thái
            ctsp.setTrangThai(ctsp.getTrangThai().equals("Hoạt động") ? "Không hoạt động" : "Hoạt động");
            chiTietSanPhamService.save(ctsp);

            return ResponseEntity.ok("Trạng thái đã được cập nhật!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái!");
        }
    }


    private void loadCommonData(Model model) {
        model.addAttribute("mauSacs", mauSacService.getAllMauSac());
        model.addAttribute("kichCos", kichCoService.getAllKichCo());

    }
}