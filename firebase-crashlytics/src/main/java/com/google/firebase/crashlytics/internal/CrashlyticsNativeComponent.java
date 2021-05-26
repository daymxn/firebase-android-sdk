// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.crashlytics.internal;

import androidx.annotation.NonNull;
import com.google.firebase.crashlytics.internal.model.StaticSessionData;

public interface CrashlyticsNativeComponent {

  boolean hasCrashDataForSession(@NonNull String sessionId);

  // TODO: Consider what to do with the rest of the lifecycle if openSession fails.
  void openSession(
      @NonNull String sessionId,
      @NonNull String generator,
      long startedAtSeconds,
      @NonNull StaticSessionData sessionData);

  // TODO: Consider whether these methods should return boolean or throw exceptions.
  void finalizeSession(@NonNull String sessionId);

  @NonNull
  NativeSessionFileProvider getSessionFileProvider(@NonNull String sessionId);
}
