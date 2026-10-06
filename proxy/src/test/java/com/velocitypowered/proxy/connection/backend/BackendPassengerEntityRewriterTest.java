package com.velocitypowered.proxy.connection.backend;

import static org.junit.jupiter.api.Assertions.*;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class BackendPassengerEntityRewriterTest {
  private static ByteBuf packet(int vehicle, int... passengers) {
    ByteBuf frame = Unpooled.buffer();
    ProtocolUtils.writeVarInt(frame, 0x6D);
    ProtocolUtils.writeVarInt(frame, vehicle);
    ProtocolUtils.writeVarInt(frame, passengers.length);
    for (int passenger : passengers) ProtocolUtils.writeVarInt(frame, passenger);
    return frame;
  }

  @Test void riderIdIsTranslatedAndBoatAndSecondPassengerArePreserved() {
    for (int[] passengers : new int[][]{{300}, {300, 42}, {42, 300}}) {
      ByteBuf input=packet(99,passengers);ByteBuf output=null;
      int[] expectedPassengers=passengers.clone();
      for(int i=0;i<expectedPassengers.length;i++) if(expectedPassengers[i]==300) expectedPassengers[i]=Integer.MAX_VALUE;
      ByteBuf expected=packet(99,expectedPassengers);
      byte[] original=ByteBufUtil.getBytes(input);
      try {
        output=BackendSoundEntityRewriter.forward(input,ProtocolVersion.MINECRAFT_26_3,300,Integer.MAX_VALUE);
        assertArrayEquals(ByteBufUtil.getBytes(expected),ByteBufUtil.getBytes(output));
        assertArrayEquals(original,ByteBufUtil.getBytes(input));
        assertEquals(0,input.readerIndex());assertEquals(1,input.refCnt());assertEquals(1,output.refCnt());
      } finally {input.release();expected.release();if(output!=null)output.release();}
    }
  }

  @Test void emptyDismountAndOtherPlayersBoatPassThroughWithExactlyOneRetainedReference() {
    for(int[] passengers:new int[][]{{},{42},{42,43}}) {
      ByteBuf input=packet(99,passengers);ByteBuf output=null;
      try {
        output=BackendSoundEntityRewriter.forward(input,ProtocolVersion.MINECRAFT_26_3,300,Integer.MAX_VALUE);
        assertSame(input,output);assertEquals(2,input.refCnt());
      } finally {if(output!=null)output.release();input.release();}
    }
  }

  @Test void matchesAcrossVarintGrowthShrinkVehicleReferencesAndNonzeroReaderIndex() {
    for(int[] ids:new int[][]{{1,Integer.MAX_VALUE},{Integer.MAX_VALUE,1},{128,16384}}) {
      ByteBuf body=packet(ids[0],42,ids[0]);ByteBuf expected=packet(ids[1],42,ids[1]);
      ByteBuf storage=Unpooled.buffer().writeLong(123).writeBytes(body);storage.readerIndex(8);ByteBuf output=null;
      try {
        output=BackendPassengerEntityRewriter.rewrite(storage.slice(),ProtocolVersion.MINECRAFT_26_3,ids[0],ids[1]);
        assertArrayEquals(ByteBufUtil.getBytes(expected),ByteBufUtil.getBytes(output));
        assertEquals(8,storage.readerIndex());assertEquals(123,storage.getLong(0));
      } finally {body.release();expected.release();storage.release();if(output!=null)output.release();}
    }
  }

  @Test void wrongProtocolAndUnchangedOrMissingIdentityCannotRewrite() {
    ByteBuf input=packet(99,300);
    try {
      for(var version:new ProtocolVersion[]{ProtocolVersion.MINECRAFT_1_21_11,ProtocolVersion.MINECRAFT_26_1,ProtocolVersion.MINECRAFT_26_2})
        assertNull(BackendPassengerEntityRewriter.rewrite(input,version,300,Integer.MAX_VALUE));
      assertNull(BackendPassengerEntityRewriter.rewrite(input,ProtocolVersion.MINECRAFT_26_3,300,300));
      assertNull(BackendPassengerEntityRewriter.rewrite(input,ProtocolVersion.MINECRAFT_26_3,null,Integer.MAX_VALUE));
      assertNull(BackendPassengerEntityRewriter.rewrite(input,ProtocolVersion.MINECRAFT_26_3,300,null));
    } finally {input.release();}
  }

  @Test void malformedCountsAndTruncatedReferencesFailBeforeAllocatingReplacement() {
    for(int count:new int[]{-1,3,Integer.MAX_VALUE}) {
      ByteBuf input=Unpooled.buffer();ProtocolUtils.writeVarInt(input,0x6D);ProtocolUtils.writeVarInt(input,99);
      ProtocolUtils.writeVarInt(input,count);ProtocolUtils.writeVarInt(input,300);
      try {
        byte[] original=ByteBufUtil.getBytes(input);
        assertThrows(IllegalArgumentException.class,()->BackendPassengerEntityRewriter.rewrite(input,ProtocolVersion.MINECRAFT_26_3,300,Integer.MAX_VALUE));
        assertArrayEquals(original,ByteBufUtil.getBytes(input));assertEquals(1,input.refCnt());
      } finally {input.release();}
    }
    ByteBuf input=packet(99,300).writeByte(1);
    try {assertThrows(IllegalArgumentException.class,()->BackendPassengerEntityRewriter.rewrite(input,ProtocolVersion.MINECRAFT_26_3,300,Integer.MAX_VALUE));}
    finally {input.release();}
    ByteBuf truncated=packet(99,300);truncated.writerIndex(truncated.writerIndex()-1);
    try {
      assertThrows(RuntimeException.class,()->BackendPassengerEntityRewriter.rewrite(truncated,ProtocolVersion.MINECRAFT_26_3,300,Integer.MAX_VALUE));
      assertEquals(1,truncated.refCnt());
    } finally {truncated.release();}
  }
}
