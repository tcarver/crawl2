package org.genedb.crawl.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceDescription {
	String value();
	String type() default "";
}
