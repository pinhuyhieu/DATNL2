package com.sd31.sunday.controller;//package com.sd31.sunday.controller;
//import com.sd31.sunday.service.EmailService;
//import jakarta.mail.MessagingException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//@Controller
//public class EmailController {
//
//    @Autowired
//    private EmailService emailService;
//
//    @GetMapping("/test-mail")
//    public String showTestMailPage() {
//        return "test-mail"; // Thymeleaf HTML
//    }
//
//    @PostMapping("/send-mail")
//    public String sendTestMail(@RequestParam("to") String to, Model model) {
//        String subject = "Mừng đại lễ giải phóng miền Nam, gửi tặng bạn Voucher lên đến 50%";
//
//        String htmlContent = """
//        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px; border-radius: 10px;">
//            <h2 style="color: #d32f2f; text-align: center;">🎉 Mừng Đại Lễ 30/4 - Giải Phóng Miền Nam 🎉</h2>
//            <p style="font-size: 16px;">
//                Nhân dịp đại lễ, <strong>Sunday Shoes</strong> hân hạnh mang đến cho bạn chương trình <span style="color: green;"><strong>Voucher giảm giá lên đến 50%</strong></span> để tri ân khách hàng đã đồng hành cùng chúng tôi suốt thời gian qua.
//            </p>
//            <div style="background-color: #f5f5f5; padding: 15px; margin: 20px 0; text-align: center; border-radius: 5px;">
//                <p style="margin: 0; font-size: 18px;">🎁 Mã khuyến mãi của bạn là:</p>
//                <h3 style="color: #ff5722; margin: 10px 0;">3041975SUNDAY</h3>
//                <p style="margin: 0;">Sử dụng mã khi thanh toán để nhận ưu đãi đặc biệt.</p>
//            </div>
//            <div style="text-align: center; margin-top: 30px;">
//                <a href="http://localhost:8080/home" style="background-color: #1976d2; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">
//                    🛍️ Mua ngay
//                </a>
//            </div>
//            <p style="margin-top: 30px; font-size: 14px; color: #888; text-align: center;">
//                Sunday Shoes xin chúc bạn và gia đình một kỳ nghỉ lễ vui vẻ, hạnh phúc và nhiều niềm vui!
//            </p>
//        </div>
//    """;
//
//        try {
//            emailService.sendHtmlEmail(to, subject, htmlContent);
//            model.addAttribute("message", "✅ Gửi mail khuyến mãi thành công!");
//        } catch (MessagingException e) {
//            model.addAttribute("message", "❌ Lỗi gửi mail: " + e.getMessage());
//        }
//
//        return "test-mail";
//    }
//
//}
