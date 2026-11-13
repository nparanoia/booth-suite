package com.booth.trade.controller.open;

import com.booth.trade.dto.MockPayNotifyRequest;
import com.booth.trade.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open/pay/mock")
@ConditionalOnProperty(name = "booth.pay.mock-enabled", havingValue = "true")
@RequiredArgsConstructor
public class MockPayController {

    private final PaymentService paymentService;

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String notify(@Valid @RequestBody MockPayNotifyRequest request) {
        if (!paymentService.verifyMock(request.orderSn(), request.amount(), request.tradeStatus(),
                request.channelTradeNo(), request.timestamp(), request.sign())) {
            return "failure";
        }
        return paymentService.handleSuccess(request.orderSn(), request.amount(), request.tradeStatus(),
                request.channelTradeNo(), PaymentService.PAY_TYPE_ALIPAY);
    }
}
