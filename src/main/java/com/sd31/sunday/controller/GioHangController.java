package com.sd31.sunday.controller;

import com.sd31.sunday.DTO.CartItemDTO;
import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.GioHang;
import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.model.SanPham;
import com.sd31.sunday.repository.ChiTietSanPhamRepository;
import com.sd31.sunday.repository.GioHangRepository;
import com.sd31.sunday.repository.SanPhamRepository;
import com.sd31.sunday.service.SanPhamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class GioHangController {

    @Autowired
    private GioHangRepository gioHangRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private SanPhamService sanPhamService;

    // --- viewShoppingCart remains the same ---
    @GetMapping("/gio-hang")
    public String viewShoppingCart(Model model, HttpSession session) {
        KhachHang user = (KhachHang) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<GioHang> cartItems = gioHangRepository.findByTaiKhoanId(user.getKhachHangId());
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        boolean hasUnavailableItems = false;

        for (GioHang cartItem : cartItems) {
            Optional<ChiTietSanPham> chiTietSanPhamOpt = chiTietSanPhamRepository.findById(cartItem.getChiTietSanPham().getChiTietSanPhamId());

            CartItemDTO dto = new CartItemDTO();
            dto.setGioHangId(cartItem.getId());
            dto.setSoLuong(cartItem.getSoLuong());

            if (chiTietSanPhamOpt.isPresent()) {
                ChiTietSanPham chiTietSanPham = chiTietSanPhamOpt.get();
                Optional<SanPham> sanPhamOpt = sanPhamRepository.findById(chiTietSanPham.getSanPham().getSanPhamId());

                if (sanPhamOpt.isPresent()) {
                    SanPham sanPham = sanPhamOpt.get();
                    String imageUrl = sanPham.getHinhAnhs() != null && !sanPham.getHinhAnhs().isEmpty()
                            ? sanPham.getHinhAnhs().get(0).getHinhAnhUrl()
                            : "/images/default-product.png";

                    dto.setSanPhamId(sanPham.getSanPhamId());
                    dto.setChiTietSanPhamId(chiTietSanPham.getChiTietSanPhamId());
                    dto.setTenSanPham(sanPham.getTenSanPham());
                    dto.setMauSac(chiTietSanPham.getMauSac() != null ? chiTietSanPham.getMauSac().getTenMauSac() : "N/A");
                    dto.setKichCo(chiTietSanPham.getKichCo() != null ? chiTietSanPham.getKichCo().getTenKichCo() : "N/A");
                    dto.setGia(chiTietSanPham.getGiaBan() != null ? chiTietSanPham.getGiaBan() : BigDecimal.ZERO);
                    dto.setHinhAnhUrl(imageUrl);

                    int availableStock = sanPhamService.calculateAvailableStock(chiTietSanPham);
                    dto.setMaxAvailableQuantity(availableStock);

                    if (availableStock <= 0) {
                        dto.setAvailable(false);
                        int physicalStock = Optional.ofNullable(chiTietSanPham.getSoLuongTon()).orElse(0);
                        dto.setAvailabilityMessage(physicalStock <= 0 ? "Sản phẩm hiện đã hết hàng." : "Sản phẩm tạm hết hàng (đã được đặt trước).");
                        hasUnavailableItems = true;
                    } else if (cartItem.getSoLuong() > availableStock) {
                        dto.setAvailable(false);
                        dto.setAvailabilityMessage("Số lượng tối đa có thể mua là " + availableStock + "."); // Simpler message
                        hasUnavailableItems = true;
                    } else {
                        dto.setAvailable(true);
                        dto.setAvailabilityMessage("");
                        if (dto.getGia() != null && dto.getGia().compareTo(BigDecimal.ZERO) > 0) {
                            totalPrice = totalPrice.add(dto.getGia().multiply(new BigDecimal(cartItem.getSoLuong())));
                        }
                    }

                } else {
                    dto.setTenSanPham("Sản phẩm không tồn tại");
                    dto.setAvailable(false);
                    dto.setAvailabilityMessage("Sản phẩm này không còn trong cửa hàng.");
                    dto.setMaxAvailableQuantity(0);
                    dto.setGia(BigDecimal.ZERO);
                    dto.setHinhAnhUrl("/images/default-product.png");
                    hasUnavailableItems = true;
                }
            } else {
                dto.setTenSanPham("Phiên bản sản phẩm không tồn tại");
                dto.setAvailable(false);
                dto.setAvailabilityMessage("Phiên bản này (màu/cỡ) không còn tồn tại.");
                dto.setMaxAvailableQuantity(0);
                dto.setGia(BigDecimal.ZERO);
                dto.setHinhAnhUrl("/images/default-product.png");
                hasUnavailableItems = true;
            }
            cartItemDTOs.add(dto);
        }

        model.addAttribute("cartItems", cartItemDTOs);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("hasUnavailableItems", hasUnavailableItems);

        return "customer/gio-hang";
    }

    @PostMapping("/gio-hang/update")
    public String updateCartItem(@RequestParam("itemId") Integer itemId,
                                 @RequestParam("quantity") int quantity,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        KhachHang user = (KhachHang) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Optional<GioHang> optionalCartItem = gioHangRepository.findById(itemId);
        if (optionalCartItem.isPresent()) {
            GioHang cartItem = optionalCartItem.get();

            if (!cartItem.getTaiKhoanId().equals(user.getKhachHangId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thao tác không hợp lệ.");
                return "redirect:/gio-hang";
            }

            Optional<ChiTietSanPham> optionalChiTiet = chiTietSanPhamRepository.findById(cartItem.getChiTietSanPham().getChiTietSanPhamId());

            if (optionalChiTiet.isPresent()) {
                ChiTietSanPham chiTietSanPham = optionalChiTiet.get();
                int availableStock = sanPhamService.calculateAvailableStock(chiTietSanPham);

                if (quantity < 1) {
                    // Use a less alarming tone for minimum quantity
                    redirectAttributes.addFlashAttribute("infoMessage", "Số lượng tối thiểu là 1.");
                    // Don't update, just inform
                } else if (quantity > availableStock) {
                    String message;
                    if (availableStock > 0) {
                        // Gentle warning about stock limit
                        message = "Rất tiếc, số lượng khả dụng cho sản phẩm này chỉ còn " + availableStock + ".";
                    } else {
                        // Informative message when stock is zero (considering reservations)
                        int physicalStock = Optional.ofNullable(chiTietSanPham.getSoLuongTon()).orElse(0);
                        message = physicalStock <= 0 ? "Sản phẩm này hiện đã hết hàng." : "Sản phẩm này tạm hết hàng do đã được đặt trước.";
                    }
                    // Use info or warning level instead of error for stock limits
                    redirectAttributes.addFlashAttribute("warningMessage", message);
                    // Don't update quantity automatically here to avoid confusion, let user adjust.
                } else {
                    // Update successful
                    cartItem.setSoLuong(quantity);
                    gioHangRepository.save(cartItem);
                    redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số lượng.");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Rất tiếc, phiên bản sản phẩm này không còn tồn tại.");
                // Consider auto-removing
                // gioHangRepository.delete(cartItem);
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm trong giỏ hàng.");
        }

        return "redirect:/gio-hang";
    }

    @PostMapping("/gio-hang/remove/{itemId}")
    public String removeCartItem(@PathVariable("itemId") Integer itemId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        KhachHang user = (KhachHang) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Optional<GioHang> optionalCartItem = gioHangRepository.findById(itemId);
        if (optionalCartItem.isPresent()) {
            GioHang cartItem = optionalCartItem.get();
            if (cartItem.getTaiKhoanId().equals(user.getKhachHangId())) {
                gioHangRepository.delete(cartItem);
                // Keep success message concise
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Thao tác không hợp lệ.");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm.");
        }
        return "redirect:/gio-hang";
    }
}