// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
//
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.perf.provider;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.tracing.Trace;

import com.google.android.gms.common.internal.Preconditions;
import com.google.android.gms.common.util.VisibleForTesting;
import com.google.firebase.perf.FirebasePerformanceInitializer;
import com.google.firebase.perf.application.AppStateMonitor;
import com.google.firebase.perf.config.ConfigResolver;
import com.google.firebase.perf.metrics.AppStartTrace;
import com.google.firebase.perf.session.SessionManager;
import com.google.firebase.perf.util.Clock;
import com.google.firebase.perf.util.Timer;

/** Initializes app start time at app startup time. */
@Keep
public class FirebasePerfProvider extends ContentProvider {

  private static final Timer APP_START_TIME = new Clock().getTime();
  /** Should match the {@link FirebasePerfProvider} authority if $androidId is empty. */
  @VisibleForTesting
  static final String EMPTY_APPLICATION_ID_PROVIDER_AUTHORITY =
      "com.google.firebase.firebaseperfprovider";

  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public static Timer getAppStartTime() {
    return APP_START_TIME;
  }

  @Override
  public void attachInfo(Context context, ProviderInfo info) {
    Trace.beginSection("Trace FirebasePerfProvider attachInfo()");

    // super.attachInfo calls onCreate(). Fail as early as possible.
    checkContentProviderAuthority(info);
    super.attachInfo(context, info);

    // Initialize ConfigResolver early for accessing device caching layer.
    androidx.tracing.Trace.beginSection("Trace FireperfInit ConfigResolver.getInstance()");
    ConfigResolver configResolver = ConfigResolver.getInstance();
    androidx.tracing.Trace.endSection();

    androidx.tracing.Trace.beginSection("Trace FireperfInit setContentProviderContext");
    configResolver.setContentProviderContext(getContext());
    androidx.tracing.Trace.endSection();

    androidx.tracing.Trace.beginSection("Trace FireperfInit AppStateMonitor.getInstance()");
    AppStateMonitor appStateMonitor = AppStateMonitor.getInstance();
    androidx.tracing.Trace.endSection();

    appStateMonitor.registerActivityLifecycleCallbacks(getContext());
    appStateMonitor.registerForAppColdStart(new FirebasePerformanceInitializer());

    androidx.tracing.Trace.beginSection("Trace FireperfInit AppStartTrace.getInstance()");
    AppStartTrace appStartTrace = AppStartTrace.getInstance();
    androidx.tracing.Trace.endSection();
    appStartTrace.registerActivityLifecycleCallbacks(getContext());

    mainHandler.post(new AppStartTrace.StartFromBackgroundRunnable(appStartTrace));

    // In the case of cold start, we create a session and start collecting gauges as early as
    // possible.
    // There is code in SessionManager that prevents us from resetting the session twice in case
    // of app cold start.
    Trace.beginSection("Trace SessionManager.instance");
    SessionManager.getInstance();

    Trace.endSection();


    Trace.beginSection("Trace SessionManager.initializeGaugeCollection()");
    SessionManager.getInstance().initializeGaugeCollection();
    Trace.endSection();

    Trace.endSection();
  }

  /** Called before {@link Application#onCreate()}. */
  @Override
  public boolean onCreate() {
    return false;
  }

  /**
   * Check that the content provider's authority does not use firebase-common's package name. If it
   * does, crash in order to alert the developer of the problem before they distribute the app.
   */
  private static void checkContentProviderAuthority(@NonNull ProviderInfo info) {
    Preconditions.checkNotNull(info, "FirebasePerfProvider ProviderInfo cannot be null.");
    if (EMPTY_APPLICATION_ID_PROVIDER_AUTHORITY.equals(info.authority)) {
      throw new IllegalStateException(
          "Incorrect provider authority in manifest. Most likely due to a missing "
              + "applicationId variable in application's build.gradle.");
    }
  }

  @Nullable
  @Override
  public Cursor query(
      Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return null;
  }

  @Nullable
  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Nullable
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }
}
