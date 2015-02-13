/*
 * Copyright (C) 2015 Pedro Vicente Gomez Sanchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pedrovgs.lynx.presenter;

import com.github.pedrovgs.lynx.LynxConfig;
import com.github.pedrovgs.lynx.model.Lynx;
import com.github.pedrovgs.lynx.model.Trace;
import java.util.LinkedList;
import java.util.List;

/**
 * Presenter created to decouple Lynx library view implementations from Lynx model. This presenter
 * responsibility is related to all the presentation logic to Lynx UI implementations. Lynx UI
 * implementations have to implement LynxPresenter.View interface.
 *
 * @author Pedro Vicente Gómez Sánchez.
 */
public class LynxPresenter implements Lynx.Listener {

  private static final int MIN_VISIBLE_POSITION_TO_ENABLE_AUTO_SCROLL = 3;

  private final Lynx lynx;
  private final View view;
  private final TraceBuffer traceBuffer;
  private boolean isInitialized;

  public LynxPresenter(Lynx lynx, View view, int maxNumberOfTracesToShow) {
    validateNumberOfTracesConfiguration(maxNumberOfTracesToShow);
    this.lynx = lynx;
    this.view = view;
    this.traceBuffer = new TraceBuffer(maxNumberOfTracesToShow);
  }

  public void setLynxConfig(LynxConfig lynxConfig) {
    validateLynxConfig(lynxConfig);
    updateBufferConfig(lynxConfig);
    updateLynxConfig(lynxConfig);
  }

  public void resume() {
    if (!isInitialized) {
      isInitialized = true;
      lynx.registerListener(this);
      lynx.startReading();
    }
  }

  public void pause() {
    if (isInitialized) {
      isInitialized = false;
      lynx.stopReading();
      lynx.unregisterListener(this);
    }
  }

  @Override public void onNewTraces(List<Trace> traces) {
    int tracesRemoved = updateTraceBuffer(traces);
    List<Trace> tracesToNotify = getCurrentTraces();
    view.showTraces(tracesToNotify, tracesRemoved);
  }

  public void onScrollToPosition(int lastVisiblePosition) {
    if (shouldDisableAutoScroll(lastVisiblePosition)) {
      view.disableAutoScroll();
    } else {
      view.enableAutoScroll();
    }
  }

  public void onFilterUpdated(String filter) {
    if (isInitialized) {
      LynxConfig lynxConfig = lynx.getConfig();
      lynxConfig.withFilter(filter);
      lynx.setConfig(lynxConfig);
      clearView();
      restartLynx();
    }
  }

  public void onShareButtonClicked() {
    List<Trace> tracesToShare = new LinkedList<Trace>(traceBuffer.getTraces());
    String plainTraces = generatePlainTracesToShare(tracesToShare);
    view.shareTraces(plainTraces);
  }

  private void clearView() {
    traceBuffer.clear();
    view.clear();
  }

  private void restartLynx() {
    lynx.restart();
  }

  private void updateBufferConfig(LynxConfig lynxConfig) {
    traceBuffer.setBufferSize(lynxConfig.getMaxNumberOfTracesToShow());
    refreshTraces();
  }

  private void refreshTraces() {
    onNewTraces(traceBuffer.getTraces());
  }

  private void updateLynxConfig(LynxConfig lynxConfig) {
    lynx.setConfig(lynxConfig);
  }

  private int updateTraceBuffer(List<Trace> traces) {
    return traceBuffer.add(traces);
  }

  public List<Trace> getCurrentTraces() {
    return traceBuffer.getTraces();
  }

  private void validateNumberOfTracesConfiguration(long maxNumberOfTracesToShow) {
    if (maxNumberOfTracesToShow <= 0) {
      throw new IllegalArgumentException(
          "You can't pass a zero or negative number of traces to show.");
    }
  }

  private void validateLynxConfig(LynxConfig lynxConfig) {
    if (lynxConfig == null) {
      throw new IllegalArgumentException(
          "You can't use a null instance of LynxConfig as configuration.");
    }
  }

  private boolean shouldDisableAutoScroll(int lastVisiblePosition) {
    int positionOffset = traceBuffer.getCurrentNumberOfTraces() - lastVisiblePosition;
    return positionOffset >= MIN_VISIBLE_POSITION_TO_ENABLE_AUTO_SCROLL;
  }

  private String generatePlainTracesToShare(List<Trace> tracesToShare) {
    StringBuilder sb = new StringBuilder();
    for (Trace trace : tracesToShare) {
      String traceLevel = trace.getLevel().getValue();
      String traceMessage = trace.getMessage();
      sb.append(traceLevel);
      sb.append("/ ");
      sb.append(traceMessage);
      sb.append("\n");
    }
    return sb.toString();
  }

  public interface View {

    void showTraces(List<Trace> traces, int removedTraces);

    void disableAutoScroll();

    void enableAutoScroll();

    void clear();

    void shareTraces(String plainTraces);
  }
}
