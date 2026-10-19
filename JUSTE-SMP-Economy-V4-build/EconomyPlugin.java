package fr.justesmp.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class EconomyPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private Connection db;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<Material, Price> prices = new LinkedHashMap<>();
    private final Set<UUID> sellMenus = new HashSet<>();
    private static final String SHOP_TITLE = "§8JUSTE-SMP • Shop";
    private static final String SELL_TITLE = "§8JUSTE-SMP • Sell";

    @Override public void onEnable() {
        saveDefaultConfig();
        try { openDb(); loadData(); } catch (Exception e) { getLogger().severe("SQLite: " + e.getMessage()); getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getPluginManager().registerEvents(this, this);
        for (String c : List.of("solde","pay","baltop","shop","sell","change")) Objects.requireNonNull(getCommand(c)).setExecutor(this);
        getServer().getScheduler().runTaskTimer(this, this::saveAll, 20L*60L, 20L*60L);
        getLogger().info("JUSTE-SMP Economy 4.0.0 active.");
    }
    @Override public void onDisable() { saveAll(); try { if (db != null) db.close(); } catch (SQLException ignored) {} }

    private void openDb() throws SQLException {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) throw new SQLException("Impossible de créer le dossier plugin");
        db = DriverManager.getConnection("jdbc:sqlite:" + new File(getDataFolder(), "economy.db"));
        try (Statement s=db.createStatement()) {
            s.executeUpdate("PRAGMA journal_mode=WAL"); s.executeUpdate("PRAGMA synchronous=NORMAL");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS balances(uuid TEXT PRIMARY KEY, name TEXT, balance REAL NOT NULL DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS prices(material TEXT PRIMARY KEY, buy REAL NOT NULL, sell REAL NOT NULL)");
        }
    }
    private void loadData() throws SQLException {
        try (PreparedStatement p=db.prepareStatement("SELECT uuid,name,balance FROM balances"); ResultSet r=p.executeQuery()) {
            while(r.next()) balances.put(UUID.fromString(r.getString(1)), r.getDouble(3));
        }
        try (PreparedStatement p=db.prepareStatement("SELECT material,buy,sell FROM prices"); ResultSet r=p.executeQuery()) {
            while(r.next()) { try { prices.put(Material.valueOf(r.getString(1)), new Price(r.getDouble(2),r.getDouble(3))); } catch(IllegalArgumentException ignored) {} }
        }
        if (prices.isEmpty()) seedPrices();
    }
    private void seedPrices() {
        add(Material.COAL,150); add(Material.COPPER_INGOT,250); add(Material.REDSTONE,200); add(Material.LAPIS_LAZULI,200);
        add(Material.IRON_INGOT,750); add(Material.GOLD_INGOT,1500); add(Material.DIAMOND,10000); add(Material.EMERALD,7500);
        add(Material.NETHERITE_INGOT,100000); add(Material.NETHERITE_SCRAP,50000); add(Material.ENDER_PEARL,25000);
        add(Material.BLAZE_ROD,30000); add(Material.TOTEM_OF_UNDYING,150000); add(Material.ELYTRA,250000);
        add(Material.ANCIENT_DEBRIS,50000); add(Material.HEART_OF_THE_SEA,75000); add(Material.SHULKER_SHELL,100000);
        try { savePrices(); } catch(SQLException e) { getLogger().warning("Prix init: "+e.getMessage()); }
    }
    private void add(Material m,double buy){ prices.put(m,new Price(buy,buy*0.5)); }
    private void saveAll() { try { saveBalances(); savePrices(); } catch(SQLException e) { getLogger().warning("Sauvegarde: "+e.getMessage()); } }
    private void saveBalances() throws SQLException {
        try (PreparedStatement p=db.prepareStatement("INSERT INTO balances(uuid,name,balance) VALUES(?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,balance=excluded.balance")) {
            for(var e:balances.entrySet()) { OfflinePlayer op=Bukkit.getOfflinePlayer(e.getKey()); p.setString(1,e.getKey().toString()); p.setString(2,op.getName()); p.setDouble(3,e.getValue()); p.addBatch(); } p.executeBatch();
        }
    }
    private void savePrices() throws SQLException {
        try(PreparedStatement p=db.prepareStatement("INSERT INTO prices(material,buy,sell) VALUES(?,?,?) ON CONFLICT(material) DO UPDATE SET buy=excluded.buy,sell=excluded.sell")) {
            for(var e:prices.entrySet()){p.setString(1,e.getKey().name());p.setDouble(2,e.getValue().buy);p.setDouble(3,e.getValue().sell);p.addBatch();} p.executeBatch();
        }
    }
    private double bal(UUID id){return balances.getOrDefault(id,0d);}
    private void setBal(UUID id,double v){balances.put(id,v);}
    private String money(double v){return String.format(Locale.US,"$%,.2f",v);}

    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if(c.getName().equals("solde")){ if(!(s instanceof Player p)){s.sendMessage("Joueur uniquement.");return true;} s.sendMessage("§aSolde: §f"+money(bal(p.getUniqueId()))); return true; }
        if(c.getName().equals("pay")){ if(!(s instanceof Player p)||a.length<2)return false; Player t=Bukkit.getPlayerExact(a[0]); if(t==null){s.sendMessage("§cJoueur introuvable.");return true;} double n=parse(a[1]); if(n<=0||bal(p.getUniqueId())<n){s.sendMessage("§cMontant invalide ou solde insuffisant.");return true;} setBal(p.getUniqueId(),bal(p.getUniqueId())-n);setBal(t.getUniqueId(),bal(t.getUniqueId())+n);s.sendMessage("§aTu as envoyé "+money(n)+" à "+t.getName()+".");t.sendMessage("§aTu as reçu "+money(n)+" de "+p.getName()+".");return true;}
        if(c.getName().equals("baltop")){ List<Map.Entry<UUID,Double>> l=new ArrayList<>(balances.entrySet());l.sort(Map.Entry.<UUID,Double>comparingByValue().reversed());s.sendMessage("§6§l--- BALTOP ---");int i=1;for(var e:l){if(i>10)break;OfflinePlayer op=Bukkit.getOfflinePlayer(e.getKey());s.sendMessage("§e"+i+". §f"+(op.getName()==null?e.getKey():op.getName())+" §7» §a"+money(e.getValue()));i++;}return true; }
        if(c.getName().equals("shop")){if(s instanceof Player p) openShop(p); else s.sendMessage("Joueur uniquement.");return true;}
        if(c.getName().equals("sell")){if(s instanceof Player p) openSell(p); else s.sendMessage("Joueur uniquement.");return true;}
        if(c.getName().equals("change")){return change(s,a);}
        return false;
    }
    private boolean change(CommandSender s,String[] a){
        if(!s.hasPermission("justesmp.admin")){s.sendMessage("§cPas de permission.");return true;}
        if(a.length<4||!a[0].equalsIgnoreCase("prix")){s.sendMessage("§c/change prix <shop|sell> <MATERIAL> <prix>");return true;}
        Material m;try{m=Material.valueOf(a[2].toUpperCase(Locale.ROOT));}catch(Exception e){s.sendMessage("§cMaterial invalide.");return true;}double n=parse(a[3]);if(n<0){s.sendMessage("§cPrix invalide.");return true;}Price old=prices.getOrDefault(m,new Price(0,0)); if(a[1].equalsIgnoreCase("shop")) prices.put(m,new Price(n,old.sell)); else if(a[1].equalsIgnoreCase("sell")) prices.put(m,new Price(old.buy,n)); else {s.sendMessage("§cshop ou sell.");return true;}try{savePrices();}catch(SQLException e){s.sendMessage("§cErreur SQLite.");}s.sendMessage("§aPrix remplacé pour "+m.name()+".");return true;
    }
    private double parse(String x){try{return Double.parseDouble(x.replace(",","."));}catch(Exception e){return -1;}}

    private void openShop(Player p){Inventory inv=Bukkit.createInventory(null,54,SHOP_TITLE);int slot=0;List<Map.Entry<Material,Price>> list=new ArrayList<>(prices.entrySet());list.sort(Comparator.comparingDouble(e->e.getValue().buy));for(var e:list){if(slot>=45)break;ItemStack it=new ItemStack(e.getKey());ItemMeta im=it.getItemMeta();im.setDisplayName("§f"+pretty(e.getKey()));im.setLore(List.of("§aAchat: §f"+money(e.getValue().buy),"§cVente: §f"+money(e.getValue().sell),"§7Clic pour acheter"));it.setItemMeta(im);inv.setItem(slot++,it);}p.openInventory(inv);}
    private void openSell(Player p){Inventory inv=Bukkit.createInventory(null,54,SELL_TITLE);sellMenus.add(p.getUniqueId());p.openInventory(inv);}
    @EventHandler public void click(InventoryClickEvent e){ if(!(e.getWhoClicked() instanceof Player p))return;String title=e.getView().getTitle();if(title.equals(SHOP_TITLE)){e.setCancelled(true);ItemStack it=e.getCurrentItem();if(it==null||it.getType().isAir())return;Price pr=prices.get(it.getType());if(pr==null)return;buy(p,it.getType(),pr.buy,1);}}
    @EventHandler public void close(InventoryCloseEvent e){if(!(e.getPlayer() instanceof Player p))return;if(!e.getView().getTitle().equals(SELL_TITLE))return;sellMenus.remove(p.getUniqueId());double total=0;for(ItemStack it:e.getInventory().getContents()){if(it==null||it.getType().isAir())continue;Price pr=prices.get(it.getType());if(pr==null){p.getWorld().dropItemNaturally(p.getLocation(),it);continue;}total+=pr.sell*it.getAmount();}if(total>0){setBal(p.getUniqueId(),bal(p.getUniqueId())+total);p.sendMessage("§aVente: §f"+money(total));}}
    private void buy(Player p,Material m,double price,int qty){double total=price*qty;if(bal(p.getUniqueId())<total){p.sendMessage("§cSolde insuffisant.");return;}setBal(p.getUniqueId(),bal(p.getUniqueId())-total);p.getInventory().addItem(new ItemStack(m,qty));p.sendMessage("§aAchat: §f"+qty+"x "+pretty(m)+" §7pour §a"+money(total));}
    private String pretty(Material m){String[] a=m.name().toLowerCase(Locale.ROOT).split("_");StringBuilder b=new StringBuilder();for(String x:a)b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1)).append(' ');return b.toString().trim();}
    private record Price(double buy,double sell){}
}
