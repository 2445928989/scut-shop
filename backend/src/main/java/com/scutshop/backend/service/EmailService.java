package com.scutshop.backend.service;

import com.scutshop.backend.model.Order;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPaymentConfirmation(String to, Order order) {
        SimpleMailMessage msg = new SimpleMailMessage();
        String from = java.util.Optional.ofNullable(System.getenv("MAIL_FROM")).orElse("no-reply@example.com");
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Order Paid: " + order.getOrderNo());
        msg.setText("Your order " + order.getOrderNo() + " has been paid. Total: " + order.getTotalAmount());
        mailSender.send(msg);
    }

    public void sendActivationEmail(String to, String activationLink) {
        SimpleMailMessage msg = new SimpleMailMessage();
        String from = java.util.Optional.ofNullable(System.getenv("MAIL_FROM")).orElse("no-reply@example.com");
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Activate your account");
        msg.setText("Please activate your account by visiting the following link: " + activationLink);
        mailSender.send(msg);
    }
}
