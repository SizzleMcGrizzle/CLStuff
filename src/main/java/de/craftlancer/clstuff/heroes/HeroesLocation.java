package de.craftlancer.clstuff.heroes;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeroesLocation implements ConfigurationSerializable {
    //baltop, clantop, or player score top
    private String category;
    //1, 2, or 3, the ranking for the category
    private String ranking;
    private List<Location> signLocations;
    //Location of either a head or a banner
    private List<Location> displayLocations;
    
    public HeroesLocation(String category, String ranking) {
        this.category = category;
        this.ranking = ranking;
        this.signLocations = new ArrayList<>();
        this.displayLocations = new ArrayList<>();
    }
    
    public HeroesLocation(Map<String, Object> map) {
        this.category = (String) map.get("category");
        this.ranking = (String) map.get("ranking");
        this.signLocations = (List<Location>) map.get("signLocations");
        this.displayLocations = (List<Location>) map.get("displayLocations");
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("category", category);
        map.put("ranking", ranking);
        map.put("signLocations", signLocations);
        map.put("displayLocations", displayLocations);
        
        return map;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getRanking() {
        return ranking;
    }
    
    public List<Location> getDisplayLocations() {
        return displayLocations;
    }
    
    public List<Location> getSignLocations() {
        return signLocations;
    }
    
    public void setDisplayLocations(List<Location> displayLocations) {
        this.displayLocations = displayLocations;
    }
    
    public void setSignLocations(List<Location> signLocations) {
        this.signLocations = signLocations;
    }
    
    public void addSignLocation(Location location) {
        signLocations.add(location);
    }
    
    public void addDisplayLocation(Location location) {
        displayLocations.add(location);
    }
    
    public void removeDisplayLocation(Location location) {
        displayLocations.remove(location);
    }
    
    public void removeSignLocation(Location location) {
        signLocations.remove(location);
    }
}
