package it.myfamilydoc.webutility.dto;

/**
 * DTO per le statistiche principali della dashboard admin.
 * Mappa il tipo DashboardStats del frontend.
 */
public class DashboardStatsDto {

    private UserStatsSection users;
    private DocumentStatsSection documents;
    private MessageStatsSection messages;
    private NotificationStatsSection notifications;
    private SystemStatsSection system;

    public DashboardStatsDto() {
        this.users = new UserStatsSection();
        this.documents = new DocumentStatsSection();
        this.messages = new MessageStatsSection();
        this.notifications = new NotificationStatsSection();
        this.system = new SystemStatsSection();
    }

    // ── Inner classes ────────────────────────────────────────────

    public static class UserStatsSection {
        private long total;
        private double trend; // % vs mese precedente

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public double getTrend() { return trend; }
        public void setTrend(double trend) { this.trend = trend; }
    }

    public static class DocumentStatsSection {
        private long total;
        private double trend;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public double getTrend() { return trend; }
        public void setTrend(double trend) { this.trend = trend; }
    }

    public static class MessageStatsSection {
        private long total;
        private double trend;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public double getTrend() { return trend; }
        public void setTrend(double trend) { this.trend = trend; }
    }

    public static class NotificationStatsSection {
        private long sent;
        private double trend;

        public long getSent() { return sent; }
        public void setSent(long sent) { this.sent = sent; }
        public double getTrend() { return trend; }
        public void setTrend(double trend) { this.trend = trend; }
    }

    public static class SystemStatsSection {
        private double uptime;
        private long activeDevices;
        private double storageUsed;
        private double storageTotal;
        private long apiCalls24h;

        public double getUptime() { return uptime; }
        public void setUptime(double uptime) { this.uptime = uptime; }
        public long getActiveDevices() { return activeDevices; }
        public void setActiveDevices(long activeDevices) { this.activeDevices = activeDevices; }
        public double getStorageUsed() { return storageUsed; }
        public void setStorageUsed(double storageUsed) { this.storageUsed = storageUsed; }
        public double getStorageTotal() { return storageTotal; }
        public void setStorageTotal(double storageTotal) { this.storageTotal = storageTotal; }
        public long getApiCalls24h() { return apiCalls24h; }
        public void setApiCalls24h(long apiCalls24h) { this.apiCalls24h = apiCalls24h; }
    }

    // ── Getters & Setters ────────────────────────────────────────

    public UserStatsSection getUsers() { return users; }
    public void setUsers(UserStatsSection users) { this.users = users; }

    public DocumentStatsSection getDocuments() { return documents; }
    public void setDocuments(DocumentStatsSection documents) { this.documents = documents; }

    public MessageStatsSection getMessages() { return messages; }
    public void setMessages(MessageStatsSection messages) { this.messages = messages; }

    public NotificationStatsSection getNotifications() { return notifications; }
    public void setNotifications(NotificationStatsSection notifications) { this.notifications = notifications; }

    public SystemStatsSection getSystem() { return system; }
    public void setSystem(SystemStatsSection system) { this.system = system; }
}