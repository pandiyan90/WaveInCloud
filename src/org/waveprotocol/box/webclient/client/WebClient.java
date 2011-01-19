/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.box.webclient.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

import org.waveprotocol.box.webclient.client.events.NetworkStatusEvent;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEvent.ConnectionStatus;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEventHandler;
import org.waveprotocol.box.webclient.client.events.WaveCreationEvent;
import org.waveprotocol.box.webclient.client.events.WaveCreationEventHandler;
import org.waveprotocol.box.webclient.client.events.WaveIndexUpdatedEvent;
import org.waveprotocol.box.webclient.client.events.WaveSelectionEvent;
import org.waveprotocol.box.webclient.client.events.WaveSelectionEventHandler;
import org.waveprotocol.box.webclient.client.events.WaveUpdatedEvent;
import org.waveprotocol.box.webclient.client.wavelist.WaveListPanel;
import org.waveprotocol.box.webclient.util.Log;
import org.waveprotocol.box.webclient.waveclient.common.ClientIdGenerator;
import org.waveprotocol.box.webclient.waveclient.common.WaveViewServiceImpl;
import org.waveprotocol.box.webclient.waveclient.common.WebClientBackend;
import org.waveprotocol.box.webclient.waveclient.common.WebClientUtils;
import org.waveprotocol.box.webclient.widget.error.ErrorIndicatorPresenter;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.common.util.AsyncHolder.Accessor;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.util.ClientFlags;
import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWavelet;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebClient implements EntryPoint {
  interface Binder extends UiBinder<DockLayoutPanel, WebClient> {
  }

  interface Style extends CssResource {
    String subPanel();
    String waveView();
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField
  SplitLayoutPanel splitPanel;

  @UiField
  Style style;

  @UiField
  ImplPanel contentPanel;

  @UiField
  WaveListPanel listPanel;

  @UiField
  DebugMessagePanel logPanel;

  static Log LOG = Log.get(WebClient.class);

  private WebClientBackend backend = null;

  /** The wave panel, if a wave is open. */
  private StagesProvider wave;

  /** The old wave panel */
  private WaveView waveView = null;

  /**
   * Create a remote websocket to talk to the server-side FedOne service.
   */
  private WaveWebSocketClient websocket;

  private ParticipantId loggedInUser;

  private IdGenerator idGenerator;

  private RemoteViewServiceMultiplexer channel;

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {

    ErrorHandler.install();

    // Set up UI
    DockLayoutPanel self = BINDER.createAndBindUi(this);
    RootPanel.get("app").add(self);
    // DockLayoutPanel forcibly conflicts with sensible layout control, and
    // sticks inline styles on elements without permission. They must be
    // cleared.
    self.getElement().getStyle().clearPosition();
    splitPanel.setWidgetMinSize(listPanel, 200);

    if (LogLevel.showDebug()) {
      logPanel.enable();
    } else {
      logPanel.removeFromParent();
    }

    if (ClientFlags.get().enableWavePanelHarness()) {
      // For handling the opening of wave using the new wave panel
      ClientEvents.get().addWaveSelectionEventHandler(
          new WaveSelectionEventHandler() {
            @Override
            public void onSelection(WaveRef waveRef) {
              openWave(waveRef, false);
            }
          });
      waveView = null;
    } else {
      waveView = new WaveView();
      contentPanel.add(waveView);

      // DockLayoutPanel manually set position relative. We need to clear it.
      waveView.getElement().getStyle().clearPosition();
    }

    ClientEvents.get().addWaveCreationEventHandler(
        new WaveCreationEventHandler() {

          @Override
          public void onCreateRequest(WaveCreationEvent event) {
            LOG.info("WaveCreationEvent received");
            if (channel == null) {
              throw new RuntimeException("Spaghetti attack.  Create occured before login");
            }

            if (ClientFlags.get().enableWavePanelHarness()) {
              WaveId newWaveId = idGenerator.newWaveId();
              openWave(WaveRef.of(newWaveId), true);
            } else {
              WaveId newWaveId = idGenerator.newWaveId();
              ClientEvents.get().fireEvent(new WaveSelectionEvent(WaveRef.of(newWaveId)));
              ObservableConversation convo = waveView.getConversationView().createRoot();
              CcBasedWavelet rootWavelet = waveView.getCcStackManager().view.getRoot();
              rootWavelet.addParticipant(loggedInUser);
              LOG.info("created conversation: " + convo);
              convo.getRootThread().appendBlip();
            }
          }
        });

    configureConnectionIndicator();

    HistorySupport.init();

    websocket = new WaveWebSocketClient(useSocketIO(), getWebSocketBaseUrl(GWT.getModuleBaseURL()));
    websocket.connect();

    if (Session.get().isLoggedIn()) {
      loggedInUser = new ParticipantId(Session.get().getAddress());
      idGenerator = ClientIdGenerator.create();
      loginToServer();
    }
    History.fireCurrentHistoryState();
    LOG.info("SimpleWebClient.onModuleLoad() done");
  }

  private void configureConnectionIndicator() {
    ClientEvents.get().addNetworkStatusEventHandler(new NetworkStatusEventHandler() {
      @Override
      public void onNetworkStatus(NetworkStatusEvent event) {
        Element element = Document.get().getElementById("netstatus");
        if (element != null) {
          switch (event.getStatus()) {
            case CONNECTED:
            case RECONNECTED:
              element.setInnerText("Online");
              element.setClassName("online");
              break;
            case DISCONNECTED:
              element.setInnerText("Offline");
              element.setClassName("offline");
              break;
            case RECONNECTING:
              element.setInnerText("Connecting...");
              element.setClassName("connecting");
              break;
          }
        }
      }
    });
  }

  /**
   * Returns <code>ws://yourhost[:port]/</code>.
   */
  // XXX check formatting wrt GPE
  private native String getWebSocketBaseUrl(String moduleBase) /*-{return "ws" + /:\/\/[^\/]+/.exec(moduleBase)[0] + "/";}-*/;

  private native boolean useSocketIO() /*-{ return !!$wnd.__useSocketIO }-*/;

  /**
   */
  private void loginToServer() {
    assert loggedInUser != null;
    backend = new WebClientBackend(loggedInUser, websocket);
    channel = new RemoteViewServiceMultiplexer(websocket, loggedInUser.getAddress());

    websocket.attachLegacy(backend);
    if (!ClientFlags.get().enableWavePanelHarness()) {
      waveView.setLegacy(backend, idGenerator);
    }

    ClientEvents.get().addNetworkStatusEventHandler(new NetworkStatusEventHandler() {
      @Override
      public void onNetworkStatus(NetworkStatusEvent event) {
        if (event.getStatus() == ConnectionStatus.CONNECTED) {
          openIndexWave();
        }
      }
    });
  }

  private void openIndexWave() {
    SimpleCcDocumentFactory docFactory = new SimpleCcDocumentFactory();
    final WaveViewServiceImpl indexWave = (WaveViewServiceImpl) backend.getIndexWave(docFactory);
    indexWave.viewOpen(IdFilters.ALL_IDS, null,
        new WaveViewService.OpenCallback() {

          @Override
          public void onException(ChannelException e) {
            LOG.severe("ChannelException opening index wave", e);
          }

          @Override
          public void onFailure(String reason) {
            LOG.info("Failure for index wave " + reason);
          }

          @Override
          public void onSuccess(String response) {
            LOG.info("Success for index wave subscription");
          }

          @Override
          public void onUpdate(WaveViewService.WaveViewServiceUpdate update) {
            LOG.info("IndexWave update received hasDeltas="
                + update.hasDeltas() + "  hasWaveletSnapshot="
                + update.hasWaveletSnapshot());
            ClientEvents.get().fireEvent(
                new WaveUpdatedEvent(indexWave, update.getChannelId(),
                    update.getWaveletId()));
            ClientEvents.get().fireEvent(
                new WaveIndexUpdatedEvent(
                    WebClientUtils.getIndexEntries(indexWave)));
          }
        });
  }

  /**
   * Shows a wave in a wave panel.
   *
   * @param waveRef wave id to open
   * @param isNewWave whether the wave is being created by this client session.
   */
  private void openWave(WaveRef waveRef, boolean isNewWave) {
    LOG.info("WebClient.openWave()");

    if (wave != null) {
      wave.destroy();
      wave = null;
    }

    wave = new StagesProvider(contentPanel.getElement().appendChild(Document.get().createDivElement()),
        contentPanel, waveRef, channel, idGenerator, isNewWave);
    wave.load(null);
    String encodedToken = History.getToken();
    if (encodedToken != null && !encodedToken.isEmpty()) {
      WaveRef fromWaveRef = HistorySupport.waveRefFromHistoryToken(encodedToken);
      if (waveRef == null) {
        LOG.info("History token contains invalid path: " + encodedToken);
        return;
      }
      if (fromWaveRef.getWaveId().equals(waveRef.getWaveId())) {
        // History change was caused by clicking on a link, it's already
        // updated by browser.
        return;
      }
    }
    History.newItem(HistorySupport.historyTokenFromWaveref(waveRef), false);
  }

  /**
   * An exception handler that reports exceptions using a <em>shiny banner</em>
   * (an alert placed on the top of the screen). Once the stack trace is
   * prepared, it is revealed in the banner via a link.
   */
  static class ErrorHandler implements UncaughtExceptionHandler {
    /** Next handler in the handler chain. */
    private final UncaughtExceptionHandler next;

    /**
     * Indicates whether an error has already been reported (at most one error
     * is ever reported by this handler).
     */
    private boolean hasFired;

    private ErrorHandler(UncaughtExceptionHandler next) {
      this.next = next;
    }

    public static void install() {
      GWT.setUncaughtExceptionHandler(new ErrorHandler(GWT.getUncaughtExceptionHandler()));
    }

    @Override
    public void onUncaughtException(Throwable e) {
      if (!hasFired) {
        hasFired = true;
        final ErrorIndicatorPresenter error =
            ErrorIndicatorPresenter.create(RootPanel.get("banner"));
        getStackTraceAsync(e, new Accessor<SafeHtml>() {
          @Override
          public void use(SafeHtml stack) {
            error.addDetail(stack, null);
          }
        });
      }

      if (next != null) {
        next.onUncaughtException(e);
      }
    }

    private void getStackTraceAsync(final Throwable t, final Accessor<SafeHtml> whenReady) {
      // TODO: Request stack-trace de-obfuscation. For now, just use the
      // javascript stack trace.
      //
      // Use minimal services here, in order to avoid the chance that reporting
      // the error produces more errors. In particular, do not use Scheduler.
      // Also, this code could potentially be put behind a runAsync boundary, to
      // save whatever dependencies it uses from the initial download.
      DeferredCommand.addCommand(new Command() {
        @Override
        public void execute() {
          SafeHtmlBuilder stack = new SafeHtmlBuilder();

          Throwable error = t;
          while (error != null) {
            stack.appendEscaped(String.valueOf(error.getMessage())).appendHtmlConstant("<br>");
            for (StackTraceElement elt : error.getStackTrace()) {
              stack.appendHtmlConstant("  ")
                  .appendEscaped(maybe(elt.getClassName(), "??")).appendHtmlConstant(".") //
                  .appendEscaped(maybe(elt.getMethodName(), "??")).appendHtmlConstant(" (") //
                  .appendEscaped(maybe(elt.getFileName(), "??")).appendHtmlConstant(":") //
                  .appendEscaped(maybe(elt.getLineNumber(), "??")).appendHtmlConstant(")") //
                  .appendHtmlConstant("<br>");
            }
            error = error.getCause();
            if (error != null) {
              stack.appendHtmlConstant("Caused by: ");
            }
          }

          whenReady.use(stack.toSafeHtml());
        }
      });
    }

    private static String maybe(String value, String otherwise) {
      return value != null ? value : otherwise;
    }

    private static String maybe(int value, String otherwise) {
      return value != -1 ? String.valueOf(value) : otherwise;
    }
  }
}
