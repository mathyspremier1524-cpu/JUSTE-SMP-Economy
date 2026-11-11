package fr.justesmp.worldswitch;
import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
public final class WorldSwitch extends JavaPlugin {
 private YamlConfiguration points;
 public void onEnable(){getDataFolder().mkdirs();points=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"points.yml"));getLogger().info("WorldSwitch V2 enabled");}
 private void save(){try{points.save(new File(getDataFolder(),"points.yml"));}catch(IOException e){getLogger().severe(e.getMessage());}}
 public boolean onCommand(CommandSender s,Command c,String label,String[] a){
  String n=c.getName().toLowerCase();
  if(!(s instanceof Player p)){s.sendMessage("Commande utilisable en jeu uniquement.");return true;}
  if(n.equals("setlobby")||n.equals("setsurvie")){
   if(!p.hasPermission("justesmp.worldswitch.set")){p.sendMessage("§cPermission refusée.");return true;}
   String key=n.equals("setlobby")?"lobby":"survie", expected=n.equals("setlobby")?"Lobby":"Survie";
   if(!p.getWorld().getName().equalsIgnoreCase(expected)){p.sendMessage("§cTu dois être dans le monde "+expected+".");return true;}
   Location l=p.getLocation();points.set(key+".world",l.getWorld().getName());points.set(key+".x",l.getX());points.set(key+".y",l.getY());points.set(key+".z",l.getZ());points.set(key+".yaw",l.getYaw());points.set(key+".pitch",l.getPitch());save();p.sendMessage("§aPoint "+key+" enregistré !");return true;
  }
  if(n.equals("lobby")||n.equals("survie")){
   String key=n, worldName=points.getString(key+".world");
   if(worldName==null){p.sendMessage("§cPoint "+key+" non défini. Demande à un admin.");return true;}
   World w=Bukkit.getWorld(worldName);if(w==null){p.sendMessage("§cMonde "+worldName+" non chargé.");return true;}
   Location l=new Location(w,points.getDouble(key+".x"),points.getDouble(key+".y"),points.getDouble(key+".z"),(float)points.getDouble(key+".yaw"),(float)points.getDouble(key+".pitch"));
   p.teleportAsync(l);p.sendMessage("§aTéléportation...");return true;
  }
  return false;
 }
}