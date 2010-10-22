// Generated by http://code.google.com/p/protostuff/ ... DO NOT EDIT!
// Generated from waveclient-rpc.proto

package org.waveprotocol.box.server.waveserver;

import com.google.gwt.core.client.*;

public class ProtocolAuthenticate extends JavaScriptObject  {


    public static class Builder extends ProtocolAuthenticate {
      protected Builder() { }
      public final ProtocolAuthenticate build() {
        return (ProtocolAuthenticate)this;
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
     * Creates a new ProtocolAuthenticate instance 
     *
     * @return new ProtocolAuthenticate instance
     */
    public static native ProtocolAuthenticate create() /*-{
        return {
          "_protoMessageName": "ProtocolAuthenticate",              
        };
    }-*/;

    /**
     * Creates a new JsArray<ProtocolAuthenticate> instance 
     *
     * @return new JsArray<ProtocolAuthenticate> instance
     */
    public static native JsArray<ProtocolAuthenticate> createArray() /*-{
        return [];
    }-*/;

    /**
     * Returns the name of this protocol buffer.
     */
    public static native String getProtocolBufferName(JavaScriptObject instance) /*-{
        return instance._protoMessageName;
    }-*/;

    /**
     * Gets a ProtocolAuthenticate (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return ProtocolAuthenticate
     */
    public static native ProtocolAuthenticate get(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Gets a JsArray<ProtocolAuthenticate> (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return JsArray<ProtocolAuthenticate> 
     */
    public static native JsArray<ProtocolAuthenticate> getArray(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Parses a ProtocolAuthenticate from a json string
     *
     * @param json string to be parsed/evaluated
     * @return ProtocolAuthenticate 
     */
    public static native ProtocolAuthenticate parse(String json) /*-{
        return eval("(" + json + ")");
    }-*/;

    /**
     * Parses a JsArray<ProtocolAuthenticate> from a json string
     *
     * @param json string to be parsed/evaluated
     * @return JsArray<ProtocolAuthenticate> 
     */
    public static native JsArray<ProtocolAuthenticate> parseArray(String json) /*-{
        return eval("(" + json + ")");
    }-*/;
    
    /**
     * Serializes a json object to a json string.
     *
     * @param ProtocolAuthenticate the object to serialize
     * @return String the serialized json string
     */
    public static native String stringify(ProtocolAuthenticate obj) /*-{
        var buf = [];
        var _1 = obj["1"];
        if(_1 != null)
            buf.push("\"1\":\"" + _1 + "\"");

        return buf.length == 0 ? "{}" : "{" + buf.join(",") + "}";
    }-*/;
    
    public static native boolean isInitialized(ProtocolAuthenticate obj) /*-{
        return 
            obj["1"] != null;
    }-*/;

    protected ProtocolAuthenticate() {}

    // getters and setters

    // token

    public final native String getToken() /*-{
        return this["1"] || "";
    }-*/;

    public final native ProtocolAuthenticate setToken(String token) /*-{
        this["1"] = token;
        return this;
    }-*/;

    public final native void clearToken() /*-{
        delete this["1"];
    }-*/;

    public final native boolean hasToken() /*-{
        return this["1"] != null;
    }-*/;


}