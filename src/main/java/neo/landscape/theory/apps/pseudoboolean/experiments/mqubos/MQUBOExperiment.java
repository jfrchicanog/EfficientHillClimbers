package neo.landscape.theory.apps.pseudoboolean.experiments.mqubos;

import neo.landscape.theory.apps.util.Process;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MQUBOExperiment implements Process  {

    private int minimumRange;
    private int maximumRange;

    private double maxRankObj1 = -1;

    private static final Comparator<Point> comparator = Comparator.comparing(Point::obj1)
        .thenComparing(Point::obj2)
        .thenComparing(Point::paretoLocalOptima);

    private record Point(double obj1, double obj2, boolean paretoLocalOptima) {
        public Point(double obj1, double obj2) {
            this(obj1, obj2, false);
        }
    }

    private double [][] q1, q2;
    private int n;
    private Point [] points;
    private boolean [] localOptima;
    private Map<List<Point>, Long> histogram;

    @Override
    public String getDescription() {
        return "Multi-objective QUBO experiment (Calais 2025)";
    }

    @Override
    public String getID() {
        return "mqubos";
    }

    @Override
    public String getInvocationInfo() {
        return "Usage: mqubos <n> <min> <max>";
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 3) {
            System.out.println(getInvocationInfo());
            return;
        }

        n = Integer.parseInt(args[0]);
        minimumRange = Integer.parseInt(args[1]);
        maximumRange = Integer.parseInt(args[2]);

        initialize();

        generateAllArrays(instance -> {
            int index=0;
            for (int i=0; i < n; i++) {
                for (int j=i; j < n; j++) {
                    q1[i][j] = instance[index++];
                }
            }

            for (int i=0; i < n; i++) {
                for (int j=i; j < n; j++) {
                    q2[i][j] = instance[index++];
                }
            }
            evaluateSolutions();
            computeLocaloptima();
            computeCanonicalRepresentative();

            histogram.compute(List.of(points), (k,v) -> v==null?1L:v+1L);
            return null;
        });

        System.out.println(histogram.size());
        OptionalLong max = histogram.values().stream().mapToLong(Long::intValue).max();
        max.ifPresent(val->{System.out.println(String.format("Maximum: %d", val));});
        histogram.entrySet().stream().forEach(entry -> {
            if (entry.getValue() == max.getAsLong()) {
                System.out.println(entry.getKey());
            }
        });
        System.out.print("Top 10 histogram values: ");
        histogram.values().stream().sorted(Comparator.reverseOrder()).limit(10).forEach(System.out::println);

        System.out.println("Max rank value: "+maxRankObj1);
    }

    private void computeLocaloptima() {
        for (int x=0; x < 1<<n; x++) {
            boolean isLocalOptimum = true;
            for (int i=0; i<n; i++) {
                int neighbor = x ^ (1<<i);
                if (points[neighbor].obj1() > points[x].obj1() && points[neighbor].obj2() >= points[x].obj2()) {
                    isLocalOptimum = false;
                    break;
                }

                if (points[neighbor].obj1() >= points[x].obj1() && points[neighbor].obj2() > points[x].obj2()) {
                    isLocalOptimum = false;
                    break;
                }
            }
            localOptima[x] = isLocalOptimum;
            points[x] = new Point(points[x].obj1(), points[x].obj2(), isLocalOptimum);
        }
    }

    private void generateAllArrays(Function<double[], Void> compute) {
        double [] array = new double[n*(n+1)];
        generateAllArrays(0, array, compute);
    }
    private void generateAllArrays(int index, double [] array, Function<double[], Void> compute) {
        if (index == array.length) {
            compute.apply(array);
            return;
        }
        for (int i = minimumRange; i <= maximumRange; i++) {
            if (index==1) {
                System.out.print("-");
            }
            array[index] = i;
            generateAllArrays(index+1, array, compute);
        }
    }

    private void computeCanonicalRepresentative() {
        double [] values1 = Stream.of(points).mapToDouble(Point::obj1).sorted().distinct().toArray();
        double [] values2 = Stream.of(points).mapToDouble(Point::obj2).sorted().distinct().toArray();
        Map<Double, Integer> rankMap1 = getRankMap(values1);
        Map<Double, Integer> rankMap2 = getRankMap(values2);

        if (values1.length > maxRankObj1) {
            maxRankObj1 = values1.length;
        }



        points = Stream.of(points)
            .map(p-> new Point(rankMap1.get(p.obj1()), rankMap2.get(p.obj2()), p.paretoLocalOptima()))
            .sorted(comparator)
            .collect(Collectors.toList())
            .toArray(new Point[0]);
        /*
        for (int x=0; x < points.length; x++) {
            points[x] = new Point(rankMap1.get(points[x].obj1),rankMap2.get(points[x].obj2));
        }*/
    }

    private Map<Double, Integer> getRankMap(double[] values) {
        Map<Double, Integer> rankMap = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            rankMap.put(values[i], i);
        }
        return rankMap;
    }

    private void evaluateSolutions() {
        for (int x=0; x < 1<<n; x++) {
            points[x] = evaluate(x);
        }
    }

    private void initialize() {
        q1 = new double[n][n];
        q2 = new double[n][n];
        points = new Point[1<<n];
        localOptima = new boolean[1<<n];
        histogram = new HashMap<>();
    }

    private Point evaluate (long x) {
        double f1 = 0.0;
        double f2 = 0.0;
        for (int i=0; i<n; i++) {
            if ((x&0x1)==1) {
                long shift = 1L;
                for (int j=i; j<n; j++) {
                    if ((x & shift) != 0) {
                        f1 += q1[i][j];
                        f2 += q2[i][j];
                    }
                    shift <<=1;
                }
            }
            x >>>=1;
        }
        return new Point(f1,f2);
    }


}
