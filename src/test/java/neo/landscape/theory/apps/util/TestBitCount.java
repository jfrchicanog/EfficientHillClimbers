package neo.landscape.theory.apps.util;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBitCount {

    @Test
    public void testBitCount() {
        long start = System.nanoTime();
        long count=0;
        for (long i=0; i < Integer.MAX_VALUE; i++) {
            count += (Long.bitCount(i)&0x01);
        }
        long end = System.nanoTime();
        System.out.println("Tiempo en nano: "+(end-start));
    }

    @Test
    public void testCompareBitCount() {
        long start = System.nanoTime();
        long count=0;
        for (long i=0; i < Integer.MAX_VALUE; i++) {
            long r1 = Long.bitCount(i)&0x01;
            long r2 = myBitCount(i);
            assertThat(r1).isEqualTo(r2);
        }
        long end = System.nanoTime();
        System.out.println("Tiempo en nano: "+(end-start));
    }

    @Test
    public void testMyBitCount() {
        long start = System.nanoTime();
        long count=0;
        for (long i=0; i < Integer.MAX_VALUE; i++) {
            count += myBitCount(i);
        }
        long end = System.nanoTime();
        System.out.println("Tiempo en nano: "+(end-start));
    }

    private long myBitCount(long i) {
        i = ((i >>> 1) ^ i);
        i = ((i >>> 2) ^ i);
        i = ((i >>> 4) ^ i);
        i = ((i >>> 8) ^ i);
        i = ((i >>> 16) ^ i);
        i = ((i >>> 32) ^ i);
        return i&0x01;
    }

}
