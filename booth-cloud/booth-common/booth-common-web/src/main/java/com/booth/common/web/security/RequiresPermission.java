package com.booth.common.web.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在后台接口上，要求当前管理员拥有该权限码，例如 {@code @RequiresPermission("product:brand:edit")}。
 * 标在类上时对类中所有方法生效，方法上的注解优先。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    String value();
}
