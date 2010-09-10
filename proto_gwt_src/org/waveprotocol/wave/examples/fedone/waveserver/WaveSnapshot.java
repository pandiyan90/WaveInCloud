// Generated by http://code.google.com/p/protostuff/ ... DO NOT EDIT!
// Generated from waveclient-rpc.proto

package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.gwt.core.client.*;

public class WaveSnapshot extends JavaScriptObject  {


    public static class Builder extends WaveSnapshot {
      protected Builder() { }
      public final WaveSnapshot build() {
        return (WaveSnapshot)this;
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
     * Creates a new WaveSnapshot instance 
     *
     * @return new WaveSnapshot instance
     */
    public static native WaveSnapshot create() /*-{
        return {
          "_protoMessageName": "WaveSnapshot",              
        };
    }-*/;

    /**
     * Creates a new JsArray<WaveSnapshot> instance 
     *
     * @return new JsArray<WaveSnapshot> instance
     */
    public static native JsArray<WaveSnapshot> createArray() /*-{
        return [];
    }-*/;

    /**
     * Returns the name of this protocol buffer.
     */
    public static native String getProtocolBufferName(JavaScriptObject instance) /*-{
        return instance._protoMessageName;
    }-*/;

    /**
     * Gets a WaveSnapshot (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return WaveSnapshot
     */
    public static native WaveSnapshot get(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Gets a JsArray<WaveSnapshot> (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return JsArray<WaveSnapshot> 
     */
    public static native JsArray<WaveSnapshot> getArray(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Parses a WaveSnapshot from a json string
     *
     * @param json string to be parsed/evaluated
     * @return WaveSnapshot 
     */
    public static native WaveSnapshot parse(String json) /*-{
        return eval("(" + json + ")");
    }-*/;

    /**
     * Parses a JsArray<WaveSnapshot> from a json string
     *
     * @param json string to be parsed/evaluated
     * @return JsArray<WaveSnapshot> 
     */
    public static native JsArray<WaveSnapshot> parseArray(String json) /*-{
        return eval("(" + json + ")");
    }-*/;
    
    /**
     * Serializes a json object to a json string.
     *
     * @param WaveSnapshot the object to serialize
     * @return String the serialized json string
     */
    public static native String stringify(WaveSnapshot obj) /*-{
        var buf = [];
        var _1 = obj["1"];
        if(_1 != null && _1.length != 0) {
            var b = [], fn = @org.waveprotocol.wave.examples.fedone.waveserver.WaveletSnapshotAndVersion::stringify(Lorg/waveprotocol/wave/examples/fedone/waveserver/WaveletSnapshotAndVersion;);
            for(var i=0,l=_1.length; i<l; i++)
                b.push(fn(_1[i]));
            buf.push("\"1\":[" + b.join(",") + "]");
        }

        return buf.length == 0 ? "{}" : "{" + buf.join(",") + "}";
    }-*/;
    
    public static native boolean isInitialized(WaveSnapshot obj) /*-{
        return true;
    }-*/;

    protected WaveSnapshot() {}

    // getters and setters

    // wavelet

    public final native JsArray<WaveletSnapshotAndVersion> getWaveletArray() /*-{
        return this["1"];
    }-*/;

    public final java.util.List<WaveletSnapshotAndVersion> getWaveletList() {
        JsArray<WaveletSnapshotAndVersion> array = getWaveletArray();
        java.util.List<WaveletSnapshotAndVersion> list = new java.util.ArrayList<WaveletSnapshotAndVersion>();
        
        if (array == null) {
          return null; 
        }
        for (int i=0; i < getWaveletCount(); i++) {
          list.add(array.get(i));
        }
        return list;
    }

    public final native WaveSnapshot setWaveletArray(JsArray<WaveletSnapshotAndVersion> wavelet) /*-{
        this["1"] = wavelet;
        return this;
    }-*/;

    public final native JsArray<WaveletSnapshotAndVersion> clearWaveletArray() /*-{
        return (this["1"] = []);
    }-*/;

    public final WaveletSnapshotAndVersion getWavelet(int index) {
        JsArray<WaveletSnapshotAndVersion> array = getWaveletArray();
        return array == null ? null : array.get(index);
    }

    public final int getWaveletCount() {
        JsArray<WaveletSnapshotAndVersion> array = getWaveletArray();
        return array == null ? 0 : array.length();
    }

    public final void addWavelet(WaveletSnapshotAndVersion wavelet) {
        JsArray<WaveletSnapshotAndVersion> array = getWaveletArray();
        if(array == null)
            array = clearWaveletArray();
        array.push(wavelet);
    }


}