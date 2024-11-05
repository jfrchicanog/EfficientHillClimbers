package neo.landscape.theory.apps.util;

import it.unimi.dsi.bits.TransformationStrategies;
import it.unimi.dsi.sux4j.mph.GOVMinimalPerfectHashFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinimalPerfectHashTests {

    public static final int NUMBER_TIMES = 10000000;
    private Random rnd;
    private List<Set<Integer>> keys;
    private List<List<Integer>> keyLists;

    @BeforeEach
    public void prepareRandom() {
        rnd = new Random(2);
        keys = provideSets().collect(Collectors.toList());
        keyLists = provideLists().collect(Collectors.toList());
    }

    private byte [] transformSet(Set<Integer> set) {
        byte [] result = new byte[3*set.size()];
        AtomicInteger i = new AtomicInteger(0);
        set.stream().sorted().forEach(x->{
            result[i.getAndIncrement()] = (byte)(x & 0xFF);
            x >>>= 8;
            result[i.getAndIncrement()] = (byte)(x & 0xFF);
            x >>>= 8;
            result[i.getAndIncrement()] = (byte)(x & 0xFF);
            /*x >>>= 8;
            result[i.getAndIncrement()] = (byte)(x & 0xFF);*/
        });

        return result;
    }

    private byte [] transformList(List<Integer> list) {
        byte [] result = new byte[3*list.size()];
        AtomicInteger i = new AtomicInteger(0);
        list.stream().forEach(x->{
            result[i.getAndIncrement()] = (byte)(x & 0xFF);
            x >>>= 8;
            result[i.getAndIncrement()] = (byte)(x & 0xFF);
            x >>>= 8;
            result[i.getAndIncrement()] = (byte)(x & 0xFF);
            /*x >>>= 8;
            result[i.getAndIncrement()] = (byte)(x & 0xFF);*/
        });

        return result;
    }

    private Stream<Set<Integer>> provideSets() {
        Stream.Builder<Set<Integer>> sets = Stream.builder();
        for (int i = 0; i < 100000; i++) {
            sets.add(Set.of(i, i+1, i+2));
            sets.add(Set.of(i, i+1));
            sets.add(Set.of(i, i+2));
            sets.add(Set.of(i));
        }
        return sets.build();
    }

    private Stream<List<Integer>> provideLists() {
        Stream.Builder<List<Integer>> lists = Stream.builder();
        for (int i = 0; i < 100000; i++) {
            lists.add(List.of(i, i+1, i+2));
            lists.add(List.of(i, i+1));
            lists.add(List.of(i, i+2));
            lists.add(List.of(i));
        }
        return lists.build();
    }

    private GOVMinimalPerfectHashFunction<byte []> prepareMPH(Stream<Set<Integer>> stream) throws Exception {
        Iterable<byte[]> keys = stream
            .map(this::transformSet)
            .collect(Collectors.toList());
        GOVMinimalPerfectHashFunction.Builder<byte[]> builder = new GOVMinimalPerfectHashFunction.Builder<>();
        return builder.keys(keys)
            .transform(TransformationStrategies.rawByteArray())
            .build();
    }

    private GOVMinimalPerfectHashFunction<byte []> prepareMPHList(Stream<List<Integer>> stream) throws Exception {
        Iterable<byte[]> keys = stream
            .map(this::transformList)
            .collect(Collectors.toList());
        GOVMinimalPerfectHashFunction.Builder<byte[]> builder = new GOVMinimalPerfectHashFunction.Builder<>();
        return builder.keys(keys)
            .transform(TransformationStrategies.rawByteArray())
            .build();
    }

    @Test
    public void testMPH() throws Exception {
        long start = System.nanoTime();
        GOVMinimalPerfectHashFunction<byte []> mph = prepareMPH(keys.stream());
        int [] values = new int[(int)mph.size64()];
        long endPreparation = System.nanoTime();

        for (int i = 0; i < NUMBER_TIMES; i++) {
            byte [] key = transformSet(keys.get(i % keys.size()));
            int val = (int)mph.getLong(key);
            values[val]++;
        }
        long end = System.nanoTime();

        System.out.println("MPH Preparation time: " + (endPreparation - start)/1e9);
        System.out.println("MPH Query time: " + (end - endPreparation)/1e9/NUMBER_TIMES);
        System.out.println("MPH Size: " + mph.size64());

    }

    private Map<Set<Integer>, Integer> prepareMap(Stream<Set<Integer>> sets) {
        Map<Set<Integer>, Integer> map = new HashMap<>();
        sets.forEach(set->map.put(set, 0));
        return map;
    }

    @Test
    public void testHashMap() throws Exception {
        long start = System.nanoTime();
        Map<Set<Integer>, Integer> map = prepareMap(keys.stream());
        long endPreparation = System.nanoTime();

        for (int i = 0; i < NUMBER_TIMES; i++) {
            //int val = map.get(keys.get(i % keys.size()));
            map.compute(keys.get(i % keys.size()), (k, v)->v+1);
        }
        long end = System.nanoTime();

        System.out.println("Map Preparation time: " + (endPreparation - start)/1e9);
        System.out.println("Map Query time: " + (end - endPreparation)/1e9/NUMBER_TIMES);
        System.out.println("Map Size: " + map.size());

    }

    @Test
    public void testMPHList() throws Exception {
        long start = System.nanoTime();
        GOVMinimalPerfectHashFunction<byte []> mph = prepareMPHList(keyLists.stream());
        int [] values = new int[(int)mph.size64()];
        long endPreparation = System.nanoTime();

        for (int i = 0; i < NUMBER_TIMES; i++) {
            byte [] key = transformList(keyLists.get(i % keyLists.size()));
            int val = (int)mph.getLong(key);
            values[val]++;
        }
        long end = System.nanoTime();

        System.out.println("MPH Lists Preparation time: " + (endPreparation - start)/1e9);
        System.out.println("MPH Lists Query time: " + (end - endPreparation)/1e9/NUMBER_TIMES);
        System.out.println("MPH Lists Size: " + mph.size64());

    }
}
