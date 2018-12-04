package edu.uncw.geofencer;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class App extends Application {

    private BoxStore boxStore;

    @Override
    public void onCreate() {
        super.onCreate();

        boxStore = MyObjectBox.builder().androidContext(App.this).build();
        Box<Building> buildingBox = boxStore.boxFor(Building.class);

        buildingBox.removeAll();
        List<Building> buildings = new ArrayList<>();
        buildings.add(new Building("Osprey Hall",34.2256950,-77.9709900, 50.0f));
        buildings.add(new Building("CIS Building",34.2241863,-77.8717940, 50.0f));
        buildingBox.put(buildings);
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }
}
