package io.sweers.catchup.ui.activity;

import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.sweers.catchup.P;
import io.sweers.catchup.data.BaseFactoryModule;
import io.sweers.catchup.data.InstantModule;
import io.sweers.catchup.injection.qualifiers.preferences.SmartLinking;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import java.util.Set;
import javax.inject.Qualifier;
import rx.Observable;

@Module(
    includes = {UiModule.class, ActivityModule.InnerModule.class}
)
public class ActivityModule {

  private MainActivity activity;

  public ActivityModule(MainActivity activity) {
    this.activity = activity;
  }

  @PerActivity
  @Provides
  MainActivity provideActivity() {
    return activity;
  }

  @PerActivity
  @Provides
  CustomTabActivityHelper provideCustomTabActivityHelper() {
    return new CustomTabActivityHelper(activity);
  }

  @PerActivity
  @Provides
  @SmartLinking
  static Preference<Boolean> provideSmartLinkingPref(RxSharedPreferences rxSharedPreferences) {
    // TODO Use psync once it's fixed
    return rxSharedPreferences.getBoolean(P.smartlinkingGlobal.key, P.smartlinkingGlobal.defaultValue());
//    return P.smartlinkingGlobal.rx();
  }

  @PerActivity
  @Provides
  @Factories
  static Moshi provideMoshi(Moshi rootMoshi, @Factories Set<JsonAdapter.Factory> factories) {
    Moshi.Builder builder = rootMoshi.newBuilder();

    // Borrow from the parent's cache
    builder.add((type, annotations, moshi) -> rootMoshi.adapter(type, annotations));

    // Populate from the factories, use https://github.com/square/moshi/pull/216
    Observable.from(factories).subscribe(builder::add);

    return builder.build();
  }

  @Qualifier
  public @interface Factories {}

  @Module(includes = BaseFactoryModule.class)
  public abstract static class InnerModule {

    @Binds
    @Factories
    public abstract Set<JsonAdapter.Factory> factories(Set<JsonAdapter.Factory> factories);
  }

}
