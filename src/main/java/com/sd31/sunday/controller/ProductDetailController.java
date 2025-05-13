package com.sd31.sunday.controller;

import com.sd31.sunday.DTO.DataDTO;
import com.sd31.sunday.model.*;
import com.sd31.sunday.repository.ChiTietSanPhamRepository;
import com.sd31.sunday.service.SanPhamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/products")
public class ProductDetailController {
    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    private static final String ACTIVE_STATUS = "Hoạt động"; // Can be defined here or accessed from service if public


    @GetMapping("/{id}")
    public String chiTietSanPham(@PathVariable("id") Integer id, Model model, HttpSession session) {
        SanPham sanPham = sanPhamService.getSanPhamById(id);

        // Important: Check if the SanPham itself is active or exists
        if (sanPham == null || !ACTIVE_STATUS.equals(sanPham.getTrangThai())) {
            model.addAttribute("errorMessage", "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
            return "customer/error-product-not-found"; // Create an error page
            // Or redirect: return "redirect:/";
        }

        // Fetch only ACTIVE colors associated with this product
        List<MauSac> mauSacList = sanPhamService.getActiveMauSacBySanPhamId(id);
        model.addAttribute("mauSacList", mauSacList);

        Integer defaultMauSacId = null;
        List<DataDTO> listKichCoDTO = new ArrayList<>();
        Integer initialAvailableStock = 0; // Default to 0
        Integer initialPhysicalStock = 0; // Default to 0
        String initialPrice = "N/A";
        Integer initialChiTietSanPhamId = null; // ID of the default variant

        if (!mauSacList.isEmpty()) {
            defaultMauSacId = mauSacList.get(0).getMauSacId(); // Select the first active color as default
            model.addAttribute("defaultMauSacId", defaultMauSacId);

            // Fetch only ACTIVE sizes for the default active color
            List<KichCo> kichCoList = sanPhamService.getActiveKichCoBySanPhamAndMauSac(id, defaultMauSacId);

            // Populate DTO list ONLY with active variants for the default color
            for (KichCo item : kichCoList) {
                // Get the specific ACTIVE variant detail
                Optional<ChiTietSanPham> chiTietOpt = sanPhamService.getActiveChiTietSanPham(id, defaultMauSacId, item.getKichCoId());

                if (chiTietOpt.isPresent()) {
                    ChiTietSanPham chiTietSanPham = chiTietOpt.get();
                    int availableStock = sanPhamService.calculateAvailableStock(chiTietSanPham);
                    int physicalStock = Optional.ofNullable(chiTietSanPham.getSoLuongTon()).orElse(0);

                    listKichCoDTO.add(new DataDTO(
                            chiTietSanPham.getChiTietSanPhamId(),
                            item.getTenKichCo(), // Size Name
                            chiTietSanPham.getGiaBan(), // Price
                            availableStock,      // Calculated Available Stock
                            physicalStock        // Physical Stock
                    ));
                }
                // No 'else' needed, if variant isn't active or doesn't exist, it's skipped
            }

            // Get initial stock/price from the FIRST item in the DTO list (which represents the first active size for the default color)
            if (!listKichCoDTO.isEmpty()) {
                DataDTO firstVariant = listKichCoDTO.get(0);
                initialAvailableStock = firstVariant.getAvailableStock();
                initialPhysicalStock = firstVariant.getPhysicalStock();
                initialPrice = formatCurrencyHelper(firstVariant.getGiaBan()); // Format initial price
                initialChiTietSanPhamId = firstVariant.getId(); // Store initial variant ID
            }
        } else {
            // Handle case where product has NO active colors/variants at all
            model.addAttribute("noVariantsMessage", "Sản phẩm này hiện chưa có màu sắc hoặc kích cỡ nào.");
        }

        model.addAttribute("sanPham", sanPham);
        // model.addAttribute("mauSacList", mauSacList); // Already added
        model.addAttribute("kichCoListDTO", listKichCoDTO); // Pass the DTO list for the default color
        // model.addAttribute("defaultMauSacId", defaultMauSacId); // Already added
        model.addAttribute("initialAvailableStock", initialAvailableStock);
        model.addAttribute("initialPhysicalStock", initialPhysicalStock);
        model.addAttribute("initialPrice", initialPrice); // Pass formatted initial price
        model.addAttribute("initialChiTietSanPhamId", initialChiTietSanPhamId); // Pass initial variant ID


        return "customer/product-detail";
    }

    // Helper to format currency (can be moved to a utility class)
    private String formatCurrencyHelper(BigDecimal amount) {
        if (amount == null) return "N/A";
        java.text.NumberFormat currencyFormatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));
        return currencyFormatter.format(amount);
    }
}