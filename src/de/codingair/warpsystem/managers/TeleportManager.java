package de.codingair.warpsystem.managers;

import de.CodingAir.v1_6.CodingAPI.Particles.Animations.CircleAnimation;
import de.CodingAir.v1_6.CodingAPI.Particles.Particle;
import de.CodingAir.v1_6.CodingAPI.Server.Sound;
import de.codingair.warpsystem.WarpSystem;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class TeleportManager {
    private List<Particle> particles = new ArrayList<>();
    private List<Teleport> teleports = new ArrayList<>();
    private boolean canMove = false;
    private int seconds = 5;
    private int particleId = 0;
    private double radius = 1.5;

    public TeleportManager() {
        particles.add(Particle.FIREWORKS_SPARK);
        particles.add(Particle.SUSPENDED_DEPTH);
        particles.add(Particle.CRIT);
        particles.add(Particle.CRIT_MAGIC);
        particles.add(Particle.SMOKE_NORMAL);
        particles.add(Particle.SMOKE_LARGE);
        particles.add(Particle.SPELL);
        particles.add(Particle.SPELL_INSTANT);
        particles.add(Particle.SPELL_MOB);
        particles.add(Particle.SPELL_WITCH);
        particles.add(Particle.DRIP_WATER);
        particles.add(Particle.DRIP_LAVA);
        particles.add(Particle.VILLAGER_ANGRY);
        particles.add(Particle.VILLAGER_HAPPY);
        particles.add(Particle.TOWN_AURA);
        particles.add(Particle.NOTE);
        particles.add(Particle.ENCHANTMENT_TABLE);
        particles.add(Particle.FLAME);
        particles.add(Particle.CLOUD);
        particles.add(Particle.REDSTONE);
        particles.add(Particle.SNOW_SHOVEL);
        particles.add(Particle.HEART);
        particles.add(Particle.DRAGON_BREATH);
        particles.add(Particle.END_ROD);
        particles.add(Particle.DAMAGE_INDICATOR);
    }

    public void load() {
        this.particleId = WarpSystem.getInstance().getFileManager().getFile("Config").getConfig().getInt("WarpSystem.Teleport.Animation", 17);
        this.seconds = WarpSystem.getInstance().getFileManager().getFile("Config").getConfig().getInt("WarpSystem.Teleport.Delay", 5);
    }

    public void save() {
        FileConfiguration config = WarpSystem.getInstance().getFileManager().getFile("Config").getConfig();

        config.set("WarpSystem.Teleport.Animation", this.particleId);
        config.set("WarpSystem.Teleport.Delay", this.seconds);
    }

    public void teleport(Player player, Location to) {
        Teleport teleport = new Teleport(player, to);
        if(seconds == 0 || player.hasPermission(WarpSystem.PERMISSION_ByPass_Teleport_Delay)) teleport.teleport();
        else teleport.start();
    }

    public void cancelTeleport(Player p) {
        if(!isTeleporting(p)) return;

        Teleport teleport = getTeleport(p);
        teleport.cancel(true);
        this.teleports.remove(teleport);
    }

    public Teleport getTeleport(Player p) {
        for(Teleport teleport : teleports) {
            if(teleport.getPlayer().getName().equalsIgnoreCase(p.getName())) return teleport;
        }

        return null;
    }

    public boolean isTeleporting(Player p) {
        return getTeleport(p) != null;
    }

    public boolean isCanMove() {
        return canMove;
    }

    public void setCanMove(boolean canMove) {
        this.canMove = canMove;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getParticleId() {
        return particleId;
    }

    public void setParticleId(int particleId) {
        this.particleId = particleId;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public List<Teleport> getTeleports() {
        return teleports;
    }

    public class Teleport {
        private Player player;
        private Location to;
        private CircleAnimation animation;
        private BukkitRunnable runnable;
        private Sound finishSound = Sound.ENDERMAN_TELEPORT;
        private Sound cancelSound = Sound.ITEM_BREAK;

        public Teleport(Player player, Location to) {
            this.player = player;
            this.to = to;
            this.animation = new CircleAnimation(particles.get(particleId), player, WarpSystem.getInstance(), radius);
            this.runnable = new BukkitRunnable() {
                private int left = seconds;

                @Override
                public void run() {
                    if(left == 0) {
                        teleport();
                        return;
                    }

                    left--;
                    player.playSound(player.getLocation(), Sound.NOTE_PIANO.bukkitSound(), 1.5F, 0.5F);
                }
            };
        }

        public void start() {
            //TODO: Send teleport message
            this.animation.setRunning(true);
            this.runnable.runTaskTimer(WarpSystem.getInstance(), 0L, 20L);
        }

        public void cancel(boolean sound) {
            if(animation.isRunning()) {
                this.animation.setRunning(false);
                this.runnable.cancel();
            }
            if(sound) cancelSound.playSound(player);
        }

        public void teleport() {
            cancel(false);
            player.teleport(to);
            finishSound.playSound(player);
        }

        public Player getPlayer() {
            return player;
        }

        public Location getTo() {
            return to;
        }

        public CircleAnimation getAnimation() {
            return animation;
        }

        public BukkitRunnable getRunnable() {
            return runnable;
        }

        public Sound getFinishSound() {
            return finishSound;
        }

        public void setFinishSound(Sound finishSound) {
            this.finishSound = finishSound;
        }

        public Sound getCancelSound() {
            return cancelSound;
        }

        public void setCancelSound(Sound cancelSound) {
            this.cancelSound = cancelSound;
        }
    }
}