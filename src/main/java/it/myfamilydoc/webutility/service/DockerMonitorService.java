package it.myfamilydoc.webutility.service;

import it.myfamilydoc.webutility.dto.ContainerHealthDto;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class DockerMonitorService {

    private static final Logger log = LoggerFactory.getLogger(DockerMonitorService.class);

    private final DockerClient dockerClient;
    private final boolean dockerAvailable;
    private final ExecutorService executorService;

    public DockerMonitorService() {
        DockerClient client = null;
        boolean available = false;
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String dockerHost;
            
            if (os.contains("win")) {
                dockerHost = "tcp://localhost:2375";
            } else {
                dockerHost = "unix:///var/run/docker.sock";
            }
            
            // Configurazione esplicita
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();
            
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();
            
            client = DockerClientImpl.getInstance(config, httpClient);
            
            // Test connessione
            client.pingCmd().exec();
            available = true;
            log.info("Docker client connesso correttamente a: {}", dockerHost);
            
        } catch (Exception e) {
            log.warn("Docker non disponibile. Errore: {}", e.getMessage());
        }
        
        this.dockerClient = client;
        this.dockerAvailable = available;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public boolean isDockerAvailable() {
        return dockerAvailable;
    }

    public List<ContainerHealthDto> getAllContainersHealth() {
        if (!dockerAvailable || dockerClient == null) {
            log.debug("Docker non disponibile, restituisco lista vuota");
            return Collections.emptyList();
        }

        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec();
            
            List<CompletableFuture<ContainerHealthDto>> futures = new ArrayList<>();

            for (Container c : containers) {
                CompletableFuture<ContainerHealthDto> future = CompletableFuture.supplyAsync(() -> {
                    return getContainerHealth(c);
                }, executorService);
                futures.add(future);
            }

            // Wait for all futures to complete (with timeout)
            List<ContainerHealthDto> result = new ArrayList<>();
            for (CompletableFuture<ContainerHealthDto> future : futures) {
                try {
                    ContainerHealthDto dto = future.get(5, TimeUnit.SECONDS);
                    if (dto != null) {
                        result.add(dto);
                    }
                } catch (TimeoutException e) {
                    log.warn("Timeout getting stats for a container");
                } catch (Exception e) {
                    log.warn("Error getting stats: {}", e.getMessage());
                }
            }

            return result;
            
        } catch (Exception e) {
            log.error("Errore durante il recupero dei container: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private ContainerHealthDto getContainerHealth(Container c) {
        String containerName = c.getNames()[0].replace("/", "");
        String status = c.getStatus();
        int restartCount = 0;

        try {
            InspectContainerResponse inspect = dockerClient
                    .inspectContainerCmd(c.getId())
                    .exec();
            restartCount = inspect.getRestartCount();
        } catch (Exception e) {
            log.warn("Impossibile leggere restartCount per container {}", containerName);
        }

        // Get statistics
        Statistics stats = getContainerStats(c.getId());
        
        double cpuPercent = 0.0;
        long memUsage = 0L;
        long memLimit = 0L;
        String netIO = "N/A";
        String blockIO = "N/A";

        if (stats != null) {
            try {
                // Calculate CPU percentage
                cpuPercent = calculateCpuPercent(stats);
                
                // Memory usage
                if (stats.getMemoryStats() != null && stats.getMemoryStats().getUsage() != null) {
                    memUsage = stats.getMemoryStats().getUsage();
                }
                
                // Memory limit
                if (stats.getMemoryStats() != null && stats.getMemoryStats().getLimit() != null) {
                    memLimit = stats.getMemoryStats().getLimit();
                }
                
                // Network I/O
                netIO = formatNetworkIO(stats);
                
                // Block I/O
                blockIO = formatBlockIO(stats);
                
                log.debug("Stats for {}: CPU={}%, Mem={} MB", 
                    containerName, 
                    String.format("%.2f", cpuPercent),
                    memUsage / (1024 * 1024)
                );
                
            } catch (Exception e) {
                log.warn("Error parsing stats for {}: {}", containerName, e.getMessage());
            }
        }

        return new ContainerHealthDto(
                containerName,
                c.getImage(),
                status,
                cpuPercent,
                memUsage,
                memLimit,
                netIO,
                blockIO,
                restartCount
        );
    }

    /**
     * Get container statistics (single snapshot)
     */
    private Statistics getContainerStats(String containerId) {
        try {
            StatsCmd statsCmd = dockerClient.statsCmd(containerId)
                    .withNoStream(true);  // Get single snapshot, not streaming
            
            StatisticsResultCallback callback = new StatisticsResultCallback();
            statsCmd.exec(callback);
            
            // Wait for the single stats result with timeout
            Statistics stats = callback.awaitStats(3, TimeUnit.SECONDS);
            return stats;
            
        } catch (Exception e) {
            log.warn("Error getting stats for container {}: {}", containerId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate CPU percentage from statistics
     */
    private double calculateCpuPercent(Statistics stats) {
        if (stats == null || stats.getCpuStats() == null || stats.getPreCpuStats() == null) {
            return 0.0;
        }

        Long cpuDelta = stats.getCpuStats().getCpuUsage().getTotalUsage() - 
                        stats.getPreCpuStats().getCpuUsage().getTotalUsage();
        
        Long systemDelta = stats.getCpuStats().getSystemCpuUsage() - 
                           stats.getPreCpuStats().getSystemCpuUsage();

        if (systemDelta == null || systemDelta == 0 || cpuDelta == null) {
            return 0.0;
        }

        // Number of CPUs
        Long onlineCpusLong = stats.getCpuStats().getOnlineCpus();
        int onlineCpus;
        if (onlineCpusLong == null || onlineCpusLong == 0) {
            onlineCpus = Runtime.getRuntime().availableProcessors();
        } else {
            onlineCpus = onlineCpusLong.intValue();
        }

        double cpuPercent = (cpuDelta.doubleValue() / systemDelta.doubleValue()) * onlineCpus * 100.0;
        
        // Round to 2 decimal places
        return Math.round(cpuPercent * 100.0) / 100.0;
    }

    /**
     * Format network I/O (RX / TX)
     */
    private String formatNetworkIO(Statistics stats) {
        if (stats.getNetworks() == null || stats.getNetworks().isEmpty()) {
            return "N/A";
        }

        long totalRx = 0;
        long totalTx = 0;

        for (Map.Entry<String, com.github.dockerjava.api.model.StatisticNetworksConfig> entry : 
             stats.getNetworks().entrySet()) {
            
            com.github.dockerjava.api.model.StatisticNetworksConfig network = entry.getValue();
            if (network.getRxBytes() != null) {
                totalRx += network.getRxBytes();
            }
            if (network.getTxBytes() != null) {
                totalTx += network.getTxBytes();
            }
        }

        return formatBytes(totalRx) + " / " + formatBytes(totalTx);
    }

    /**
     * Format block I/O (Read / Write)
     */
    private String formatBlockIO(Statistics stats) {
        if (stats.getBlkioStats() == null || 
            stats.getBlkioStats().getIoServiceBytesRecursive() == null) {
            return "N/A";
        }

        long totalRead = 0;
        long totalWrite = 0;

        for (com.github.dockerjava.api.model.BlkioStatEntry entry : 
             stats.getBlkioStats().getIoServiceBytesRecursive()) {
            
            if ("Read".equalsIgnoreCase(entry.getOp())) {
                totalRead += entry.getValue();
            } else if ("Write".equalsIgnoreCase(entry.getOp())) {
                totalWrite += entry.getValue();
            }
        }

        return formatBytes(totalRead) + " / " + formatBytes(totalWrite);
    }

    /**
     * Format bytes to human readable (KB, MB, GB)
     */
    private String formatBytes(long bytes) {
        if (bytes == 0) return "0B";
        
        String[] units = {"B", "kB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        if (unitIndex == 0) {
            return String.format("%dB", (long) size);
        } else {
            return String.format("%.1f%s", size, units[unitIndex]);
        }
    }

    /**
     * Callback to capture statistics
     */
    private static class StatisticsResultCallback extends InvocationBuilder.AsyncResultCallback<Statistics> {
        private Statistics statistics;
        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void onNext(Statistics stats) {
            this.statistics = stats;
            latch.countDown();
        }

        public Statistics awaitStats(long timeout, TimeUnit unit) throws InterruptedException {
            latch.await(timeout, unit);
            return statistics;
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }
}