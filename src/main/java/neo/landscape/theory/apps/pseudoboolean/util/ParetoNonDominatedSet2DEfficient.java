package neo.landscape.theory.apps.pseudoboolean.util;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

public class ParetoNonDominatedSet2DEfficient implements Iterable<double []>, IParetoNonDominatedSet<ParetoNonDominatedSet2DEfficient>{

    private NavigableSet<double []> archive;
    public static final Comparator<double[]> COMPARATOR_2D =
            Comparator.<double[]>comparingDouble(d -> d[0])
                .thenComparing(d->d[1]);

    public ParetoNonDominatedSet2DEfficient() {
        archive = new TreeSet<double []>(COMPARATOR_2D);
    }

    @Override
    public void clear() {
        archive.clear();
    }

    @Override
    public int size() {
        return archive.size();
    }

    @Override
    public IParetoNonDominatedSetFactory<ParetoNonDominatedSet2DEfficient> getFactory() {
        return new ParetoNonDominatedSet2DFactory();
    }

    @Override
    @Nonnull
    public Iterator<double []> iterator() {
        return archive.iterator();
    }

    public Stream<double[]> stream() {
        return archive.stream();
    }

    public void addPoint(double[] point) {
        var first = archive.tailSet(point).stream().findFirst();
        if (first.isPresent()) {
            double[] firstPoint = first.get();
            if (firstPoint[1] >= point[1]) {
                // point is dominated
                return;
            }
        }
        archive.headSet(point, false)
            .descendingSet()
            .stream()
            .takeWhile(p -> p[1] <= point[1])
            .toList()
            .forEach(archive::remove);
        archive.add(point);
    }

    public static void combine(ParetoNonDominatedSet2DEfficient source1, double [] offset1, ParetoNonDominatedSet2DEfficient source2, double [] offset2, ParetoNonDominatedSet2DEfficient target) {
        target.clear();
        if (!source1.archive.isEmpty() && source2.archive.isEmpty()) {
            moveToTarget(source1, offset1, target);
        } else if (!source2.archive.isEmpty() && source1.archive.isEmpty()) {
            moveToTarget(source2, offset2, target);
        } else if (!source1.archive.isEmpty()) {
            // both sets are non-empty
            Iterator<double [] > it1 = source1.archive.iterator();
            Iterator<double [] > it2 = source2.archive.iterator();
            Optional<double []> p1 = advanceIterator(it1);
            Optional<double []> p2 = advanceIterator(it2);

            while (p1.isPresent() && p2.isPresent()) {
                // if p1 dominates or is equal to p2
                if (dominatesOrEqual(p1.get(), offset1, p2.get(), offset2)) {
                    do {
                        p2 = advanceIterator(it2);
                    } while(p2.isPresent() && dominatesOrEqual(p1.get(), offset1, p2.get(), offset2));
                    // store p1 in the target set and advance it1
                    addWithOffset(p1.get(), offset1, target);
                    p1 = advanceIterator(it1);
                } else if (dominatesOrEqual(p2.get(), offset2, p1.get(), offset1)) { // if p2 dominates p1 (or equal)
                    // iterate over source1 until we find a point that is not dominated by p2 and keep it there for later
                    do {
                        p1 = advanceIterator(it1);
                    } while(p1.isPresent() && dominatesOrEqual(p2.get(), offset2, p1.get(), offset1));
                    // store p2 in the target set and advance it2
                    addWithOffset(p2.get(), offset2, target);
                    p2 = advanceIterator(it2);
                } else { // (points are non-dominated among them)
                    // store in the target the point with lower x, iterate the corresponding iterator
                    if (p1.get()[0]+offset1[0] < p2.get()[0]+offset2[0]) {
                        addWithOffset(p1.get(), offset1, target);
                        p1 = advanceIterator(it1);
                    } else {
                        addWithOffset(p2.get(), offset2, target);
                        p2 = advanceIterator(it2);
                    }
                }
            }
            while (p1.isPresent()) {
                addWithOffset(p1.get(), offset1, target);
                p1 = advanceIterator(it1);
            }
            while (p2.isPresent()) {
                addWithOffset(p2.get(), offset2, target);
                p2 = advanceIterator(it2);
            }
        }

    }

    private static Optional<double[]> advanceIterator(Iterator<double[]> it2) {
        return it2.hasNext() ? Optional.of(it2.next()) : Optional.empty();
    }

    private static boolean dominatesOrEqual(double [] p1, double [] offset1, double [] p2, double [] offset2) {
        return (p1[0]+offset1[0] >= p2[0]+offset2[0]) && (p1[1]+offset1[1] >= p2[1]+offset2[1]);
    }

    private static void moveToTarget(ParetoNonDominatedSet2DEfficient source, double[] offset, ParetoNonDominatedSet2DEfficient target) {
        for (double [] p : source.archive) {
            addWithOffset(p, offset, target);
        }
    }

    private static void addWithOffset(double[] p, double[] offset, ParetoNonDominatedSet2DEfficient target) {
        double [] point = new double[2];
        point[0] = p[0] + offset[0];
        point[1] = p[1] + offset[1];
        target.addPoint(point);
    }


}