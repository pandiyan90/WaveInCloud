// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.wave.api;

import junit.framework.TestCase;

/**
 * @author jli@google.com (Jimin Li)
 *
 */
public class AttachmentRobotTest extends TestCase {
 
  public static final String CAPTION = "caption";
  public static final byte[] DATA = "data".getBytes();

  public void testConstructorWithCaptionAndData() { 
    Attachment attachment = new Attachment(CAPTION, DATA);
    assertEquals(ElementType.ATTACHMENT, attachment.getType());
    assertEquals(CAPTION, attachment.getCaption());
    assertEquals(DATA, attachment.getData());
  }

  public void testSetProperty(){
    Attachment attachment = new Attachment(CAPTION, DATA);
    try {
      attachment.setProperty(Attachment.ATTACHMENT_ID, "attachment1");
      fail("Should have thrown exception when trying to set attachment id.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    
    try {
      attachment.setProperty(Attachment.MIME_TYPE, "m1");
      fail("Should have thrown exception when trying to set mime type.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      attachment.setProperty(Attachment.ATTACHMENT_URL, "a_url");
      fail("Should have thrown exception when trying to set attachment url.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      attachment.setProperty(Attachment.DATA, "data");
      fail("Should have thrown exception when trying to set data.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    attachment.setCaption("new caption");
    assertEquals("new caption", attachment.getCaption());
  } 
}
