package de.codingair.warpsystem;

import de.codingair.codingapi.API;
import de.codingair.codingapi.files.FileManager;
import de.codingair.codingapi.server.Version;
import de.codingair.codingapi.time.Timer;
import de.codingair.warpsystem.commands.CPortal;
import de.codingair.warpsystem.commands.CWarp;
import de.codingair.warpsystem.commands.CWarpSystem;
import de.codingair.warpsystem.commands.CWarps;
import de.codingair.warpsystem.language.Lang;
import de.codingair.warpsystem.listeners.NotifyListener;
import de.codingair.warpsystem.listeners.PortalListener;
import de.codingair.warpsystem.listeners.TeleportListener;
import de.codingair.warpsystem.managers.IconManager;
import de.codingair.warpsystem.managers.TeleportManager;
import de.codingair.warpsystem.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.logging.Level;

public class WarpSystem extends JavaPlugin {
    public static final String PERMISSION_NOTIFY = "WarpSystem.Notify";
    public static final String PERMISSION_MODIFY = "WarpSystem.Modify";
    public static final String PERMISSION_ByPass_Maintenance = "WarpSystem.ByPass.Maintenance";
    public static final String PERMISSION_ByPass_Teleport_Delay = "WarpSystem.ByPass.Teleport.Delay";
    public static boolean OP_CAN_SKIP_DELAY = false;

    private static WarpSystem instance;
    public static boolean activated = false;
    public static boolean maintenance = false;

    private boolean onBungeeCord;

    private IconManager iconManager = new IconManager();
    private TeleportManager teleportManager = new TeleportManager();
    private FileManager fileManager = new FileManager(this);

    private UpdateChecker updateChecker = new UpdateChecker("https://www.spigotmc.org/resources/warpsystem-gui.29595/history");
    private Timer timer = new Timer();

    private static boolean updateAvailable = false;
    private boolean old = false;
    private boolean ERROR = true;

    @Override
    public void onEnable() {
        try {
            checkOldDirectory();

            instance = this;
            API.getInstance().onEnable(this);

            timer.start();

            updateAvailable = WarpSystem.this.updateChecker.needsUpdate();

            log(" ");
            log("__________________________________________________________");
            log(" ");
            log("                       WarpSystem [" + getDescription().getVersion() + "]");
            if(updateAvailable) {
                log(" ");
                log("New update available [v" + updateChecker.getVersion() + " - " + WarpSystem.this.updateChecker.getUpdateInfo() + "].");
                log("Download it on\n\n" + updateChecker.getDownload() + "\n");
            }
            log(" ");
            log("Status:");
            log(" ");
            log("MC-Version: " + Version.getVersion().getVersionName());
            log(" ");

            log("Loading files.");
            this.fileManager.loadFile("ActionIcons", "/Memory/");
            this.fileManager.loadFile("Teleporters", "/Memory/");
            this.fileManager.loadFile("Language", "/");
            this.fileManager.loadFile("Config", "/");

            log("Loading icons.");
            this.iconManager.load(true);
            log("Loading TeleportManager.");
            this.teleportManager.load();

            maintenance = fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Maintenance", false);
            OP_CAN_SKIP_DELAY = fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Teleport.Op_Can_Skip_Delay", false);

            Bukkit.getPluginManager().registerEvents(new TeleportListener(), this);
            Bukkit.getPluginManager().registerEvents(new NotifyListener(), this);
            Bukkit.getPluginManager().registerEvents(new PortalListener(), this);

            if(fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Functions.Warps", true)) {
                new CWarp().register(this);
                new CWarps().register(this);
            }

            new CWarpSystem().register(this);

            if(fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Functions.Portals", true)) {
                new CPortal().register(this);
            }

            this.startAutoSaver();

            timer.stop();

            log(" ");
            log("Done (" + timer.getLastStoppedTime() + "s)");
            log(" ");
            log("__________________________________________________________");
            log(" ");

            activated = true;
            notifyPlayers(null);

            this.ERROR = false;
        } catch(Exception ex) {
            //make error-report

            if(!getDataFolder().exists()) {
                try {
                    getDataFolder().createNewFile();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }

            BufferedWriter writer = null;
            try {
                File log = new File(getDataFolder(), "ErrorReport.txt");
                if(log.exists()) log.delete();

                writer = new BufferedWriter(new FileWriter(log));

                PrintWriter printWriter = new PrintWriter(writer);
                ex.printStackTrace(printWriter);
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch(Exception ignored) {
                }
            }


            log(" ");
            log("__________________________________________________________");
            log(" ");
            log("                       WarpSystem [" + getDescription().getVersion() + "]");
            log(" ");
            log("       COULD NOT ENABLE CORRECTLY!!");
            log(" ");
            log("       Please contact the author with the ErrorReport.txt");
            log("       file in the plugins/WarpSystem folder.");
            log(" ");
            log(" ");
            log("       Thanks for supporting!");
            log(" ");
            log("__________________________________________________________");
            log(" ");

            this.ERROR = true;
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        API.getInstance().onDisable(this);
        save(false);
    }

    private void startAutoSaver() {
        WarpSystem.log("Starting AutoSaver.");
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(WarpSystem.getInstance(), () -> save(true), 20 * 60 * 20, 20 * 60 * 20);
    }

    private void save(boolean saver) {
        try {
            if(!saver) {
                timer.start();

                log(" ");
                log("__________________________________________________________");
                log(" ");
                if(saver)
                    log("           AutoSaver - WarpSystem [" + getDescription().getVersion() + "]");
                else
                    log("                       WarpSystem [" + getDescription().getVersion() + "]");
                if(updateAvailable) {
                    log(" ");
                    log("New update available [v" + updateChecker.getVersion() + " - " + WarpSystem.this.updateChecker.getUpdateInfo() + "]. Download it on \n\n" + updateChecker.getDownload() + "\n");
                }
                log(" ");
                log("Status:");
                log(" ");
                log("MC-Version: " + Version.getVersion().name());
                log(" ");

                if(!this.ERROR) log("Saving icons.");
                else {
                    log("Does not save data, because of errors at enabling this plugin.");
                    log(" ");
                    log("Please submit the ErrorReport.txt file to CodingAir.");
                }
            }

            if(!this.ERROR) {
                iconManager.save(true);
                if(!saver) log("Saving options.");
                fileManager.getFile("Config").getConfig().set("WarpSystem.Maintenance", maintenance);
                fileManager.getFile("Config").getConfig().set("WarpSystem.Teleport.Op_Can_Skip_Delay", OP_CAN_SKIP_DELAY);
                teleportManager.save();
            }

            if(!saver) {
                timer.stop();

                log(" ");
                log("Done (" + timer.getLastStoppedTime() + "s)");
                log(" ");
                log("__________________________________________________________");
                log(" ");
            }
        } catch(Exception ex) {
            getLogger().log(Level.SEVERE, "Error at saving data! Exception: \n\n" + ex.toString() + "\n");
        }
    }

    private void checkOldDirectory() {
        File file = getDataFolder();

        if(file.exists()) {
            File warps = new File(file, "Memory/Warps.yml");

            if(warps.exists()) {
                old = true;
                renameUnnecessaryFiles();
            }
        }
    }

    private void renameUnnecessaryFiles() {
        File file = getDataFolder();

        new File(file, "Config.yml").renameTo(new File(file, "OldConfig_Update_2.0.yml"));
        new File(file, "Language.yml").renameTo(new File(file, "OldLanguage_Update_2.0.yml"));
    }

    public void notifyPlayers(Player player) {
        if(player == null) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                notifyPlayers(p);
            }
        } else {
            if(player.hasPermission(WarpSystem.PERMISSION_NOTIFY) && WarpSystem.updateAvailable) {
                player.sendMessage("");
                player.sendMessage("");
                player.sendMessage(Lang.getPrefix() + "§aA new update is available §8[§bv" + WarpSystem.getInstance().updateChecker.getVersion() + "§8 - §b" + WarpSystem.getInstance().updateChecker.getUpdateInfo() + "§8]§a. Download it on §b§nhttps://www.spigotmc.org/resources/warpsystem-gui.29595/history");
                player.sendMessage("");
                player.sendMessage("");
            }
        }
    }

    public static WarpSystem getInstance() {
        return instance;
    }

    public IconManager getIconManager() {
        return iconManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public boolean isOnBungeeCord() {
        return onBungeeCord;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public boolean isOld() {
        return old;
    }
}
