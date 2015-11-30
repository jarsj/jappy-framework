package com.crispy.server;

/**
 * Created by harsh on 11/18/15.
 */
public @interface PostMethod {
    String path() default "";
}
