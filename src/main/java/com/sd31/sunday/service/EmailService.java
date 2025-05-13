package com.sd31.sunday.service;

import com.sd31.sunday.model.KhachHang;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendOrderConfirmationEmail(KhachHang khachHang, String fullName,String phone, String diaChi, String phuongThucThanhToan)
            throws MessagingException {

        // Tạo nội dung từ template
        Context context = new Context();
        context.setVariable("tenKhachHang", khachHang.getTen());
        context.setVariable("tenNguoiNhan", fullName);
        context.setVariable("soDienThoai", khachHang.getSoDienThoai());
        context.setVariable("soDienThoaiNguoiNhan", phone);
        context.setVariable("diaChi", diaChi);
        context.setVariable("phuongThucThanhToan", phuongThucThanhToan);

        String htmlContent = templateEngine.process("email/order-confirmation", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(khachHang.getEmail());
        helper.setSubject("🎉 Đơn hàng của bạn đã được đặt thành công!");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}


