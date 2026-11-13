package com.booth.trade.error;

/** 限流不是普通业务失败，要返回 HTTP 429。 */
public class SeckillBusyException extends RuntimeException {
}
