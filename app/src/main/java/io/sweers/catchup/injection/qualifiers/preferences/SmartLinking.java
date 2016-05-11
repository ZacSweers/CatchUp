package io.sweers.catchup.injection.qualifiers.preferences;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface SmartLinking {
}
