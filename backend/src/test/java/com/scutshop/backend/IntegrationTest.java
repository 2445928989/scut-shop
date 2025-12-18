package com.scutshop.backend;

import com.scutshop.backend.dto.AuthRequest;
import com.scutshop.backend.dto.CheckoutRequest;
import com.scutshop.backend.dto.AddCartItemRequest;
import com.scutshop.backend.mapper.ProductMapper;
import com.scutshop.backend.model.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import javax.sql.DataSource;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;

import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
public class IntegrationTest {

    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0").withDatabaseName("testdb")
            .withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        mysql.start();
        r.add("spring.datasource.url", () -> mysql.getJdbcUrl());
        r.add("spring.datasource.username", () -> mysql.getUsername());
        r.add("spring.datasource.password", () -> mysql.getPassword());
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ProductMapper productMapper;

    private RestTemplate rest = new RestTemplate();

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void init() throws Exception {
        // execute schema to create tables
        ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("db/schema.sql"));

        // insert a test product
        Product p = new Product();
        p.setName("Integration Product");
        p.setPrice(new java.math.BigDecimal("9.99"));
        p.setStock(100);
        p.setStatus(1);
        productMapper.insert(p);
    }

    @Test
    void fullCheckoutFlow() {
        String base = "http://localhost:" + port;

        // register
        java.util.Map<String, String> reg = java.util.Map.of(
                "username", "ituser",
                "password", "password",
                "email", "ituser@example.com");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<java.util.Map<String, String>> regReq = new HttpEntity<>(reg, h);
        ResponseEntity<String> regResp = rest.postForEntity(base + "/api/auth/register", regReq, String.class);
        Assertions.assertTrue(regResp.getStatusCode().is2xxSuccessful());

        // login
        AuthRequest login = new AuthRequest();
        login.setUsername("ituser");
        login.setPassword("password");
        HttpEntity<AuthRequest> loginReq = new HttpEntity<>(login, h);
        ResponseEntity<java.util.Map> loginResp = rest.postForEntity(base + "/api/auth/login", loginReq,
                java.util.Map.class);
        Assertions.assertEquals(200, loginResp.getStatusCodeValue());
        String token = (String) loginResp.getBody().get("accessToken");
        Assertions.assertNotNull(token);

        // add to cart
        AddCartItemRequest add = new AddCartItemRequest();
        add.setProductId(1L);
        add.setQuantity(2);
        HttpHeaders auth = new HttpHeaders();
        auth.setContentType(MediaType.APPLICATION_JSON);
        auth.setBearerAuth(token);
        HttpEntity<AddCartItemRequest> addReq = new HttpEntity<>(add, auth);
        ResponseEntity<java.util.Map> addResp = rest.postForEntity(base + "/api/cart/items", addReq,
                java.util.Map.class);
        Assertions.assertEquals(200, addResp.getStatusCodeValue());
        Long cartId = Long.valueOf(addResp.getBody().get("cartId").toString());
        Assertions.assertNotNull(cartId);

        // checkout
        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setAddressId(null);
        checkout.setPaymentMethod("mock");
        HttpEntity<CheckoutRequest> coReq = new HttpEntity<>(checkout, auth);
        ResponseEntity<java.util.Map> coResp = rest.postForEntity(base + "/api/orders/checkout", coReq,
                java.util.Map.class);
        Assertions.assertEquals(200, coResp.getStatusCodeValue());
        Assertions.assertTrue(coResp.getBody().containsKey("orderId"));

        // verify email was sent
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
    }
}
