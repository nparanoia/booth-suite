package com.booth.trade.config;

import com.booth.common.core.result.Result;
import com.booth.trade.error.SeckillBusyException;
import com.booth.trade.error.TradeErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SeckillLimitAdvice {

    @ExceptionHandler(SeckillBusyException.class)
    public ResponseEntity<Result<Void>> busy() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Result.fail(TradeErrorCode.SECKILL_BUSY));
    }
}
