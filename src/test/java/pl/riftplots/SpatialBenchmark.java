package pl.riftplots;

import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.service.plot.PlotSpatialIndex;

import java.util.*;
import java.nio.file.*;

public final class SpatialBenchmark {
    public static void main(String[] args) throws Exception {
        List<String> results = new ArrayList<>();
        for (int size : new int[]{100, 1000, 10000}) {
            Map<String, PlotBounds> bounds = new LinkedHashMap<>();
            PlotSpatialIndex index = new PlotSpatialIndex();
            for (int i = 0; i < size; i++) {
                var area = new PlotBounds("world", i * 32, 60, 0, i * 32 + 10, 70, 10);
                bounds.put("p" + i, area);
                index.put("p" + i, area);
            }
            int queries = 2000, linearMatches = 0, indexMatches = 0;
            // Sample across the entire city and warm both paths before recording elapsed time.
            for (int warmup = 0; warmup < queries; warmup++) {
                int x = ((warmup * 7919) % size) * 32 + 1;
                for (var area : bounds.values()) if (area.contains("world", x, 65, 1)) break;
                for (String id : index.candidates("world", x, 1)) if (bounds.get(id).contains("world", x, 65, 1)) break;
            }
            long begin = System.nanoTime();
            for (int q = 0; q < queries; q++) {
                int x = ((q * 7919) % size) * 32 + 1;
                for (var area : bounds.values())
                    if (area.contains("world", x, 65, 1)) {
                        linearMatches++;
                        break;
                    }
            }
            long linear = System.nanoTime() - begin;
            begin = System.nanoTime();
            for (int q = 0; q < queries; q++) {
                int x = ((q * 7919) % size) * 32 + 1;
                for (String id : index.candidates("world", x, 1))
                    if (bounds.get(id).contains("world", x, 65, 1)) {
                        indexMatches++;
                        break;
                    }
            }
            long indexed = System.nanoTime() - begin;
            if (linearMatches != queries || indexMatches != queries)
                throw new AssertionError("Spatial lookup mismatch");
            results.add("{\"plots\":" + size + ",\"queries\":" + queries + ",\"linear_ns\":" + linear + ",\"indexed_ns\":" + indexed + "}");
        }
        Path report = Path.of("build/reports/benchmarks/spatial-index.json");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "[" + String.join(",", results) + "]\n");
        System.out.println(report + " " + results);
    }
}
