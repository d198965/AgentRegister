package com.stk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by zdh on 17/4/26.
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AgentKey {
    public String[] Keys();
}
