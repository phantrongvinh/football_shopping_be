package com.datn.project.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.datn.project.entity.Order;
import com.datn.project.entity.OrderStatus;
import com.datn.project.repository.IOrderRepository;

@Service
public class GHNService {

    @Value("${ghn.token}")
    private String token;
    @Value("${ghn.shop-id}")
    private String shopId;
    @Value("${ghn.url}")
    private String url;

    @Autowired
    private IOrderRepository orderRepository;

    private RestTemplate restTemplate = new RestTemplate();

    public void createShipment(Integer orderId) {
        try {
            System.out.println(token);
            System.out.println(shopId);
            System.out.println(orderId);

            Order order = orderRepository.findById(orderId).orElseThrow();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.set("ShopId", shopId);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("to_name", order.getReceiverName());
            body.put("to_phone", order.getReceiverPhone());
            body.put("to_address", order.getShippingAddress());
            body.put("to_ward_name", "Phường Bến Nghé"); // test
            body.put("to_district_name", "Quận 1"); // test
            body.put("to_province_name", "HCM"); // test
            body.put("weight", 500); // gram
            body.put("length", 20);
            body.put("width", 15);
            body.put("height", 10);
            body.put("service_type_id", 2); // express
            body.put("payment_type_id", 1); // người gửi trả phí
            body.put("required_note", "KHONGCHOXEMHANG");

            // shop
            body.put("from_name", "DATN Shop");
            body.put("from_phone", "0909090909");
            body.put("from_address", "123 Nguyen Hue");
            body.put("from_ward_name", "Phường Bến Nghé");
            body.put("from_district_name", "Quận 1");
            body.put("from_province_name", "TP Hồ Chí Minh");

            // items
            body.put("items", order.getOrderDetails().stream().map(d -> Map.of(
                    "name", d.getProductName(),
                    "quantity", d.getQuantity(),
                    "price", d.getPrice().intValue())).toList());

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> res = restTemplate.postForEntity(
                    url + "/shiip/public-api/v2/shipping-order/create", req, Map.class);
            System.out.println("=== GHN status: " + res.getStatusCode());
            System.out.println("=== GHN body: " + res.getBody());
            // lưu mã vận đơn vào order
            if (res.getStatusCode().is2xxSuccessful()) {
                String trackingCode = (String) ((Map) res.getBody().get("data")).get("order_code");
                order.setTrackingCode(trackingCode);
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }
        } catch (HttpClientErrorException e) {
            System.out.println("=== GHN error status: " + e.getStatusCode());
            System.out.println("=== GHN error body: " + e.getResponseBodyAsString());
        }

    }
}
