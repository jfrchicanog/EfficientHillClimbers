package neo.landscape.theory.apps.pseudoboolean.util;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ParetoNonDominatedSet2DEfficient implements Iterable<double []>, IParetoNonDominatedSet<ParetoNonDominatedSet2DEfficient>{

    private static final int INITIAL_CAPACITY = 16;

    private double [] xs;
    private double [] ys;
    private int size;

    public ParetoNonDominatedSet2DEfficient() {
        xs = new double [INITIAL_CAPACITY];
        ys = new double [INITIAL_CAPACITY];
        size = 0;
    }

    private void grow() {
        xs = Arrays.copyOf(xs, xs.length * 2);
        ys = Arrays.copyOf(ys, ys.length * 2);
    }

    private void ensureCapacity(int capacity) {
        if (capacity > xs.length) {
            int newCapacity = xs.length;
            while (newCapacity < capacity) {
                newCapacity *= 2;
            }
            xs = Arrays.copyOf(xs, newCapacity);
            ys = Arrays.copyOf(ys, newCapacity);
        }
    }

    @Override
    public void clear() {
        size=0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public IParetoNonDominatedSetFactory<ParetoNonDominatedSet2DEfficient> getFactory() {
        return new ParetoNonDominatedSet2DEfficientFactory();
    }

    @Override
    @Nonnull
    public Iterator<double []> iterator() {
        return new Iterator<double[]>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < size;
            }

            @Override
            public double[] next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                double[] point = new double[]{xs[currentIndex], ys[currentIndex]};
                currentIndex++;
                return point;
            }
        };
    }

    public Stream<double[]> stream() {
        Iterator<double []> iterator = iterator();
        List<double []> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list.stream();
    }

    public boolean add(double x, double y) {
        // --- 1) búsqueda binaria en xs ---
        int pos = Arrays.binarySearch(xs, 0, size, x);
        if (pos >= 0) {
            // ya existe este x; si el nuevo y es menor o igual, dominado
            if (ys[pos] >= y) return false;
            // si el nuevo domina, lo sustituimos
        } else {
            pos = -pos - 1;
        }

        // --- 2) revisar si está dominado por el punto a la derecha ---
        if (pos < size && ys[pos] >= y) return false;

        // --- 3) eliminar los puntos a la izquierda dominados por el nuevo ---
        int i = pos - 1;
        while (i >= 0 && ys[i] <= y) i--;

        int newPos = i + 1;

        // --- 4) mover datos si es necesario ---
        int shift = pos - newPos;
        if (shift > 0) {
            System.arraycopy(xs, pos, xs, newPos + 1, size - pos);
            System.arraycopy(ys, pos, ys, newPos + 1, size - pos);
        } else {
            System.arraycopy(xs, pos, xs, pos + 1, size - pos);
            System.arraycopy(ys, pos, ys, pos + 1, size - pos);
        }
        // Eliminar dominados: simplemente sobrescribimos
        size -= shift;

        if (size == xs.length) grow();

        // --- 5) insertar ---
        xs[newPos] = x;
        ys[newPos] = y;
        size++;

        return true;
    }


    public void addPoint(double[] point) {
        if (point.length != 2) {
            throw new IllegalArgumentException("Point must be 2-dimensional");
        }
        add(point[0], point[1]);
    }

    // FIXME: probably efficiency can be improved
    public static void convolute(ParetoNonDominatedSet2DEfficient source1, ParetoNonDominatedSet2DEfficient source2, ParetoNonDominatedSet2DEfficient target) {
        target.clear();
        for (int index1 = 0; index1 < source1.size(); index1++) {
            for (int index2 = 0; index2 < source2.size(); index2++) {
                double newX = source1.xs[index1] + source2.xs[index2];
                double newY = source1.ys[index1] + source2.ys[index2];
                target.add(newX, newY);
            }
        }
    }

    public static void combine(ParetoNonDominatedSet2DEfficient source1, double [] offset1, ParetoNonDominatedSet2DEfficient source2, double [] offset2, ParetoNonDominatedSet2DEfficient target) {
        target.clear();
        if (source1.size > 0 && source2.size==0) {
            moveToTarget(source1, offset1, target);
        } else if (source2.size > 0 && source1.size==0) {
            moveToTarget(source2, offset2, target);
        } else if (source1.size > 0) {
            // both sets are non-empty
            int index1 = 0;
            int index2 = 0;

            while (index1 < source1.size && index2 < source2.size) {
                // if p1 dominates or is equal to p2
                if (dominatesOrEqual(source1.xs[index1], source1.ys[index1], offset1, source2.xs[index2], source2.ys[index2], offset2)) {
                    do {
                        index2++;
                    } while(index2 < source2.size && dominatesOrEqual(source1.xs[index1], source1.ys[index1], offset1, source2.xs[index2], source2.ys[index2], offset2));
                    // store p1 in the target set and advance it1
                    addWithOffset(source1.xs[index1], source1.ys[index1], offset1, target);
                    index1++;
                } else if (dominatesOrEqual(source2.xs[index2], source2.ys[index2], offset2, source1.xs[index1], source1.ys[index1], offset1)) { // if p2 dominates p1 (or equal)
                    // iterate over source1 until we find a point that is not dominated by p2 and keep it there for later
                    do {
                        index1++;
                    } while(index1 < source1.size && dominatesOrEqual(source2.xs[index2], source2.ys[index2], offset2, source1.xs[index1], source1.ys[index1], offset1));
                    // store p2 in the target set and advance it2
                    addWithOffset(source2.xs[index2], source2.ys[index2], offset2, target);
                    index2++;
                } else { // (points are non-dominated among them)
                    // store in the target the point with lower x, iterate the corresponding iterator
                    if (source1.xs[index1]+offset1[0] < source2.xs[index2]+offset2[0]) {
                        addWithOffset(source1.xs[index1], source1.ys[index1], offset1, target);
                        index1++;
                    } else {
                        addWithOffset(source2.xs[index2], source2.ys[index2], offset2, target);
                        index2++;
                    }
                }
            }
            while (index1 < source1.size) {
                addWithOffset(source1.xs[index1], source1.ys[index1], offset1, target);
                index1++;
            }
            while (index2 < source2.size) {
                addWithOffset(source2.xs[index2], source2.ys[index2], offset2, target);
                index2++;
            }
        }

    }

    private static Optional<double[]> advanceIterator(Iterator<double[]> it2) {
        return it2.hasNext() ? Optional.of(it2.next()) : Optional.empty();
    }

    private static boolean dominatesOrEqual(double [] p1, double [] offset1, double [] p2, double [] offset2) {
        return (p1[0]+offset1[0] >= p2[0]+offset2[0]) && (p1[1]+offset1[1] >= p2[1]+offset2[1]);
    }

    private static boolean dominatesOrEqual(double p1x, double p1y, double [] offset1, double p2x, double p2y, double [] offset2) {
        return (p1x+offset1[0] >= p2x+offset2[0]) && (p1y+offset1[1] >= p2y+offset2[1]);
    }

    private static void moveToTarget(ParetoNonDominatedSet2DEfficient source, double[] offset, ParetoNonDominatedSet2DEfficient target) {
        target.ensureCapacity(source.size);
        System.arraycopy(source.xs, 0, target.xs, 0, source.size);
        System.arraycopy(source.ys, 0, target.ys, 0, source.size);
        target.size=source.size;
        for (int i=0; i<source.size; i++) {
            target.xs[i] += offset[0];
            target.ys[i] += offset[1];
        }
    }

    private static void addWithOffset(double px, double py, double[] offset, ParetoNonDominatedSet2DEfficient target) {
        target.ensureCapacity(target.size+1);
        target.xs[target.size] = px + offset[0];
        target.ys[target.size] = py + offset[1];
        target.size++;
    }



}