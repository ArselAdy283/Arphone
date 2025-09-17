package org.arphone;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Arphone extends JavaPlugin {

    private final Map<UUID, String> nomorHp = new HashMap<>();
    private final Map<UUID, Map<String, UUID>> kontak = new HashMap<>();
    private final Map<UUID, List<String>> inbox = new HashMap<>();
    private final Random random = new Random();

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        createDataFile();
        loadData();
        getLogger().info("Arphone enabled!");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("Arphone disabled & data saved!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cHanya player yang bisa pakai command ini.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "belinomer" -> beliNomer(p);
            case "tambahkontak" -> {
                if (cekNomor(p)) addKontak(p, Arrays.asList(args));
            }
            case "sms" -> {
                if (cekNomor(p)) kirimSMS(p, Arrays.asList(args));
            }
            case "kontak" -> {
                if (cekNomor(p)) lihatKontak(p);
            }
            case "hapuskontak" -> {
                if (cekNomor(p)) hapusKontak(p, Arrays.asList(args));
            }
            case "editkontak" -> {
                if (cekNomor(p)) gantiNamaKontak(p, Arrays.asList(args));
            }
            case "ubahnomer" -> {
                if (p.hasPermission("arphone.admin")) {
                    ubahNomer(p, args);
                } else {
                    p.sendMessage("§cKamu tidak punya izin untuk command ini.");
                }
            }
            case "nomerku" -> lihatNomorSendiri(p);
            case "inbox" -> {
                if (cekNomor(p)) lihatInbox(p);
            }
            case "clearinbox" -> {
                if (cekNomor(p)) clearInbox(p);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // ================== CEK NOMOR ==================
    private boolean cekNomor(Player p) {
        if (!nomorHp.containsKey(p.getUniqueId())) {
            p.sendMessage("§cKamu belum punya nomor HP. Gunakan §e/belinomer §cuntuk membeli dulu.");
            return false;
        }
        return true;
    }

    // ================== FITUR ==================
    private void beliNomer(Player p) {
        if (nomorHp.containsKey(p.getUniqueId())) {
            p.sendMessage("§cKamu sudah punya nomor HP: §b" + nomorHp.get(p.getUniqueId()));
            return;
        }

        String nomor;
        do {
            nomor = "08" + (1000 + random.nextInt(9000));
        } while (nomorHp.containsValue(nomor)); // pastikan unik

        nomorHp.put(p.getUniqueId(), nomor);
        p.sendMessage("§aNomor HP barumu: §b" + nomor);
    }

    private void addKontak(Player p, List<String> args) {
        if (args.size() < 2) {
            p.sendMessage("§cUsage: /tambahkontak <nama> <nomor>");
            return;
        }
        String nama = args.get(0);
        String nomor = args.get(1);

        UUID target = null;
        for (Map.Entry<UUID, String> entry : nomorHp.entrySet()) {
            if (entry.getValue().equals(nomor)) {
                target = entry.getKey();
                break;
            }
        }
        if (target == null) {
            p.sendMessage("§cNomor tidak ditemukan!");
            return;
        }

        kontak.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(nama, target);
        p.sendMessage("§aKontak §b" + nama + "§a berhasil ditambahkan.");
    }

    private void kirimSMS(Player p, List<String> args) {
        if (args.size() < 2) {
            p.sendMessage("§cUsage: /sms <nama> <pesan>");
            return;
        }

        String nama = args.get(0);
        String pesan = String.join(" ", args.subList(1, args.size()));

        Map<String, UUID> bukuKontak = kontak.get(p.getUniqueId());
        if (bukuKontak == null || !bukuKontak.containsKey(nama)) {
            p.sendMessage("§cKontak tidak ditemukan!");
            return;
        }

        UUID targetId = bukuKontak.get(nama);
        Player target = getServer().getPlayer(targetId);
        if (target == null) {
            p.sendMessage("§cPlayer tidak online.");
            return;
        }

        // Simpan ke inbox target
        inbox.computeIfAbsent(targetId, k -> new ArrayList<>())
                .add("§7Dari §b" + p.getName() + "§7: " + pesan);

        // Kirim notifikasi saja
        target.sendMessage("§aSMS dari §b" + p.getName() + "§a: §7" + pesan);
        p.sendMessage("§aSMS terkirim ke §b" + nama);

        // Auto save nomor ke kontak target kalau belum ada
        Map<String, UUID> kontakTarget = kontak.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>());
        boolean sudahAda = kontakTarget.containsValue(p.getUniqueId());

        if (!sudahAda) {
            String autoNama = "*" + p.getName();
            kontakTarget.put(autoNama, p.getUniqueId());
            target.sendMessage("§eNomor baru otomatis tersimpan sebagai §b" + autoNama);
        }
    }

    private void lihatKontak(Player p) {
        Map<String, UUID> bukuKontak = kontak.get(p.getUniqueId());
        if (bukuKontak == null || bukuKontak.isEmpty()) {
            p.sendMessage("§eKontakmu masih kosong.");
            return;
        }
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("§aDaftar kontakmu:");
        for (Map.Entry<String, UUID> entry : bukuKontak.entrySet()) {
            String nomor = nomorHp.get(entry.getValue());
            p.sendMessage("- " + entry.getKey() + ": " + nomor);
            p.sendMessage("");
            p.sendMessage("");
            p.sendMessage("§7ketik /editkontak untuk mengubah nama kontak");
            p.sendMessage("§7ketik /hapuskontak untuk menhapus kontak");
        }
    }

    private void hapusKontak(Player p, List<String> args) {
        if (args.isEmpty()) {
            p.sendMessage("§cUsage: /hapuskontak <nama>");
            return;
        }
        String nama = args.get(0);
        Map<String, UUID> bukuKontak = kontak.get(p.getUniqueId());
        if (bukuKontak != null && bukuKontak.remove(nama) != null) {
            p.sendMessage("§aKontak §b" + nama + "§a berhasil dihapus.");
        } else {
            p.sendMessage("§cKontak tidak ditemukan.");
        }
    }

    private void gantiNamaKontak(Player p, List<String> args) {
        if (args.size() < 2) {
            p.sendMessage("§cUsage: /editkontak <nama lama> <nama baru>");
            return;
        }
        String namaLama = args.get(0);
        String namaBaru = args.get(1);

        Map<String, UUID> bukuKontak = kontak.get(p.getUniqueId());
        if (bukuKontak == null || !bukuKontak.containsKey(namaLama)) {
            p.sendMessage("§cKontak tidak ditemukan.");
            return;
        }

        UUID nomor = bukuKontak.remove(namaLama);
        bukuKontak.put(namaBaru, nomor);
        p.sendMessage("§aKontak §b" + namaLama + "§a berhasil diganti menjadi §b" + namaBaru);
    }

    private void ubahNomer(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ubahnomer <player> <nomorBaru>");
            return;
        }

        String targetName = args[0];
        String nomorBaru = args[1];

        Player target = getServer().getPlayerExact(targetName);
        UUID targetId;

        if (target != null) {
            targetId = target.getUniqueId();
        } else {
            targetId = null;
            for (Map.Entry<UUID, String> entry : nomorHp.entrySet()) {
                if (getServer().getOfflinePlayer(entry.getKey()).getName().equalsIgnoreCase(targetName)) {
                    targetId = entry.getKey();
                    break;
                }
            }
        }

        if (targetId == null) {
            sender.sendMessage("§cPlayer tidak ditemukan: " + targetName);
            return;
        }

        nomorHp.put(targetId, nomorBaru);
        sender.sendMessage("§aNomor HP player §b" + targetName + " §a berhasil diubah ke: §b" + nomorBaru);

        if (target != null) {
            target.sendMessage("§eNomor HP-mu telah diubah admin menjadi: §b" + nomorBaru);
        }
    }

    private void lihatNomorSendiri(Player p) {
        String nomor = nomorHp.get(p.getUniqueId());
        if (nomor == null) {
            p.sendMessage("§eKamu belum punya nomor HP. Gunakan /belinomer untuk membeli.");
            return;
        }
        p.sendMessage("§aNomor HP-mu: §b" + nomor);
    }

    private void lihatInbox(Player p) {
        List<String> pesanList = inbox.get(p.getUniqueId());
        if (pesanList == null || pesanList.isEmpty()) {
            p.sendMessage("§eInbox kamu masih kosong.");
            return;
        }
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("§a=== 📩 Inbox SMS ===");
        for (String pesan : pesanList) {
            p.sendMessage(pesan);
        }
        p.sendMessage("");
        p.sendMessage("");
        p.sendMessage("§7ketik /clearinbox untuk menhapus chat di inbox");
    }

    private void clearInbox(Player p) {
        inbox.put(p.getUniqueId(), new ArrayList<>());
        p.sendMessage("§aInbox berhasil dikosongkan.");
    }

    // ================== SAVE & LOAD DATA ==================
    private void createDataFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        dataConfig.set("nomorHp", null);
        for (Map.Entry<UUID, String> entry : nomorHp.entrySet()) {
            dataConfig.set("nomorHp." + entry.getKey().toString(), entry.getValue());
        }

        dataConfig.set("kontak", null);
        for (Map.Entry<UUID, Map<String, UUID>> entry : kontak.entrySet()) {
            String playerId = entry.getKey().toString();
            for (Map.Entry<String, UUID> kontakEntry : entry.getValue().entrySet()) {
                dataConfig.set("kontak." + playerId + "." + kontakEntry.getKey(), kontakEntry.getValue().toString());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        if (dataConfig.isConfigurationSection("nomorHp")) {
            for (String uuidStr : dataConfig.getConfigurationSection("nomorHp").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String nomor = dataConfig.getString("nomorHp." + uuidStr);
                nomorHp.put(uuid, nomor);
            }
        }

        if (dataConfig.isConfigurationSection("kontak")) {
            for (String uuidStr : dataConfig.getConfigurationSection("kontak").getKeys(false)) {
                UUID ownerId = UUID.fromString(uuidStr);
                Map<String, UUID> bukuKontak = new HashMap<>();
                for (String nama : dataConfig.getConfigurationSection("kontak." + uuidStr).getKeys(false)) {
                    String targetStr = dataConfig.getString("kontak." + uuidStr + "." + nama);
                    if (targetStr != null) {
                        bukuKontak.put(nama, UUID.fromString(targetStr));
                    }
                }
                kontak.put(ownerId, bukuKontak);
            }
        }
    }
}
