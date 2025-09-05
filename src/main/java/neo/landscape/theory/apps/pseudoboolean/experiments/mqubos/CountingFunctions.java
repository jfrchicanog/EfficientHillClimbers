package neo.landscape.theory.apps.pseudoboolean.experiments.mqubos;

import neo.landscape.theory.apps.util.Process;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class CountingFunctions implements Process {

    record GroupElement(int mask, int [] permutation) {
        public String toString() {
            return String.format("permutation=%s, mask=%d", Arrays.toString(permutation), mask);
        }
    }

    private int n;
    private List<GroupElement> groupElements;

    private int auxiliaryArrayForpermutationGeneration[];

    @Override
    public String getDescription() {
        return "Counting functions";
    }

    @Override
    public String getID() {
        return "fcount";
    }

    @Override
    public String getInvocationInfo() {
        return String.format("Usage: %s <n>", getID());
    }

    public void permute(int n, Consumer<int []> callback) {
        auxiliaryArrayForpermutationGeneration = IntStream.range(0, n).toArray();
        backtrack(0, callback);
    }

    // Función recursiva para generar permutaciones
    private void backtrack(int start, Consumer<int []> callback) {
        if (start == auxiliaryArrayForpermutationGeneration.length) {
            callback.accept(auxiliaryArrayForpermutationGeneration);
            return;
        }

        for (int i = start; i < auxiliaryArrayForpermutationGeneration.length; i++) {
            swap(start, i);             // intercambiamos elementos
            backtrack(start + 1, callback); // llamamos recursivamente
            swap(start, i);             // deshacemos el swap (backtracking)
        }
    }

    // Función auxiliar para intercambiar elementos
    private void swap(int i, int j) {
        int tmp = auxiliaryArrayForpermutationGeneration[i];
        auxiliaryArrayForpermutationGeneration[i] = auxiliaryArrayForpermutationGeneration[j];
        auxiliaryArrayForpermutationGeneration[j] = tmp;
    }

    private void createGroupElements(int n) {
        groupElements = new ArrayList<>();
        permute(n, permutation -> {
            IntStream.range(0, 1 <<n).forEach(mask->{
                    groupElements.add(new GroupElement(mask, permutation.clone()));
                });
        });
    }

    private BigInteger fubiniNumber(long val) {
        return FubiniNumbers.fubini((int)val);
    }

    private int applyGroupElement(GroupElement ge, int x) {
        int newSolution = 0;
        for (int i=n-1; i>=0; i--) {
            newSolution <<= 1;
            newSolution |= (x >> ge.permutation[i]) & 0x1;
        }
        return newSolution ^ ge.mask;
    }

    private int cyclesOfGroupElement(GroupElement groupElement) {
        boolean [] marked = new boolean [1<<n];
        int cycles = 0;
        for (int x=0; x < 1<<n; x++) {
            if (!marked[x]) {
                while (!marked[x]) {
                    marked[x] = true;
                    x = applyGroupElement(groupElement, x);
                }
                cycles++;
            }
        }
        return cycles;
    }

    private BigInteger ontoNumber(int n, int m) {
        return FubiniNumbers.ontoNumber(n,m);
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            System.out.println(getInvocationInfo());
            return;
        }
        n = Integer.parseInt(args[0]);

        /*
        permute(n, s->{
            System.out.println(Arrays.toString(s));
        });*/

        createGroupElements(n);
        //System.out.format("Group elements: %d\n", groupElements.size());
        BigInteger fixedFunctions = BigInteger.ZERO;
        BigInteger [] withDifferentValues = new BigInteger[(1<<n)+1];
        IntStream.range(0, 1+ (1<<n)).forEach(i->{
           withDifferentValues[i] = BigInteger.ZERO;
        });
        for (GroupElement groupElement : groupElements) {
            final int cycles = cyclesOfGroupElement(groupElement);
            //System.out.println(String.format("GroupElement: %s, cycles=%d", groupElement,cycles));
            fixedFunctions = fixedFunctions.add(fubiniNumber(cycles));
            IntStream.rangeClosed(1, 1<<n).forEach(i->{
                withDifferentValues[i] = withDifferentValues[i].add(ontoNumber(cycles, i));
            });
        }
        BigInteger classes = fixedFunctions.divide(BigInteger.valueOf(groupElements.size()));
        System.out.println(String.format("Function classes of %d bits: %s", n, classes.toString()));
        IntStream.rangeClosed(1, 1<<n).forEach(i->{
            System.out.println(String.format("Function classes of %d bits with %d values: %s", n, i, withDifferentValues[i].divide(BigInteger.valueOf(groupElements.size()))));
        });

    }
}
