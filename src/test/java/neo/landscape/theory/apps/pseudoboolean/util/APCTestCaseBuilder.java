package neo.landscape.theory.apps.pseudoboolean.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class APCTestCaseBuilder {
    
    private static class CycleBuilder extends APCTestCaseBuilder {
        int n;
        private CycleBuilder(int n) {
            this.n = n;
        }
        @Override
        public APCTestCase build() {
            return new APCTestCase () {
                {
                    graph = new Integer [n][2];
                    articulationPoints = new Integer[0];
                    biconnectedComponents = new Integer[1][n];
                    for (int i=0; i < n; i++) {
                        graph[i][0] = ((i+n-1) % n);
                        graph[i][1] = ((i+1)%n);
                        biconnectedComponents[0][i]=i;
                    }
                }
            };
        }
    }
    
    private static class PointBuilder extends APCTestCaseBuilder {
        @Override
        public APCTestCase build() {
            return new APCTestCase () {
                {
                    graph = new Integer [1][0];
                    articulationPoints = new Integer[0];
                    biconnectedComponents = new Integer[0][0];
                }
            };
        }
    }
    
    private static class LineBuilder extends APCTestCaseBuilder {
        private int n;
        private LineBuilder(int n) {
            assert n>=2;
            this.n=n;
        }
        
        @Override
        public APCTestCase build() {
            return new APCTestCase () {
                {
                    graph = new Integer [n][];
                    articulationPoints = new Integer[n-2];
                    biconnectedComponents = new Integer[n-1][2];
                    int k=0;
                    for (int i=0; i < n; i++) {
                        if (i==0) {
                            graph[i] = new Integer[1];
                            graph[i][0] = 1;
                        } else if (i==n-1) {
                            graph[i] = new Integer[1];
                            graph[i][0] = n-2;
                        } else {
                            graph[i] = new Integer[2];
                            graph[i][0] = i-1;
                            graph[i][1] = i+1;
                            articulationPoints[k++]=i;
                        }
                        
                        if (i < n-1) {
                            biconnectedComponents[i][0]=i;
                            biconnectedComponents[i][1]=i+1;
                        }
                    }
                }
            };
        }
    }
    
    private static class RepeatBuilder extends APCTestCaseBuilder {
        private int n;
        private APCTestCaseBuilder builder;
        
        private RepeatBuilder (int n, APCTestCaseBuilder builder){
            this.n=n;
            this.builder=builder;
        }
        
        @Override
        public APCTestCase build() {
            return new APCTestCase () {
                {
                    APCTestCase previous = builder.build();
                    int nodes = previous.graph.length;
                    int aps = previous.articulationPoints.length;
                    int bcs = previous.biconnectedComponents.length;
                    
                    graph = new Integer[nodes*n][];
                    articulationPoints = new Integer[aps*n];
                    biconnectedComponents = new Integer[bcs*n][];
                    
                    for (int i=0; i < n; i++) {
                        for (int j=0; j < nodes; j++) {
                            graph[i*nodes+j] = new Integer[previous.graph[j].length];
                            for (int k=0; k < previous.graph[j].length; k++) {
                                graph[i*nodes+j][k] = previous.graph[j][k] + i*nodes;
                            }
                        }
                        
                        for (int j=0; j < aps; j++) {
                            articulationPoints[i*aps+j] = previous.articulationPoints[j] + i*nodes;
                        }
                        
                        for (int j=0; j < bcs; j++) {
                            biconnectedComponents[i*bcs+j] = new Integer[previous.biconnectedComponents[j].length];
                            for (int k=0; k < previous.biconnectedComponents[j].length; k++) {
                                biconnectedComponents[i*bcs+j][k] = previous.biconnectedComponents[j][k] + i*nodes;
                            }
                        }
                    }
                }
            };
        }
    }
    
    private class JoinBuilder extends APCTestCaseBuilder {
        private APCTestCaseBuilder builders [];
        
        private JoinBuilder(APCTestCaseBuilder ... builders) {
            this.builders = builders;
        }

        @Override
        public APCTestCase build() {
            List<APCTestCase> previous = Arrays.stream(builders)
                    .map(APCTestCaseBuilder::build)
                    .collect(Collectors.toList());
            
            return new APCTestCase () {
                {
                    int n = previous.stream().mapToInt(tc->tc.graph.length).sum();
                    int aps = previous.stream().mapToInt(tc->tc.articulationPoints.length).sum();
                    int bcs = previous.stream().mapToInt(tc->tc.biconnectedComponents.length).sum();
                    
                    graph = new Integer[n][];
                    articulationPoints = new Integer[aps];
                    biconnectedComponents = new Integer[bcs][];
                    
                    int node=0;
                    int articulationPoint = 0;
                    int biconnectedComponent = 0;
                    
                    for (APCTestCase tc: previous) {
                        int startNode = node;
                        for (int i=0; i < tc.graph.length; i++) {
                            graph[node] = new Integer[tc.graph[i].length];
                            for (int j=0; j < tc.graph[i].length; j++) {
                                graph[node][j] = tc.graph[i][j] + startNode;
                            }
                            node++;
                        }
                        
                        for (int i=0; i < tc.articulationPoints.length; i++) {
                            articulationPoints[articulationPoint++] = tc.articulationPoints[i] + startNode;
                        }
                        
                        for (int i=0; i < tc.biconnectedComponents.length; i++) {
                            biconnectedComponents[biconnectedComponent] = new Integer[tc.biconnectedComponents[i].length];
                            for (int j=0; j < tc.biconnectedComponents[i].length; j++) {
                                biconnectedComponents[biconnectedComponent][j] = tc.biconnectedComponents[i][j] + startNode;
                            }
                            biconnectedComponent++;        
                        }
                    }
                }
            };
        }
        
    }
    
    public static APCTestCaseBuilder cycleGraph(int n) {
        return new CycleBuilder(n);
    }
    
    public static APCTestCaseBuilder single() {
        return new PointBuilder();
    }
    
    public static APCTestCaseBuilder line(int n) {
        if (n >= 2) {
            return new LineBuilder(n);
        } else {
            return new PointBuilder();
        }
    }
    
    public APCTestCaseBuilder repeat(int n) {
        return new RepeatBuilder(n, this);
    }
    
    public APCTestCaseBuilder join(APCTestCaseBuilder ... builders) {
        APCTestCaseBuilder [] blds = Arrays.copyOf(builders, builders.length+1);
        blds[builders.length] = this;
        return new JoinBuilder(blds);
    }
    
    public abstract APCTestCase build ();

}
