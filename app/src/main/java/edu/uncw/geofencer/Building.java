package edu.uncw.geofencer;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class Building {

    @Id
    private long id;

    private String name;
    private double lat;
    private double lon;
    private float radius;

    public Building(long id, String name, double lat, double lon, float radius) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
    }

    public Building(String name, double lat, double lon, float radius) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
