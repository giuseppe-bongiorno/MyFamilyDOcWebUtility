package it.myfamilydoc.webutility.service;

import it.myfamilydoc.webutility.dto.ContainerHealthDto;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DockerMonitorService {

    private static final Logger log = LoggerFactory.getLogger(DockerMonitorService.class);

    private final DockerClient dockerClient;
    private final boolean dockerAvailable;

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
            
            List<ContainerHealthDto> result = new ArrayList<>();

            for (Container c : containers) {
                int restartCount = 0;
                String status = c.getStatus();

                try {
                    InspectContainerResponse inspect = dockerClient
                            .inspectContainerCmd(c.getId())
                            .exec();
                    restartCount = inspect.getRestartCount();
                } catch (Exception e) {
                    log.warn("Impossibile leggere restartCount per container {}", 
                            c.getNames()[0], e);
                }

                double cpuPercent = 0.0;
                long memUsage = 0L;
                long memLimit = 0L;
                String netIO = "N/A";
                String blockIO = "N/A";

                result.add(new ContainerHealthDto(
                        c.getNames()[0].replace("/", ""),
                        c.getImage(),
                        status,
                        cpuPercent,
                        memUsage,
                        memLimit,
                        netIO,
                        blockIO,
                        restartCount
                ));
            }

            return result;
            
        } catch (Exception e) {
            log.error("Errore durante il recupero dei container: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}