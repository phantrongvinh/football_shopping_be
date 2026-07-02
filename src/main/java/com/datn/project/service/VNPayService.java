package com.datn.project.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.datn.project.config.VNPayConfig;
import com.datn.project.entity.Order;
import com.datn.project.repository.IOrderRepository;

@Service
public class VNPayService {

    @Autowired
    private VNPayConfig config;

    @Autowired
    private IOrderRepository orderRepository;

    public String createPaymentUrl(Integer orderId, BigDecimal amount, String ipAddress) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
        params.put("vnp_CurrCode", "VND");
        String txnRef = orderId + "_" + System.currentTimeMillis();

        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setPaymentTxnRef(txnRef);
        orderRepository.save(order);

        // tạo chuỗi hash
        String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String secureHash = hmacSHA512(config.getHashSecret(), queryString);
        return config.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifyCallback(Map<String, String> params) throws Exception {
        String receivedHash = params.get("vnp_SecureHash");
        Map<String, String> filtered = new TreeMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        String queryString = filtered.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String expectedHash = hmacSHA512(config.getHashSecret(), queryString);
        return expectedHash.equals(receivedHash);
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return Hex.encodeHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean refund(Order order, String transactionDate) throws Exception {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", "").substring(0, 32));
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "refund");
            params.put("vnp_TmnCode", config.getTmnCode());
            params.put("vnp_TransactionType", "02"); // 02 = hoàn toàn bộ
            params.put(
                    "vnp_TxnRef",
                    order.getPaymentTxnRef());
            params.put("vnp_Amount", String.valueOf(
                    order.getFinalPrice().multiply(BigDecimal.valueOf(100)).longValue()));
            params.put("vnp_OrderInfo", "Hoan tien don hang " + order.getId());
            params.put("vnp_TransactionNo", order.getPaymentTransactionId());
            params.put("vnp_TransactionDate", transactionDate);
            params.put("vnp_CreateBy", "system");
            params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            params.put("vnp_IpAddr", "127.0.0.1");

            String hashData = String.join("|",
                    params.get("vnp_RequestId"),
                    params.get("vnp_Version"),
                    params.get("vnp_Command"),
                    params.get("vnp_TmnCode"),
                    params.get("vnp_TransactionType"),
                    params.get("vnp_TxnRef"),
                    params.get("vnp_Amount"),
                    params.get("vnp_TransactionNo"),
                    params.get("vnp_TransactionDate"),
                    params.get("vnp_CreateBy"),
                    params.get("vnp_CreateDate"),
                    params.get("vnp_IpAddr"),
                    params.get("vnp_OrderInfo"));

            params.put("vnp_SecureHash", hmacSHA512(config.getHashSecret(), hashData));

            // Gọi VNPay Refund API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> res = restTemplate.exchange(
                    "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction",
                    HttpMethod.POST,
                    entity,
                    Map.class);
            String responseCode = (String) res.getBody().get("vnp_ResponseCode");
            return "00".equals(responseCode);
        }

        catch (HttpStatusCodeException ex) {
            System.out.println(ex.getStatusCode());
            System.out.println(ex.getResponseBodyAsString());
            return false;
        }
    }
}
