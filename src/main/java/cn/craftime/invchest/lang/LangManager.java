package cn.craftime.invchest.lang;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;
import java.util.Map;

public class LangManager {
    private final Plugin plugin;
    private String mode;
    private String baseCode;
    private FileConfiguration baseLang;

    public LangManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void init(String setting) {
        this.mode = setting == null ? "auto_en_us" : setting;
        if (this.mode.startsWith("auto_")) {
            this.baseCode = this.mode.substring("auto_".length()).toLowerCase(Locale.ROOT);
        } else {
            this.baseCode = this.mode.toLowerCase(Locale.ROOT);
        }
        ensureDefaults();
        this.baseLang = loadLangFile(this.baseCode);
        if (this.baseLang == null) {
            this.baseCode = "en_us";
            this.baseLang = loadLangFile(this.baseCode);
        }
    }

    private void ensureDefaults() {
        File dir = new File(plugin.getDataFolder(), "lang");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        copyIfMissing("lang/en_us.yml");
        copyIfMissing("lang/zh_cn.yml");
        copyIfMissing("lang/zh_tw.yml");
        copyIfMissing("lang/zh_hk.yml");
        copyIfMissing("lang/en_gb.yml");
        copyIfMissing("lang/en_au.yml");
        copyIfMissing("lang/ja_jp.yml");
        copyIfMissing("lang/ko_kr.yml");
        copyIfMissing("lang/ru_ru.yml");
        copyIfMissing("lang/kk_kz.yml");
        copyIfMissing("lang/mn_mn.yml");
        copyIfMissing("lang/de_de.yml");
        copyIfMissing("lang/fr_fr.yml");
        copyIfMissing("lang/it_it.yml");
        copyIfMissing("lang/es_es.yml");
        copyIfMissing("lang/pt_pt.yml");
        copyIfMissing("lang/ar_sa.yml");
    }

    private void copyIfMissing(String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (!target.exists()) {
            plugin.saveResource(path, false);
        }
    }

    private FileConfiguration loadLangFile(String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    public String get(Player player, String key, Map<String, String> args) {
        FileConfiguration lang = baseLang;
        if (mode.startsWith("auto_") && player != null) {
            String lc = player.getLocale();
            if (lc != null) {
                lc = lc.toLowerCase(Locale.ROOT);
                FileConfiguration pLang = loadLangFile(lc);
                if (pLang != null) {
                    lang = pLang;
                }
            }
        }
        String v = lang.getString(key, key);
        if (v == null) v = key;
        if (args != null) {
            for (Map.Entry<String, String> e : args.entrySet()) {
                v = v.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return v;
    }
}
