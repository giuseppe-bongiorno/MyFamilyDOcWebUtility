package it.myfamilydoc.webutility.dto;

public class ContainerHealthDto {
    private String name;
    private String image;
    private String status;
    private double cpuPercent;
    private long memoryUsage;
    private long memoryLimit;
    private String netIO;
    private String blockIO;
    private int restartCount;

    public ContainerHealthDto() { }

    public ContainerHealthDto(String name, String image, String status, double cpuPercent,
                              long memoryUsage, long memoryLimit, String netIO, String blockIO,
                              int restartCount) {
        this.name = name;
        this.image = image;
        this.status = status;
        this.cpuPercent = cpuPercent;
        this.memoryUsage = memoryUsage;
        this.memoryLimit = memoryLimit;
        this.netIO = netIO;
        this.blockIO = blockIO;
        this.restartCount = restartCount;
    }

    // --- GETTER & SETTER ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(double cpuPercent) { this.cpuPercent = cpuPercent; }
    public long getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
    public long getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(long memoryLimit) { this.memoryLimit = memoryLimit; }
    public String getNetIO() { return netIO; }
    public void setNetIO(String netIO) { this.netIO = netIO; }
    public String getBlockIO() { return blockIO; }
    public void setBlockIO(String blockIO) { this.blockIO = blockIO; }
    public int getRestartCount() { return restartCount; }
    public void setRestartCount(int restartCount) { this.restartCount = restartCount; }
}
