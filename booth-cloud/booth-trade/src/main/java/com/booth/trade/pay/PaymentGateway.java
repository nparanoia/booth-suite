package com.booth.trade.pay;

import com.booth.trade.entity.TradeOrder;

public interface PaymentGateway {

    String createPay(TradeOrder order);
}
