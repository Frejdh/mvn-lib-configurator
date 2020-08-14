package com.frejdh.util.environment.annotation;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.lang.annotation.*;

/**
 * TODO: Start work
 * Enables support for the ${@link PropertyValue @PropertyValue} class so that it can be used for non-static members.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public @interface PropertyEnabled {

}
