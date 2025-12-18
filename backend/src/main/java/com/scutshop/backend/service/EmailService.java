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
        msg.setTo(to);
        msg.setSubject("Order Paid: " + order.getOrderNo());
        msg.setText("Your order " + order.getOrderNo() + " has been paid. Total: " + order.getTotalAmount());
        mailSender.send(msg);
    }
}
