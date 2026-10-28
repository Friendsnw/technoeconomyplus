package com.techno.economy.license;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.api.LicensablePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseManager {

    private static final String LICENSE_API_BASE = "https://plugins.teknodev.qzz.io";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern CHALLENGE_PATTERN = Pattern.compile("a=toNumbers\\(\"([0-9a-fA-F]+)\"\\),b=toNumbers\\(\"([0-9a-fA-F]+)\"\\),c=toNumbers\\(\"([0-9a-fA-F]+)\"\\)");

    private final TechnoEconomyPlus plugin;
    private final Map<String, LicensablePlugin> registeredPlugins = new HashMap<>();

    private String cachedPackage = "large";
    private boolean packageEnforcementEnabled = false;
    private boolean cachedValid = false;
    private long cacheTimestamp = 0;
    private String lastWebsiteUrl = null;
    private String cachedCookie = null;

    private String allowedIp = null;
    private String expiresAt = null;
    private int daysRemaining = -1;
    private String lastErrorMessage = null;

    private String latestVersion = null;
    private String latestDownloadUrl = null;
    private boolean latestForceDelete = false;

    public LicenseManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void registerPlugin(LicensablePlugin licensable) {
        registeredPlugins.put(licensable.getPluginId(), licensable);
    }

    public Map<String, LicensablePlugin> getRegisteredPlugins() {
        return registeredPlugins;
    }

    public boolean isTurkish() {
        return false;
    }

    public boolean validateLicense() {
        boolean tr = isTurkish();
        FileConfiguration config = plugin.getConfig();
        String key = config.getString("license.key", "");

        long now = System.currentTimeMillis();
        long twentyFourHours = 24L * 60 * 60 * 1000;
        if (cachedValid && cacheTimestamp > 0 && (now - cacheTimestamp) < twentyFourHours) {
            return true;
        }

        if (key == null || key.isBlank()) {
            cachedValid = false;
            cacheTimestamp = now;
            lastErrorMessage = LicenseMessages.getEmptyKeyMessage(tr);
            sendFailureBlock(key, lastErrorMessage);
            return false;
        }

        try {
            plugin.getLogger().info(LicenseMessages.getConnectingLog(tr, LICENSE_API_BASE));

            String serverId = getOrCreateServerId();
            String serverName = plugin.getServer().getName();
            String serverIp = getServerIp();

            String json = String.format(
                    "{\"license_key\":\"%s\",\"plugin\":\"%s\",\"server_id\":\"%s\",\"server_name\":\"%s\",\"server_ip\":\"%s\"}",
                    escape(key), escape("technoEconomyPlus"), escape(serverId), escape(serverName), escape(serverIp)
            );

            String targetUrl = LICENSE_API_BASE + "/license-info.php?plugin_request=true";

            if (cachedCookie == null) {
                plugin.getLogger().info(LicenseMessages.getChallengeLog(tr));
                fetchAndSolveChallenge(targetUrl);
            }

            ApiResponse resp = executePost(targetUrl, json);

            if (resp.body.contains("toNumbers(")) {
                plugin.getLogger().info(LicenseMessages.getChallengeRefreshLog(tr));
                String solved = solveChallenge(resp.body);
                if (solved != null) {
                    cachedCookie = "__test=" + solved;
                    resp = executePost(targetUrl, json);
                }
            }

            if (resp.statusCode == 404) {
                cachedValid = false;
                cacheTimestamp = now;
                lastErrorMessage = LicenseMessages.getHttp404Message(tr);
                sendFailureBlock(key, lastErrorMessage);
                return false;
            }

            if (resp.statusCode == 403) {
                cachedValid = false;
                cacheTimestamp = now;
                lastErrorMessage = LicenseMessages.getHttp403Message(tr);
                sendFailureBlock(key, lastErrorMessage);
                return false;
            }

            if (resp.statusCode >= 500) {
                cachedValid = false;
                cacheTimestamp = now;
                lastErrorMessage = LicenseMessages.getHttp500Message(tr);
                sendFailureBlock(key, lastErrorMessage);
                return false;
            }

            String body = resp.body;
            lastWebsiteUrl = extractJsonString(body, "website");
            boolean valid = body.contains("\"valid\":true") || body.contains("\"authorized\":true");
            String pkg = extractJsonString(body, "package");
            if (pkg == null) pkg = extractJsonString(body, "tier");

            allowedIp = extractJsonString(body, "allowed_ip");
            if (allowedIp == null) allowedIp = extractJsonString(body, "bound_node");

            expiresAt = extractJsonString(body, "expires_at");
            if (expiresAt == null) expiresAt = extractJsonString(body, "sunset_date");

            daysRemaining = extractJsonInt(body, "days_remaining", -1);

            if (body.contains("\"latest_version\"")) {
                int lvIdx = body.indexOf("\"latest_version\"");
                if (lvIdx != -1) {
                    String lvSub = body.substring(lvIdx);
                    latestVersion = extractJsonString(lvSub, "version");
                    latestDownloadUrl = extractJsonString(lvSub, "download_url");
                    latestForceDelete = lvSub.contains("\"force_delete\":true") || lvSub.contains("\"force_delete\":1");
                }
            }

            cachedValid = valid;
            cachedPackage = (pkg != null && !pkg.isBlank()) ? pkg : config.getString("package.default", "starter");
            cacheTimestamp = now;

            if (!valid) {
                String reason = extractJsonString(body, "reason");
                lastErrorMessage = resolveReasonMessage(tr, reason, resp.statusCode);
                sendFailureBlock(key, lastErrorMessage);
            } else {
                lastErrorMessage = null;
                plugin.getLogger().info(LicenseMessages.getSuccessLog(tr, cachedPackage));
            }

            return valid;

        } catch (UnknownHostException | ConnectException | NoRouteToHostException | SocketTimeoutException e) {
            cachedValid = false;
            cacheTimestamp = now;
            lastErrorMessage = LicenseMessages.getNoInternetMessage(tr);
            sendFailureBlock(key, lastErrorMessage);
            return false;
        } catch (Exception e) {
            cachedValid = false;
            cacheTimestamp = now;
            lastErrorMessage = e.getMessage();
            sendFailureBlock(key, lastErrorMessage);
            return false;
        }
    }

    private String resolveReasonMessage(boolean tr, String reason, int status) {
        if (reason == null) {
            if (status == 403) return LicenseMessages.getHttp403Message(tr);
            if (status == 404) return LicenseMessages.getHttp404Message(tr);
            if (status >= 500) return LicenseMessages.getHttp500Message(tr);
            return LicenseMessages.getInvalidKeyMessage(tr);
        }

        switch (reason.toLowerCase()) {
            case "expired":
            case "sunset_exceeded":
                return LicenseMessages.getExpiredKeyMessage(tr);
            case "ip_mismatch":
            case "node_unauthorized":
                return LicenseMessages.getIpMismatchMessage(tr);
            case "max_servers_exceeded":
            case "cluster_limit_exceeded":
                return LicenseMessages.getMaxServersMessage(tr);
            case "plugin_not_licensed":
            case "module_not_found":
                return LicenseMessages.getPluginNotLicensedMessage(tr);
            case "inactive":
            case "revoked":
                return LicenseMessages.getInactiveMessage(tr);
            case "server_error":
                return LicenseMessages.getHttp500Message(tr);
            case "invalid_key":
            case "not_found":
            case "checksum_mismatch":
            default:
                return LicenseMessages.getInvalidKeyMessage(tr);
        }
    }

    private void fetchAndSolveChallenge(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return;

            String content;
            try (Scanner s = new Scanner(is, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                while (s.hasNextLine()) sb.append(s.nextLine());
                content = sb.toString();
            }

            String solved = solveChallenge(content);
            if (solved != null) {
                cachedCookie = "__test=" + solved;
            }
        } catch (Exception ignored) {
        }
    }

    private ApiResponse executePost(String targetUrl, String json) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (cachedCookie != null) {
            conn.setRequestProperty("Cookie", cachedCookie);
        }
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return new ApiResponse(status, "");

        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) sb.append(scanner.nextLine());
            return new ApiResponse(status, sb.toString().trim());
        }
    }

    private String solveChallenge(String html) {
        if (html == null) return null;
        Matcher m = CHALLENGE_PATTERN.matcher(html);
        if (!m.find()) return null;
        try {
            byte[] key = hexToBytes(m.group(1));
            byte[] iv = hexToBytes(m.group(2));
            byte[] cipher = hexToBytes(m.group(3));

            Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            aes.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plain = aes.doFinal(cipher);
            return bytesToHex(plain);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void sendFailureBlock(String currentKey) {
        sendFailureBlock(currentKey, lastErrorMessage);
    }

    public void sendFailureBlock(String currentKey, String reason) {
        boolean tr = isTurkish();
        String displayKey = (currentKey == null || currentKey.isBlank())
                ? LicenseMessages.getNoKeyDisplay(tr)
                : currentKey;

        var console = plugin.getServer().getConsoleSender();
        console.sendMessage("§c============================");
        console.sendMessage("§c" + LicenseMessages.getBoxTitle(tr));
        if (reason != null && !reason.isBlank()) {
            console.sendMessage("§c" + reason);
        }
        console.sendMessage("§c");
        if (lastWebsiteUrl != null && !lastWebsiteUrl.isBlank()
                && (lastWebsiteUrl.startsWith("http://") || lastWebsiteUrl.startsWith("https://"))) {
            console.sendMessage("§c" + LicenseMessages.getWebsiteLabel(tr) + lastWebsiteUrl);
        }
        console.sendMessage("§c" + LicenseMessages.getCurrentKeyLabel(tr) + displayKey);
        console.sendMessage("§c" + (tr ? "Sunucu adresi: " : "Server address: ") + getServerIp());
        console.sendMessage("§c============================");
    }

    public boolean hasPackageFeature(String feature) {
        if (!packageEnforcementEnabled) return true;
        if (cachedPackage == null) return false;
        List<String> features = plugin.getConfig().getStringList("packages." + cachedPackage + ".allowed-features");
        return features.contains("*") || features.contains(feature);
    }

    public boolean hasPackageCommand(String command) {
        if (!packageEnforcementEnabled) return true;
        if (cachedPackage == null) return false;
        List<String> commands = plugin.getConfig().getStringList("packages." + cachedPackage + ".allowed-commands");
        return commands.contains("*") || commands.contains(command);
    }

    public boolean isPackageEnforcementEnabled() {
        return packageEnforcementEnabled;
    }

    public void setPackageEnforcementEnabled(boolean enabled) {
        this.packageEnforcementEnabled = enabled;
    }

    public String getCachedPackage() {
        return cachedPackage;
    }

    public String getPackageTier() {
        return cachedPackage;
    }

    public boolean isCachedValid() {
        return cachedValid;
    }

    public String getAllowedIp() {
        return allowedIp;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public boolean isUpdateAvailable() {
        if (latestVersion == null || latestVersion.isBlank()) return false;
        return isNewerVersion(latestVersion, plugin.getDescription().getVersion());
    }

    public static boolean isNewerVersion(String latest, String current) {
        if (latest == null || current == null) return false;
        String[] lParts = latest.trim().split("[.-]");
        String[] cParts = current.trim().split("[.-]");
        int len = Math.max(lParts.length, cParts.length);
        for (int i = 0; i < len; i++) {
            int lNum = 0;
            int cNum = 0;
            if (i < lParts.length) {
                try { lNum = Integer.parseInt(lParts[i].replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }
            if (i < cParts.length) {
                try { cNum = Integer.parseInt(cParts[i].replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }
            if (lNum > cNum) return true;
            if (lNum < cNum) return false;
        }
        return false;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }

    public boolean isLatestForceDelete() {
        return latestForceDelete;
    }

    public String getUpdateNoticeMessage(boolean tr) {
        String current = plugin.getDescription().getVersion();
        String link = (latestDownloadUrl != null && !latestDownloadUrl.isBlank()) ? latestDownloadUrl : "https://plugins.teknodev.qzz.io";
        String delNoticeTr = latestForceDelete ? "zorundasınız." : "zorunda değilsiniz.";
        String delNoticeEn = latestForceDelete ? "mandatory." : "not required.";

        if (tr) {
            return "§6TechnoEconomyPlus, yeni sürümü çıkmıştır. Güncellemek için: §e" + link + " §6En güncel versiyon: §f" + latestVersion + "§6, sizin versiyonunuz: §f" + current + " §e(Bu mesaj sadece operatör yetkisi olan oyunculara gönderilmektedir. Config.yml üzerinden kapatabilirsiniz. Güncellemeleri almak isteyebilirsiniz, her güncelleme bazı açıkları kapatır.) Bu güncelleme sonucunda, eklenti dosyasını silmek " + delNoticeTr;
        } else {
            return "§6TechnoEconomyPlus, a new version is available! To update: §e" + link + " §6Latest version: §f" + latestVersion + "§6, your version: §f" + current + " §e(This notice is only sent to players with operator permissions. You can disable this in config.yml. Updates patch vulnerabilities.) After this update, deleting the old plugin file is " + delNoticeEn;
        }
    }

    private String getOrCreateServerId() {
        FileConfiguration config = plugin.getConfig();
        String id = config.getString("server-id", null);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            config.set("server-id", id);
            plugin.saveConfig();
        }
        return id;
    }

    public String getServerIp() {
        String[] services = new String[]{
                "https://api.ipify.org",
                "https://checkip.amazonaws.com",
                "https://icanhazip.com"
        };
        for (String svc : services) {
            try {
                URL url = new URL(svc);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    if (s.hasNext()) {
                        String ip = s.next().trim();
                        if (!ip.isEmpty() && ip.length() <= 45) {
                            return ip;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractJsonString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private int extractJsonInt(String json, String key, int def) {
        if (json == null) return def;
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return def;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (start == end) return def;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) {
            return def;
        }
    }

    private static class ApiResponse {
        final int statusCode;
        final String body;

        ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body != null ? body : "";
        }
    }
}
