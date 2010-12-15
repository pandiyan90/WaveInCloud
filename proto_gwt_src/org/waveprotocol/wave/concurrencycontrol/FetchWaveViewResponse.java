// Generated by http://code.google.com/p/protostuff/ ... DO NOT EDIT!
// Generated from clientserver.proto

package org.waveprotocol.wave.concurrencycontrol;

import com.google.gwt.core.client.*;

public class FetchWaveViewResponse extends JavaScriptObject  {

    public static class Wavelet extends JavaScriptObject  {


        public static class Builder extends Wavelet {
          protected Builder() { }
          public final Wavelet build() {
            return (Wavelet)this;
          }
          public static native Builder create() /*-{
            return {
            };
          }-*/;
        }

        public static final Builder newBuilder() {
          return Builder.create();
        }
        
        /**
         * Creates a new Wavelet instance 
         *
         * @return new Wavelet instance
         */
        public static native Wavelet create() /*-{
            return {
              "_protoMessageName": "Wavelet",              
            };
        }-*/;

        /**
         * Creates a new JsArray<Wavelet> instance 
         *
         * @return new JsArray<Wavelet> instance
         */
        public static native JsArray<Wavelet> createArray() /*-{
            return [];
        }-*/;

        /**
         * Returns the name of this protocol buffer.
         */
        public static native String getProtocolBufferName(JavaScriptObject instance) /*-{
            return instance._protoMessageName;
        }-*/;

        /**
         * Gets a Wavelet (casting) from a JavaScriptObject
         *
         * @param JavaScriptObject to cast
         * @return Wavelet
         */
        public static native Wavelet get(JavaScriptObject jso) /*-{
            return jso;
        }-*/;

        /**
         * Gets a JsArray<Wavelet> (casting) from a JavaScriptObject
         *
         * @param JavaScriptObject to cast
         * @return JsArray<Wavelet> 
         */
        public static native JsArray<Wavelet> getArray(JavaScriptObject jso) /*-{
            return jso;
        }-*/;

        /**
         * Parses a Wavelet from a json string
         *
         * @param json string to be parsed/evaluated
         * @return Wavelet 
         */
        public static native Wavelet parse(String json) /*-{
            return eval("(" + json + ")");
        }-*/;

        /**
         * Parses a JsArray<Wavelet> from a json string
         *
         * @param json string to be parsed/evaluated
         * @return JsArray<Wavelet> 
         */
        public static native JsArray<Wavelet> parseArray(String json) /*-{
            return eval("(" + json + ")");
        }-*/;
        
        /**
         * Serializes a json object to a json string.
         *
         * @param Wavelet the object to serialize
         * @return String the serialized json string
         */
        public static native String stringify(Wavelet obj) /*-{
            var buf = [];
            var _1 = obj["1"];
            if(_1 != null)
                buf.push("\"1\":\"" + _1 + "\"");
            var _2 = obj["2"];
            if(_2 != null)
                buf.push("\"2\":" + @org.waveprotocol.wave.concurrencycontrol.WaveletSnapshot::stringify(Lorg/waveprotocol/wave/concurrencycontrol/WaveletSnapshot;)(_2));

            return buf.length == 0 ? "{}" : "{" + buf.join(",") + "}";
        }-*/;
        
        public static native boolean isInitialized(Wavelet obj) /*-{
            return 
                obj["1"] != null;
        }-*/;

        protected Wavelet() {}

        // getters and setters

        // waveletId

        public final native String getWaveletId() /*-{
            return this["1"] || "";
        }-*/;

        public final native Wavelet setWaveletId(String waveletId) /*-{
            this["1"] = waveletId;
            return this;
        }-*/;

        public final native void clearWaveletId() /*-{
            delete this["1"];
        }-*/;

        public final native boolean hasWaveletId() /*-{
            return this["1"] != null;
        }-*/;

        // snapshot

        public final native WaveletSnapshot getSnapshot() /*-{
            return this["2"];
        }-*/;

        public final native Wavelet setSnapshot(WaveletSnapshot snapshot) /*-{
            this["2"] = snapshot;
            return this;
        }-*/;

        public final native void clearSnapshot() /*-{
            delete this["2"];
        }-*/;

        public final native boolean hasSnapshot() /*-{
            return this["2"] != null;
        }-*/;


    }

    public static class Builder extends FetchWaveViewResponse {
      protected Builder() { }
      public final FetchWaveViewResponse build() {
        return (FetchWaveViewResponse)this;
      }
      public static native Builder create() /*-{
        return {
        };
      }-*/;
    }

    public static final Builder newBuilder() {
      return Builder.create();
    }
    
    /**
     * Creates a new FetchWaveViewResponse instance 
     *
     * @return new FetchWaveViewResponse instance
     */
    public static native FetchWaveViewResponse create() /*-{
        return {
          "_protoMessageName": "FetchWaveViewResponse",              
        };
    }-*/;

    /**
     * Creates a new JsArray<FetchWaveViewResponse> instance 
     *
     * @return new JsArray<FetchWaveViewResponse> instance
     */
    public static native JsArray<FetchWaveViewResponse> createArray() /*-{
        return [];
    }-*/;

    /**
     * Returns the name of this protocol buffer.
     */
    public static native String getProtocolBufferName(JavaScriptObject instance) /*-{
        return instance._protoMessageName;
    }-*/;

    /**
     * Gets a FetchWaveViewResponse (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return FetchWaveViewResponse
     */
    public static native FetchWaveViewResponse get(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Gets a JsArray<FetchWaveViewResponse> (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return JsArray<FetchWaveViewResponse> 
     */
    public static native JsArray<FetchWaveViewResponse> getArray(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Parses a FetchWaveViewResponse from a json string
     *
     * @param json string to be parsed/evaluated
     * @return FetchWaveViewResponse 
     */
    public static native FetchWaveViewResponse parse(String json) /*-{
        return eval("(" + json + ")");
    }-*/;

    /**
     * Parses a JsArray<FetchWaveViewResponse> from a json string
     *
     * @param json string to be parsed/evaluated
     * @return JsArray<FetchWaveViewResponse> 
     */
    public static native JsArray<FetchWaveViewResponse> parseArray(String json) /*-{
        return eval("(" + json + ")");
    }-*/;
    
    /**
     * Serializes a json object to a json string.
     *
     * @param FetchWaveViewResponse the object to serialize
     * @return String the serialized json string
     */
    public static native String stringify(FetchWaveViewResponse obj) /*-{
        var buf = [];
        var _1 = obj["1"];
        if(_1 != null)
            buf.push("\"1\":" + @org.waveprotocol.wave.concurrencycontrol.ResponseStatus::stringify(Lorg/waveprotocol/wave/concurrencycontrol/ResponseStatus;)(_1));
        var _2 = obj["2"];
        if(_2 != null && _2.length != 0) {
            var b = [], fn = @org.waveprotocol.wave.concurrencycontrol.FetchWaveViewResponse.Wavelet::stringify(Lorg/waveprotocol/wave/concurrencycontrol/FetchWaveViewResponse$Wavelet;);
            for(var i=0,l=_2.length; i<l; i++)
                b.push(fn(_2[i]));
            buf.push("\"2\":[" + b.join(",") + "]");
        }

        return buf.length == 0 ? "{}" : "{" + buf.join(",") + "}";
    }-*/;
    
    public static native boolean isInitialized(FetchWaveViewResponse obj) /*-{
        return 
            obj["1"] != null;
    }-*/;

    protected FetchWaveViewResponse() {}

    // getters and setters

    // status

    public final native ResponseStatus getStatus() /*-{
        return this["1"];
    }-*/;

    public final native FetchWaveViewResponse setStatus(ResponseStatus status) /*-{
        this["1"] = status;
        return this;
    }-*/;

    public final native void clearStatus() /*-{
        delete this["1"];
    }-*/;

    public final native boolean hasStatus() /*-{
        return this["1"] != null;
    }-*/;

    // wavelet

    public final native JsArray<Wavelet> getWaveletArray() /*-{
        return this["2"];
    }-*/;

    public final java.util.List<Wavelet> getWaveletList() {
        JsArray<Wavelet> array = getWaveletArray();
        java.util.List<Wavelet> list = new java.util.ArrayList<Wavelet>();
        
        if (array == null) {
          return null; 
        }
        for (int i=0; i < getWaveletCount(); i++) {
          list.add(array.get(i));
        }
        return list;
    }

    public final native FetchWaveViewResponse setWaveletArray(JsArray<Wavelet> wavelet) /*-{
        this["2"] = wavelet;
        return this;
    }-*/;

    public final native JsArray<Wavelet> clearWaveletArray() /*-{
        return (this["2"] = []);
    }-*/;

    public final Wavelet getWavelet(int index) {
        JsArray<Wavelet> array = getWaveletArray();
        return array == null ? null : array.get(index);
    }

    public final int getWaveletCount() {
        JsArray<Wavelet> array = getWaveletArray();
        return array == null ? 0 : array.length();
    }

    public final void addWavelet(Wavelet wavelet) {
        JsArray<Wavelet> array = getWaveletArray();
        if(array == null)
            array = clearWaveletArray();
        array.push(wavelet);
    }


}