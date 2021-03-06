package net.krinsoft.teleportsuite;

import net.krinsoft.teleportsuite.destinations.TPDestination;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author krinsdeath
 */
public class TeleportPlayer {
    public enum Status {
        ACCEPTING,
        REJECTING,
        AUTO;
        
        public static Status fromName(String name) {
            if (name == null) { return ACCEPTING; }
            for (Status value : values()) {
                if (value.getName().equals(name.toLowerCase())) {
                    return value;
                }
            }
            return ACCEPTING;
        }
        
        public String getName() {
            return name().toLowerCase();
        }
    }

    private TeleportSuite plugin;
    private Player ref;
    private List<TPDestination> stack = new ArrayList<TPDestination>();
    private List<Request> requests = new ArrayList<Request>();
    private String language;
    private String request;
    private boolean requesting;
    private boolean silent;
    private Status status;
    
    public TeleportPlayer(TeleportSuite plugin, String name) {
        this.plugin = plugin;
        this.ref = plugin.getServer().getPlayer(name);
        this.language = plugin.getUsers().getString(name + ".language");
        if (this.language == null) {
            this.language = plugin.getConfig().getString("languages.default");
        }
        String tmp = plugin.getUsers().getString(name + ".status");
        if (tmp == null) {
            tmp = plugin.getLocalization(language).get("status.toggle.default", name);
        }
        this.status = Status.fromName(tmp);
        this.stack.clear();
    }
    
    public Player getReference() {
        return ref;
    }
    
    public boolean hasPermission(String node) {
        return ref.hasPermission(node);
    }
    
    public String getName() {
        return ref.getName();
    }
    
    public TPDestination pushToStack(Location loc) {
        TPDestination dest = new TPDestination(plugin, loc);
        stack.add(0, dest);
        if (stack.size() > 5) {
            stack = stack.subList(0, 4);
        }
        return dest;
    }

    /**
     * Rewinds the player to his last location and returns the destination
     * @return the player's new location 
     */
    public TPDestination rewind() {
        sendLocalizedString("teleport.tpback", getName());
        if (stack.isEmpty()) { return null; }
        ref.teleport(stack.get(0).getLocation());
        return stack.remove(0);
    }

    /**
     * Rewinds the player the specified number of teleports
     * @param places The number of locations to rewind the player, a maximum of 5
     * @return A list of locations that were rewinded
     */
    public List<TPDestination> rewind(int places) {
        if (places > 5) { places = 5; }
        if (stack.isEmpty()) { return stack; }
        if (places > stack.size()) { places = stack.size(); }
        List<TPDestination> list = new ArrayList<TPDestination>();
        for (int i = 0; i < places; i++) {
            if (stack.isEmpty()) { break; }
            list.add(stack.remove(0));
        }
        sendLocalizedString("teleport.tprewind", String.valueOf(places));
        ref.teleport(list.get(list.size() - 1).getLocation());
        return list;
    }

    /**
     * Teleports the player representing this object to the location specified
     * @param location The location to send the player
     * @return A TPDestination object representing the player's previous location
     */
    public TPDestination teleport(Location location) {
        TPDestination last = pushToStack(ref.getLocation());
        ref.teleport(location);
        return last;
    }
    
    public void sendMessage(String message) {
        if (message == null) { return; }
        ref.sendMessage(message);
    }
    
    public void sendLocalizedString(String key, String token) {
        String msg = plugin.getLocalization(language).get(key, token);
        sendMessage(msg);
    }

    public boolean isSilent() {
        return silent;
    }

    public void toggleSilence() {
        silent = !silent;
        if (!silent) {
            sendLocalizedString("status.silent.disabled", getName());
        } else {
            sendLocalizedString("status.silent.enabled", getName());
        }
    }

    public boolean isRequesting() {
        return requesting;
    }

    public void setRequesting(String name, boolean val) {
        request = name;
        requesting = val;
    }
    
    public String getActive() {
        return this.request;
    }
    
    public Status getStatus() {
        return this.status;
    }
    
    public void setStatus(Status s) {
        sendLocalizedString("status.toggle." + s.getName(), this.getName());
        this.status = s;
    }
    
    public Location getLocation() {
        return ref.getLocation();
    }
    
    public List<Request> getRequests() {
        return requests;
    }
    
    public Request getRequest(String name) {
        for (Request r : requests) {
            if (r.getTo().getName().equals(name)) {
                return r;
            }
        }
        return null;
    }
    
    public void cancelRequest() {
        if (request != null) {
            TeleportPlayer p = plugin.getManager().getPlayer(request);
            Request r = p.getRequest(this.getName());
            if (r != null) {
                p.getRequests().remove(r);
                p.sendLocalizedString("error.requests.canceled", this.getName());
            }
            sendLocalizedString("teleport.tpcancel", request);
            request = null;
            requesting = false;
        }
    }
    
}
