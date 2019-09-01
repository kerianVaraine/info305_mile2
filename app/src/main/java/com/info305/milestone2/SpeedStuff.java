package com.info305.milestone2;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class SpeedStuff {
    private Location oldLocation;
    private Location newLocation;

    LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            if(oldLocation == null && newLocation ==null) {

                oldLocation.setLatitude(location.getLatitude());
                oldLocation.setLongitude(location.getLongitude());
                newLocation.setLatitude(location.getLatitude());
                newLocation.setLongitude(location.getLongitude());
            } else {
                oldLocation = newLocation;
                newLocation.setLatitude(location.getLatitude());
                newLocation.setLongitude(location.getLongitude());
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    public static double getSpeed(Location currentLocation, Location oldLocation)
    {
        double newLat = currentLocation.getLatitude();
        double newLon = currentLocation.getLongitude();

        double oldLat = oldLocation.getLatitude();
        double oldLon = oldLocation.getLongitude();

        if(currentLocation.hasSpeed()){
            return currentLocation.getSpeed();
        } else {
            double radius = 6371000;
            double dLat = Math.toRadians(newLat-oldLat);
            double dLon = Math.toRadians(newLon-oldLon);
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(Math.toRadians(newLat)) * Math.cos(Math.toRadians(oldLat)) *
                            Math.sin(dLon/2) * Math.sin(dLon/2);
            double c = 2 * Math.asin(Math.sqrt(a));
            double distance =  Math.round(radius * c);

            double timeDifferent = currentLocation.getTime() - oldLocation.getTime();
            return distance/timeDifferent;
        }
    }

}
