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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.protobuf.ByteString;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.waveserver.AppliedDeltaUtil;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A flat file based implementation of DeltasAccess. This class provides a storage backend for the
 * deltas in a single wavelet.
 *
 * The file starts with a header. The header contains the version of the file protocol. After the
 * version, the file contains a sequence of delta records. Each record contains a header followed
 * by a WaveletDeltaRecord.
 *
 * See this document for design specifics:
 * https://sites.google.com/a/waveprotocol.org/wave-protocol/protocol/design-proposals/wave-store-design-for-wave-in-a-box
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FileDeltaCollection implements DeltasAccess {
  public static final String DELTAS_FILE_SUFFIX = ".deltas";
  public static final String INDEX_FILE_SUFFIX = ".index";

  private static final byte[] FILE_MAGIC_BYTES = new byte[]{'W', 'A', 'V', 'E'};
  private static final short FILE_PROTOCOL_VERSION = 1;
  private static final int FILE_HEADER_LENGTH = 6;

  private static final int DELTA_PROTOCOL_VERSION = 1;

  private final WaveletName waveletName;
  private final String basePath;
  private final String deltasFilename;
  private final String indexFilename;

  private RandomAccessFile file;
  private DeltaIndex index;
  private HashedVersion endVersion;

  /**
   * A single record in the delta file.
   */
  private class DeltaHeader {
    /** Length in bytes of the header */
    public static final int HEADER_LENGTH = 12;

    /** The protocol version of the remaining fields. For now, must be 1. */
    public final int protoVersion;

    /** The length of the applied delta segment, in bytes. */
    public final int appliedDeltaLength;
    public final int transformedDeltaLength;

    public DeltaHeader(int protoVersion, int appliedDeltaLength, int transformedDeltaLength) {
      this.protoVersion = protoVersion;
      this.appliedDeltaLength = appliedDeltaLength;
      this.transformedDeltaLength = transformedDeltaLength;
    }

    public void checkVersion() throws IOException {
      if (protoVersion != DELTA_PROTOCOL_VERSION) {
        throw new IOException("Invalid delta header");
      }
    }
  }

  /**
   * Create a new file delta collection for the given wavelet.
   *
   * @param waveletName name of the wavelet
   * @param basePath the file store's base path
   */
  public FileDeltaCollection(WaveletName waveletName, String basePath) {
    Preconditions.checkNotNull(waveletName, "wavelet name cannot be null");
    Preconditions.checkNotNull(basePath, "base path cannot be null");

    this.waveletName = waveletName;
    this.basePath = basePath;

    String waveletPathPrefix = FileUtils.waveletNameToPathSegment(waveletName);
    deltasFilename = waveletPathPrefix + DELTAS_FILE_SUFFIX;
    indexFilename = waveletPathPrefix + INDEX_FILE_SUFFIX;
  }

  private boolean isOpen() {
    return file != null;
  }

  private void setOrCheckFileHeader() throws IOException {
    Preconditions.checkNotNull(file);
    file.seek(0);

    if (file.length() < FILE_HEADER_LENGTH) {
      // The file is new. Insert a header.
      file.write(FILE_MAGIC_BYTES);
      file.writeShort(FILE_PROTOCOL_VERSION);
    } else {
      byte[] magic = new byte[4];
      file.readFully(magic);
      if (!Arrays.equals(FILE_MAGIC_BYTES, magic)) {
        throw new IOException("Delta file magic bytes are incorrect");
      }

      short version = file.readShort();
      if (version != FILE_PROTOCOL_VERSION) {
        throw new IOException(String.format("File protocol version mismatch - expected %d got %d",
            FILE_PROTOCOL_VERSION, version));
      }
    }
  }

  /** Open the delta collection, if the collection is not already open. */
  private void openIfNeeded() throws IOException {
    if (!isOpen()) {
      File fileRef = new File(basePath, deltasFilename);
      file = FileUtils.getOrCreateFile(fileRef);
      setOrCheckFileHeader();

      index = new DeltaIndex(new File(basePath, indexFilename));
      index.openForCollection(this);

      long numRecords = index.length();
      if (numRecords >= 1) {
        endVersion = getDeltaByEndVersion(numRecords).getResultingVersion();
      } else {
        endVersion = null;
      }

      // After reading the last record, the file's position should be at the end. There might
      // be trailing junk here if there was a partially completed write, and the server crashed.
      // So, we'll truncate the file here.
      file.setLength(file.getFilePointer());
    }
  }

  /**
   * Delete the delta files from disk.
   *
   * @throws IOException
   */
  public void delete() throws IOException, FileNotFoundException {
    close();

    File deltas = new File(basePath, deltasFilename);
    if (deltas.exists()) {
      if (!deltas.delete()) {
        throw new IOException(String.format("Could not delete deltas file: %s", deltasFilename));
      }
    } else {
      throw new FileNotFoundException("Deltas already deleted");
    }

    File index = new File(basePath, indexFilename);
    if (index.exists()) {
      if (!index.delete()) {
        throw new IOException(String.format("Could not delete index file: %s", indexFilename));
      }
    }
  }

  @Override
  public WaveletName getWaveletName() {
    return waveletName;
  }

  @Override
  public HashedVersion getEndVersion() {
    return endVersion;
  }

  @Override
  public WaveletDeltaRecord getDelta(long version) throws IOException {
    return seekToRecord(version) ? readRecord() : null;
  }

  @Override
  public WaveletDeltaRecord getDeltaByEndVersion(long version) throws IOException {
    return seekToEndRecord(version) ? readRecord() : null;
  }
  
  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(long version)
      throws IOException {
    return seekToRecord(version) ? readAppliedDeltaFromRecord() : null;
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(long version) throws IOException {
    return seekToRecord(version) ? readTransformedDeltaFromRecord() : null;
  }

  @Override
  public HashedVersion getAppliedAtVersion(long version) throws IOException {
    ByteStringMessage<ProtocolAppliedWaveletDelta> applied = getAppliedDelta(version);

    return (applied != null) ? AppliedDeltaUtil.getHashedVersionAppliedAt(applied) : null;
  }

  @Override
  public HashedVersion getResultingVersion(long version) throws IOException {
    TransformedWaveletDelta transformed = getTransformedDelta(version);

    return (transformed != null) ? transformed.getResultingVersion() : null;
  }

  @Override
  public void close() throws IOException {
    if (file != null) {
      file.close();
      file = null;
      if (index != null) {
        index.close();
        index = null;
      }
      endVersion = null;
    }
  }

  @Override
  public void append(Collection<WaveletDeltaRecord> deltas) throws IOException {
    openIfNeeded();

    file.seek(file.length());

    for (WaveletDeltaRecord delta : deltas) {
      index.addDelta(delta.transformed.getAppliedAtVersion(),
          delta.transformed.getOperations().size(), file.getFilePointer());
      writeDelta(delta);
      endVersion = delta.transformed.getResultingVersion();
    }

    // fsync() before returning.
    file.getChannel().force(true);
  }

  @Override
  public boolean isEmpty() throws IOException {
    openIfNeeded();

    return index.length() == 0;
  }

  /**
   * Creates a new iterator to move over the positions of the deltas in the file.
   *
   * Each pair returned is ((version, numOperations), offset).
   * @throws IOException
   */
  Iterable<Pair<Pair<Long,Integer>, Long>> getOffsetsIterator() throws IOException {
    openIfNeeded();

    return new Iterable<Pair<Pair<Long, Integer>, Long>>() {
      @Override
      public Iterator<Pair<Pair<Long, Integer>, Long>> iterator() {
        return new Iterator<Pair<Pair<Long, Integer>, Long>>() {
          Pair<Pair<Long, Integer>, Long> nextRecord;
          long nextPosition = FILE_HEADER_LENGTH;

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Pair<Pair<Long, Integer>, Long> next() {
            Pair<Pair<Long, Integer>, Long> record = nextRecord;
            nextRecord = null;
            return record;
          }

          @Override
          public boolean hasNext() {
            // We're using hasNext to prime the next call to next(). This works because in practice
            // any call to next() is preceeded by at least one call to hasNext().
            // We need to actually read the record here because hasNext() should return false
            // if there's any incomplete data at the end of the file.
            try {
              if (file.length() <= nextPosition) {
                // End of file.
                return false;
              }
            } catch (IOException e) {
              throw new RuntimeException("Could not get file position", e);
            }

            if (nextRecord == null) {
              // Read the next record
              try {
                file.seek(nextPosition);
                TransformedWaveletDelta transformed = readTransformedDeltaFromRecord();
                nextRecord = Pair.of(Pair.of(transformed.getAppliedAtVersion(),
                        transformed.getOperations().size()), nextPosition);
                nextPosition = file.getFilePointer();
              } catch (IOException e) {
                // The next entry is invalid. There was probably a write error / crash.
                return false;
              }
            }

            return true;
          }
        };
      }
    };
  }

  /** 
   * Seek to the start of a delta record. Returns false if the record doesn't exist.
   */
  private boolean seekToRecord(long version) throws IOException {
    Preconditions.checkArgument(version >= 0, "Version can't be negative");

    long offset = index.getOffsetForVersion(version);
    return seekTo(offset);
  }

  /** 
   * Seek to the start of a delta record given its end version. 
   * Returns false if the record doesn't exist. 
   */
  private boolean seekToEndRecord(long version) throws IOException {
    Preconditions.checkArgument(version >= 0, "Version can't be negative");

    long offset = index.getOffsetForEndVersion(version);
    return seekTo(offset);
  }

  private boolean seekTo(long offset) throws IOException {
    if (offset == DeltaIndex.NO_RECORD_FOR_VERSION) {
      // There's no record for the specified version.
      return false;
    } else {
      openIfNeeded();
      file.seek(offset);
      return true;
    }
  }

  /**
   * Read a record and return it.
   */
  private WaveletDeltaRecord readRecord() throws IOException {
    DeltaHeader header = readDeltaHeader();

    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        readAppliedDelta(header.appliedDeltaLength);
    TransformedWaveletDelta transformedDelta = readTransformedWaveletDelta();

    return new WaveletDeltaRecord(appliedDelta, transformedDelta);
  }

  /**
   * Reads a record, and only parses & returns the applied data field.
   */
  private ByteStringMessage<ProtocolAppliedWaveletDelta> readAppliedDeltaFromRecord()
      throws IOException {
    DeltaHeader header = readDeltaHeader();

    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        readAppliedDelta(header.appliedDeltaLength);
    file.skipBytes(header.transformedDeltaLength);

    return appliedDelta;
  }

  /**
   * Reads a record, and only parses & returns the transformed data field.
   */
  private TransformedWaveletDelta readTransformedDeltaFromRecord() throws IOException {
    DeltaHeader header = readDeltaHeader();

    file.skipBytes(header.appliedDeltaLength);
    TransformedWaveletDelta transformedDelta = readTransformedWaveletDelta();

    return transformedDelta;
  }


  // *** Low level data reading methods

  /** Read a header from the file. Does not move the file pointer before reading. */
  private DeltaHeader readDeltaHeader() throws IOException {
    int version = file.readInt();
    if (version != DELTA_PROTOCOL_VERSION) {
      throw new IOException("Delta header invalid");
    }
    int appliedDeltaLength = file.readInt();
    int transformedDeltaLength = file.readInt();
    DeltaHeader deltaHeader = new DeltaHeader(version, appliedDeltaLength, transformedDeltaLength);
    deltaHeader.checkVersion();
    return deltaHeader;
  }

  /**
   * Write a header to the current location in the file
   */
  private void writeDeltaHeader(DeltaHeader header) throws IOException {
    file.writeInt(header.protoVersion);
    file.writeInt(header.appliedDeltaLength);
    file.writeInt(header.transformedDeltaLength);
  }

  /**
   * Read the applied delta at the current file position. After method call,
   * file position is directly after applied delta field.
   */
  private ByteStringMessage<ProtocolAppliedWaveletDelta> readAppliedDelta(int length)
      throws IOException {
    if (length == 0) {
      return null;
    }

    byte[] bytes = new byte[length];
    file.readFully(bytes);
    return ByteStringMessage.parseProtocolAppliedWaveletDelta(ByteString.copyFrom(bytes));
  }

  /**
   * Write an applied delta to the current position in the file. Returns number of bytes written.
   */
  private int writeAppliedDelta(ByteStringMessage<ProtocolAppliedWaveletDelta> delta)
      throws IOException {
    if (delta != null) {
      byte[] bytes = delta.getByteArray();
      file.write(bytes);
      return bytes.length;
    } else {
      return 0;
    }
  }

  private TransformedWaveletDelta readTransformedWaveletDelta() throws IOException {
    // The data is:
    // - author (string)
    // - resultingVersion (ProtoHashedVersion)
    // - timestamp (long)
    // - number of ops (int)
    // - ops (list of ProtocolWaveletOperation)

    // Read the author
    String authorStr = file.readUTF();
    ParticipantId author;
    try {
      author = ParticipantId.of(authorStr);
    } catch (InvalidParticipantAddress e) {
      throw new IOException("Author field invalid", e);
    }

    // Read the resultingVersion
    InputStream stream = Channels.newInputStream(file.getChannel());
    ProtocolHashedVersion resultingVersionProto = ProtocolHashedVersion.parseDelimitedFrom(stream);
    HashedVersion resultingVersion =
        CoreWaveletOperationSerializer.deserialize(resultingVersionProto);

    // Read the application timestamp
    long timestamp = file.readLong();

    // Read the ops.
    int num = file.readInt();
    Builder<ProtocolWaveletOperation> protoOpBuilder = ImmutableList.builder();
    for (int i = 0; i < num; i++) {
      ProtocolWaveletOperation protoOp = ProtocolWaveletOperation.parseDelimitedFrom(stream);
      protoOpBuilder.add(protoOp);
    }
    ImmutableList<ProtocolWaveletOperation> protoOps = protoOpBuilder.build();

    Builder<WaveletOperation> opBuilder = ImmutableList.builder();
    for (int i = 0; i < protoOps.size(); i++) {
      ProtocolWaveletOperation protoOp = protoOps.get(i);
      WaveletOperationContext context;
      if (i == protoOps.size() - 1) {
        // The last op has its hashedVersion set to the resultingVersion of the op stream.
        context = new WaveletOperationContext(author, timestamp, 1, resultingVersion);
      } else {
        context = new WaveletOperationContext(author, timestamp, 1);
      }

      opBuilder.add(CoreWaveletOperationSerializer.deserialize(protoOp, context));
    }

    return new TransformedWaveletDelta(
        author, resultingVersion, timestamp, opBuilder.build());
  }

  // Returns length of written data
  private int writeTransformedWaveletDelta(TransformedWaveletDelta delta) throws IOException {
    long startingPosition = file.getFilePointer();

    // Write author
    file.writeUTF(delta.getAuthor().getAddress());

    // Write resulting version
    HashedVersion resultingVersion = delta.getResultingVersion();
    ProtocolHashedVersion protoHashedVersion =
        CoreWaveletOperationSerializer.serialize(resultingVersion);
    OutputStream stream = Channels.newOutputStream(file.getChannel());
    protoHashedVersion.writeDelimitedTo(stream);

    // Write timestamp
    file.writeLong(delta.getApplicationTimestamp());

    // Write ops
    List<? extends WaveletOperation> ops = delta.getOperations();
    file.writeInt(ops.size());
    for (WaveletOperation op : ops) {
      ProtocolWaveletOperation protoOp = CoreWaveletOperationSerializer.serialize(op);
      protoOp.writeDelimitedTo(stream);
    }

    return (int) (file.getFilePointer() - startingPosition);
  }

  /**
   * Read a delta to the file. Does not move the file pointer before writing. Returns number of
   * bytes written.
   */
  private long writeDelta(WaveletDeltaRecord delta) throws IOException {
    // We'll write zeros in place of the header and come back & write it at the end.
    long headerPointer = file.getFilePointer();
    file.write(new byte[DeltaHeader.HEADER_LENGTH]);

    int appliedLength = writeAppliedDelta(delta.applied);
    int transformedLength = writeTransformedWaveletDelta(delta.transformed);

    long endPointer = file.getFilePointer();
    file.seek(headerPointer);
    writeDeltaHeader(new DeltaHeader(DELTA_PROTOCOL_VERSION, appliedLength, transformedLength));
    file.seek(endPointer);

    return endPointer - headerPointer;
  }
}