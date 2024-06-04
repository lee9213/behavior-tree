package com.lee9213.behavior.parser.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author lee9213@163.com
 * @date 2024/6/3 17:48
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({SpringNodeUtil.class})
public @interface EnableBehavior {

}
