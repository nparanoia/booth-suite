package com.booth.trade.controller.open;

import com.booth.trade.pay.AlipayGateway;
import com.booth.trade.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/open/pay/alipay")
@RequiredArgsConstructor
public class AlipayNotifyController {

    private final ObjectProvider<AlipayGateway> alipayGateway;
    private final PaymentService paymentService;

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String notify(HttpServletRequest request) {
        AlipayGateway gateway = alipayGateway.getIfAvailable();
        Map<String, String> params = flatten(request);
        if (gateway == null || !gateway.verify(params)) {
            return "failure";
        }
        return paymentService.handleSuccess(params.get("out_trade_no"), new BigDecimal(params.get("total_amount")),
                params.get("trade_status"), params.get("trade_no"), PaymentService.PAY_TYPE_ALIPAY);
    }

    private static Map<String, String> flatten(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
