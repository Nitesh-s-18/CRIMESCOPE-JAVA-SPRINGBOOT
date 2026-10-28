package com.crimescope.service;

import com.crimescope.model.AreaRanking;
import com.crimescope.model.CrimeRecord;
import com.crimescope.model.DbscanStats;
import com.crimescope.model.HotspotCluster;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HotspotService {

    public static final double EARTH_RADIUS_KM = 6371.0088;

    public static class HotspotResult {
        private final List<CrimeRecord> clusteredRecords;
        private final List<HotspotCluster> clusterSummaries;
        private final List<AreaRanking> areaRankings;
        private final DbscanStats stats;

        public HotspotResult(List<CrimeRecord> clusteredRecords, List<HotspotCluster> clusterSummaries,
                             List<AreaRanking> areaRankings, DbscanStats stats) {
            this.clusteredRecords = clusteredRecords;
            this.clusterSummaries = clusterSummaries;
            this.areaRankings = areaRankings;
            this.stats = stats;
        }

        public List<CrimeRecord> getClusteredRecords() { return clusteredRecords; }
        public List<HotspotCluster> getClusterSummaries() { return clusterSummaries; }
        public List<AreaRanking> getAreaRankings() { return areaRankings; }
        public DbscanStats getStats() { return stats; }
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public HotspotResult runDbscanHotspots(List<CrimeRecord> records, double epsKm, int minSamples) {
        if (records == null || records.size() < minSamples) {
            DbscanStats emptyStats = new DbscanStats(0, 0, records != null ? records.size() : 0, 100.0, epsKm, minSamples);
            return new HotspotResult(records != null ? records : Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), emptyStats);
        }

        int n = records.size();
        int[] labels = new int[n];
        Arrays.fill(labels, -1); // -1 = noise

        boolean[] visited = new boolean[n];

        // Precompute neighbors for core points
        List<List<Integer>> neighborsList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            neighborsList.add(new ArrayList<>());
        }

        for (int i = 0; i < n; i++) {
            CrimeRecord r1 = records.get(i);
            for (int j = i + 1; j < n; j++) {
                CrimeRecord r2 = records.get(j);
                double dist = haversineDistance(r1.getLatitude(), r1.getLongitude(), r2.getLatitude(), r2.getLongitude());
                if (dist <= epsKm) {
                    neighborsList.get(i).add(j);
                    neighborsList.get(j).add(i);
                }
            }
            // self neighbor
            neighborsList.get(i).add(i);
        }

        int clusterId = 0;
        for (int i = 0; i < n; i++) {
            if (visited[i]) continue;
            visited[i] = true;

            List<Integer> neighbors = neighborsList.get(i);
            if (neighbors.size() < minSamples) {
                labels[i] = -1; // Noise
            } else {
                labels[i] = clusterId;
                Deque<Integer> queue = new ArrayDeque<>(neighbors);
                Set<Integer> inQueue = new HashSet<>(neighbors);

                while (!queue.isEmpty()) {
                    int pt = queue.poll();
                    if (!visited[pt]) {
                        visited[pt] = true;
                        List<Integer> ptNeighbors = neighborsList.get(pt);
                        if (ptNeighbors.size() >= minSamples) {
                            for (int nb : ptNeighbors) {
                                if (inQueue.add(nb)) {
                                    queue.add(nb);
                                }
                            }
                        }
                    }
                    if (labels[pt] == -1) {
                        labels[pt] = clusterId;
                    }
                }
                clusterId++;
            }
        }

        int hotspotCrimes = 0;
        int noiseCrimes = 0;
        for (int i = 0; i < n; i++) {
            records.get(i).setClusterId(labels[i]);
            records.get(i).setHotspot(labels[i] != -1);
            if (labels[i] != -1) {
                hotspotCrimes++;
            } else {
                noiseCrimes++;
            }
        }

        double noisePct = n > 0 ? Math.round((noiseCrimes * 100.0 / n) * 100.0) / 100.0 : 0.0;
        DbscanStats stats = new DbscanStats(clusterId, hotspotCrimes, noiseCrimes, noisePct, epsKm, minSamples);

        // Build cluster summaries
        List<HotspotCluster> clusterSummaries = new ArrayList<>();
        Map<Integer, List<CrimeRecord>> byCluster = records.stream()
                .filter(r -> r.getClusterId() != -1)
                .collect(Collectors.groupingBy(CrimeRecord::getClusterId));

        for (Map.Entry<Integer, List<CrimeRecord>> entry : byCluster.entrySet()) {
            int cid = entry.getKey();
            List<CrimeRecord> subset = entry.getValue();
            int count = subset.size();
            double pct = Math.round((count * 100.0 / n) * 100.0) / 100.0;

            double avgLat = Math.round(subset.stream().mapToDouble(CrimeRecord::getLatitude).average().orElse(0.0) * 100000.0) / 100000.0;
            double avgLon = Math.round(subset.stream().mapToDouble(CrimeRecord::getLongitude).average().orElse(0.0) * 100000.0) / 100000.0;

            String topArea = modeOf(subset.stream().map(CrimeRecord::getArea).toList());
            String topCrime = modeOf(subset.stream().map(CrimeRecord::getCrimeType).toList());
            String topLoc = modeOf(subset.stream().map(CrimeRecord::getLocationType).toList());

            clusterSummaries.add(new HotspotCluster(
                    "Hotspot #" + (cid + 1), cid, count, pct, topArea, topCrime, topLoc, avgLat, avgLon
            ));
        }

        clusterSummaries.sort((a, b) -> Integer.compare(b.getTotalIncidents(), a.getTotalIncidents()));

        // Area Rankings
        Map<String, List<CrimeRecord>> byArea = records.stream()
                .collect(Collectors.groupingBy(CrimeRecord::getArea));

        List<AreaRanking> areaRankings = new ArrayList<>();
        for (Map.Entry<String, List<CrimeRecord>> entry : byArea.entrySet()) {
            String areaName = entry.getKey();
            List<CrimeRecord> subset = entry.getValue();
            int total = subset.size();
            double pct = Math.round((total * 100.0 / n) * 100.0) / 100.0;
            String topCrime = modeOf(subset.stream().map(CrimeRecord::getCrimeType).toList());
            double avgAge = Math.round(subset.stream().mapToInt(CrimeRecord::getVictimAge).average().orElse(35.0) * 10.0) / 10.0;
            double lat = subset.stream().mapToDouble(CrimeRecord::getLatitude).average().orElse(0.0);
            double lon = subset.stream().mapToDouble(CrimeRecord::getLongitude).average().orElse(0.0);

            areaRankings.add(new AreaRanking(0, areaName, total, pct, topCrime, avgAge, lat, lon));
        }

        areaRankings.sort((a, b) -> Integer.compare(b.getTotalCrimes(), a.getTotalCrimes()));
        for (int i = 0; i < areaRankings.size(); i++) {
            areaRankings.get(i).setRank(i + 1);
        }

        return new HotspotResult(records, clusterSummaries, areaRankings, stats);
    }

    private static String modeOf(List<String> list) {
        if (list == null || list.isEmpty()) return "N/A";
        Map<String, Long> freq = list.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return freq.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");
    }
}
