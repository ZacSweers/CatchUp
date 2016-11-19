package io.sweers.catchup.data;

import com.squareup.moshi.JsonAdapter;

import org.threeten.bp.Instant;

import javax.inject.Qualifier;

import dagger.Module;
import dagger.Provides;
import dagger.Reusable;
import dagger.multibindings.IntoSet;

@Module(includes = {
        BaseOneFactoryModule.class, BaseTwoFactoryModule.class
})
public abstract class InstantModule {

    @Qualifier
    private @interface Private {}

    @Provides
    @Reusable
    @Private
    static JsonAdapter<Instant> provideInstantAdapter() {
        return new EpochInstantJsonAdapter(true);
    }

    @Provides
    @Reusable
    @IntoSet
    static JsonAdapter.Factory provideFactory(
            @Private final JsonAdapter<Instant> adapter) {
        return (type, annotations, moshi) -> annotations.isEmpty() && Instant.class.equals(type)
                ? adapter
                : null;
    }

}
