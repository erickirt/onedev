package io.onedev.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Image {
	String accept() default "image/*";
	
	int width() default 128;
	
	int height() default 128;
	
	String backgroundColor() default "white";
	
}
