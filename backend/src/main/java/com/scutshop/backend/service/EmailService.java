package com.scutshop.backend.service;

import com.scutshop.backend.model.Order;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        // QQ邮箱要求发件人 = 授权用户, 默认用 MAIL_USERNAME
        String customFrom = System.getenv("MAIL_FROM");
        if (customFrom != null && !customFrom.isBlank()) {
            this.fromAddress = customFrom;
        } else {
            this.fromAddress = java.util.Optional.ofNullable(System.getenv("MAIL_USERNAME")).orElse("noreply@shop.local");
        }
    }

    public void sendPaymentConfirmation(String to, Order order) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Order Paid: " + order.getOrderNo());
        msg.setText("Your order " + order.getOrderNo() + " has been paid. Total: " + order.getTotalAmount());
        mailSender.send(msg);
    }

    public void sendActivationEmail(String to, String activationLink) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Activate your account");
        msg.setText("Please activate your account by visiting the following link: " + activationLink);
        mailSender.send(msg);
    }
}
