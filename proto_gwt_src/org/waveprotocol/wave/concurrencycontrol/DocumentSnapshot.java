// Generated by http://code.google.com/p/protostuff/ ... DO NOT EDIT!
// Generated from clientserver.proto

package org.waveprotocol.wave.concurrencycontrol;

import com.google.gwt.core.client.*;

public class DocumentSnapshot extends JavaScriptObject  {


    public static class Builder extends DocumentSnapshot {
      protected Builder() { }
      public final DocumentSnapshot build() {
        return (DocumentSnapshot)this;
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
     * Creates a new DocumentSnapshot instance 
     *
     * @return new DocumentSnapshot instance
     */
    public static native DocumentSnapshot create() /*-{
        return {
          "_protoMessageName": "DocumentSnapshot",              
        };
    }-*/;

    /**
     * Creates a new JsArray<DocumentSnapshot> instance 
     *
     * @return new JsArray<DocumentSnapshot> instance
     */
    public static native JsArray<DocumentSnapshot> createArray() /*-{
        return [];
    }-*/;

    /**
     * Returns the name of this protocol buffer.
     */
    public static native String getProtocolBufferName(JavaScriptObject instance) /*-{
        return instance._protoMessageName;
    }-*/;

    /**
     * Gets a DocumentSnapshot (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return DocumentSnapshot
     */
    public static native DocumentSnapshot get(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Gets a JsArray<DocumentSnapshot> (casting) from a JavaScriptObject
     *
     * @param JavaScriptObject to cast
     * @return JsArray<DocumentSnapshot> 
     */
    public static native JsArray<DocumentSnapshot> getArray(JavaScriptObject jso) /*-{
        return jso;
    }-*/;

    /**
     * Parses a DocumentSnapshot from a json string
     *
     * @param json string to be parsed/evaluated
     * @return DocumentSnapshot 
     */
    public static native DocumentSnapshot parse(String json) /*-{
        return eval("(" + json + ")");
    }-*/;

    /**
     * Parses a JsArray<DocumentSnapshot> from a json string
     *
     * @param json string to be parsed/evaluated
     * @return JsArray<DocumentSnapshot> 
     */
    public static native JsArray<DocumentSnapshot> parseArray(String json) /*-{
        return eval("(" + json + ")");
    }-*/;
    
    /**
     * Serializes a json object to a json string.
     *
     * @param DocumentSnapshot the object to serialize
     * @return String the serialized json string
     */
    public static native String stringify(DocumentSnapshot obj) /*-{
        var buf = [];
        var _1 = obj["1"];
        if(_1 != null)
            buf.push("\"1\":\"" + _1 + "\"");
        var _2 = obj["2"];
        if(_2 != null)
            buf.push("\"2\":" + @org.waveprotocol.wave.federation.ProtocolDocumentOperation::stringify(Lorg/waveprotocol/wave/federation/ProtocolDocumentOperation;)(_2));
        var _3 = obj["3"];
        if(_3 != null)
            buf.push("\"3\":\"" + _3 + "\"");
        var _4 = obj["4"];
        if(_4 != null && _4.length != 0) {
            buf.push("\"4\":[\"" + _4.join("\",\"") + "\"]");
        }
        var _5 = obj["5"];
        if(_5 != null)
            buf.push("\"5\":" + _5);
        var _6 = obj["6"];
        if(_6 != null)
            buf.push("\"6\":" + _6);

        return buf.length == 0 ? "{}" : "{" + buf.join(",") + "}";
    }-*/;
    
    public static native boolean isInitialized(DocumentSnapshot obj) /*-{
        return 
            obj["1"] != null 
            && obj["2"] != null 
            && obj["3"] != null 
            && obj["5"] != null 
            && obj["6"] != null;
    }-*/;

    protected DocumentSnapshot() {}

    // getters and setters

    // documentId

    public final native String getDocumentId() /*-{
        return this["1"] || "";
    }-*/;

    public final native DocumentSnapshot setDocumentId(String documentId) /*-{
        this["1"] = documentId;
        return this;
    }-*/;

    public final native void clearDocumentId() /*-{
        delete this["1"];
    }-*/;

    public final native boolean hasDocumentId() /*-{
        return this["1"] != null;
    }-*/;

    // documentOperation

    public final native org.waveprotocol.wave.federation.ProtocolDocumentOperation getDocumentOperation() /*-{
        return this["2"];
    }-*/;

    public final native DocumentSnapshot setDocumentOperation(org.waveprotocol.wave.federation.ProtocolDocumentOperation documentOperation) /*-{
        this["2"] = documentOperation;
        return this;
    }-*/;

    public final native void clearDocumentOperation() /*-{
        delete this["2"];
    }-*/;

    public final native boolean hasDocumentOperation() /*-{
        return this["2"] != null;
    }-*/;

    // author

    public final native String getAuthor() /*-{
        return this["3"] || "";
    }-*/;

    public final native DocumentSnapshot setAuthor(String author) /*-{
        this["3"] = author;
        return this;
    }-*/;

    public final native void clearAuthor() /*-{
        delete this["3"];
    }-*/;

    public final native boolean hasAuthor() /*-{
        return this["3"] != null;
    }-*/;

    // contributor

    public final native JsArrayString getContributorArray() /*-{
        return this["4"];
    }-*/;

    public final java.util.List<String> getContributorList() {
        JsArrayString array = getContributorArray();
        java.util.List<String> list = new java.util.ArrayList<String>();
        
        if (array == null) {
          return null; 
        }
        for (int i=0; i < getContributorCount(); i++) {
          list.add(array.get(i));
        }
        return list;
    }

    public final native DocumentSnapshot setContributorArray(JsArrayString contributor) /*-{
        this["4"] = contributor;
        return this;
    }-*/;

    public final native JsArrayString clearContributorArray() /*-{
        return (this["4"] = []);
    }-*/;

    public final String getContributor(int index) {
        JsArrayString array = getContributorArray();
        return array == null ? null : array.get(index);
    }

    public final int getContributorCount() {
        JsArrayString array = getContributorArray();
        return array == null ? 0 : array.length();
    }

    public final void addContributor(String contributor) {
        JsArrayString array = getContributorArray();
        if(array == null)
            array = clearContributorArray();
        array.push(contributor);
    }

    // lastModifiedVersion

    public final native double getLastModifiedVersion() /*-{
        return this["5"] || 0;
    }-*/;

    public final native DocumentSnapshot setLastModifiedVersion(double lastModifiedVersion) /*-{
        this["5"] = lastModifiedVersion;
        return this;
    }-*/;

    public final native void clearLastModifiedVersion() /*-{
        delete this["5"];
    }-*/;

    public final native boolean hasLastModifiedVersion() /*-{
        return this["5"] != null;
    }-*/;

    // lastModifiedTime

    public final native double getLastModifiedTime() /*-{
        return this["6"] || 0;
    }-*/;

    public final native DocumentSnapshot setLastModifiedTime(double lastModifiedTime) /*-{
        this["6"] = lastModifiedTime;
        return this;
    }-*/;

    public final native void clearLastModifiedTime() /*-{
        delete this["6"];
    }-*/;

    public final native boolean hasLastModifiedTime() /*-{
        return this["6"] != null;
    }-*/;


}