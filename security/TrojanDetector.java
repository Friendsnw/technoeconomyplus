package com.techno.economy.security;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class TrojanDetector {

    private final TechnoEconomyPlus plugin;
    private BukkitTask task;
    private String ownHash;

    public TrojanDetector(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        ownHash = computeOwnHash();
        long interval = 20L * 60 * 10;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::check, interval, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void check() {
        String current = computeOwnHash();
        if (ownHash != null && current != null && !ownHash.equals(current)) {
            printWarning();
            ownHash = current;
        }
    }

    private String computeOwnHash() {
        try {
            File jar = new File(plugin.getClass()
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jar.exists() || !jar.getName().endsWith(".jar")) return null;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(jar)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) md.update(buf, 0, n);
            }
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public void printWarning() {
        boolean tr = "tr".equalsIgnoreCase(plugin.getConfig().getString("language", "en"));
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        if (tr) {
            c.sendMessage("§c=======================");
            c.sendMessage("§c§lTROJAN TEHLİKESİ BULUNDU.");
            c.sendMessage("§cSunucunuzda yüksek ihtimalle bir trojan virüsü tespit edilmiştir.");
            c.sendMessage("§cTrojan virüsleri, sunucunuzda bulunan .jar uzantılı java dosyalarını");
            c.sendMessage("§cenjekte ederek değiştirerek sunucuya zarar vermeyi amaçlamaktadır.");
            c.sendMessage("§4§lBu hatayı neden alıyorsunuz?");
            c.sendMessage("§cBir uygulama, bir dosya veya bir eklenti, orijinal jar dosyasını");
            c.sendMessage("§czorla değiştirdi ve enjekte etti. Bu konuda %100 emin olamayız");
            c.sendMessage("§cancak dikkatli olmakta fayda var.");
            c.sendMessage("§cLütfen en acil şekilde eklentilerinizi online trojan tarama");
            c.sendMessage("§cprogramıyla taratın. Tüm eklentiler yeşil ise sorun yoktur.");
            c.sendMessage("§c=======================");
        } else {
            c.sendMessage("§c=======================");
            c.sendMessage("§c§lTROJAN THREAT DETECTED.");
            c.sendMessage("§cA trojan virus has likely been detected on your server.");
            c.sendMessage("§cTrojan viruses aim to inject and modify .jar files on your server.");
            c.sendMessage("§4§lWhy are you seeing this?");
            c.sendMessage("§cAn application, file, or plugin has forcibly modified and injected");
            c.sendMessage("§cthe original jar file. We cannot be 100% certain, but be cautious.");
            c.sendMessage("§cPlease immediately scan your plugins using an online trojan scanner.");
            c.sendMessage("§cIf all plugins are green, you can ignore this message.");
            c.sendMessage("§c=======================");
        }
    }
}
