// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: org/waveprotocol/pst/protobuf/extensions.proto

package org.waveprotocol.pst.protobuf;

public final class Extensions {
  private Extensions() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registry.add(org.waveprotocol.pst.protobuf.Extensions.int53);
  }
  public static final int INT53_FIELD_NUMBER = 20000;
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.FieldOptions,
      java.lang.Boolean> int53 =
        com.google.protobuf.GeneratedMessage
          .newGeneratedExtension();
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n.org/waveprotocol/pst/protobuf/extensio" +
      "ns.proto\032 google/protobuf/descriptor.pro" +
      "to:.\n\005int53\022\035.google.protobuf.FieldOptio" +
      "ns\030\240\234\001 \001(\010B\037\n\035org.waveprotocol.pst.proto" +
      "buf"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          org.waveprotocol.pst.protobuf.Extensions.int53.internalInit(
              org.waveprotocol.pst.protobuf.Extensions.getDescriptor().getExtensions().get(0),
              java.lang.Boolean.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.DescriptorProtos.getDescriptor(),
        }, assigner);
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}