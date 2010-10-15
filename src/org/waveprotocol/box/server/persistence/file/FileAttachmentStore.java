/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.server.persistence.file;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.AttachmentUtil;
import org.waveprotocol.wave.model.util.CharBase64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * An implementation of AttachmentStore which uses files on disk
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FileAttachmentStore implements AttachmentStore {

  /**
   * The directory in which the attachments are stored
   */
  private String path;

  @Inject
  public FileAttachmentStore(@Named("attachment_store_directory") String requestedPath) {
    path = makePath(requestedPath);
  }

  private static String makePath(String requestedPath) {
    // Should check the directory exists and create it if it doesn't.
    File path = new File(requestedPath);

    if (!path.exists()) {
      boolean succeeded = path.mkdirs();
      if (!succeeded) {
        throw new RuntimeException("Cannot create attachment store at path '" + requestedPath + "'");
      }
    }

    return path.getAbsolutePath();
  }

  private static String encodeId(String id) {
    try {
      return CharBase64.encode(id.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private String getAttachmentPath(String id) {
    return path + File.separatorChar + encodeId(id);
  }

  @Override
  public AttachmentData getAttachment(String id) {
    final File file = new File(getAttachmentPath(id));
    if (!file.exists() || !file.canRead()) {
      return null;
    }

    return new AttachmentData() {
      @Override
      public Date getLastModifiedDate() {
        return new Date(file.lastModified());
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
      }

      @Override
      public long getContentSize() {
        return file.length();
      }

      @Override
      public void writeDataTo(OutputStream out) throws IOException {
        InputStream is = getInputStream();
        AttachmentUtil.writeTo(is, out);
        is.close();
      }
    };
  }

  @Override
  public boolean storeAttachment(String id, InputStream data) throws IOException {
    File file = new File(getAttachmentPath(id));

    if (file.exists()) {
      return false;
    } else {
      FileOutputStream stream = new FileOutputStream(file);
      AttachmentUtil.writeTo(data, stream);
      stream.close();
      return true;
    }
  }

  @Override
  public void deleteAttachment(String id) {
    File file = new File(getAttachmentPath(id));
    if (file.exists()) {
      file.delete();
    }
  }
}
