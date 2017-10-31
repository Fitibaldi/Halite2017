package nn;

import hlt.Entity;
import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;

import java.util.Map;

public class NNParametersHalite {
    private int shipId;
    private float myRang;
    private int freePlanets;
    private int myPlanets;
    private double distanceToMyFreePlanet;
    private double distanceToEnemyPlanet;
    private double distanceToFreePlanet;
    private double distanceEnemyShip;

    public NNParametersHalite() {
        super();
    }

    public void calcNNValues(Ship ship, GameMap gameMap, int myPlayerId) {
        shipId = ship.getId();

        int[] playersRang = new int[4];
        for (Ship mapShip : gameMap.getAllShips()) {
            playersRang[mapShip.getOwner()]++;
        }

        for (Planet mapPlanet : gameMap.getAllPlanets().values()) {
            if (!mapPlanet.isOwned()) {
                freePlanets++;
            } else {
                if (mapPlanet.getOwner() == myPlayerId) {
                    myPlanets++;
                }

                playersRang[mapPlanet.getOwner()] += mapPlanet.getDockedShips().size() * 2;
            }
        }

        myRang = (playersRang[0] * 1.0f) / (playersRang[0] + playersRang[1] + playersRang[2] + playersRang[3]);

        boolean myFreePlanetFound = false;
        boolean enemyPlanetFound = false;
        boolean freePlanetFound = false;
        boolean enemyShipFound = false;
        Map<Double, Entity> nearestObjects = gameMap.nearbyEntitiesByDistance(ship);
        for (Map.Entry<Double, Entity> obj : nearestObjects.entrySet()) {
            if (obj.getValue() instanceof Planet) {
                Planet planet = (Planet) obj.getValue();
                if (!freePlanetFound && !planet.isOwned()) {
                    distanceToFreePlanet = ship.getDistanceTo(planet);
                    freePlanetFound = true;
                }

                if (!myFreePlanetFound && planet.getOwner() == myPlayerId) {
                    distanceToMyFreePlanet = ship.getDistanceTo(planet);
                    myFreePlanetFound = true;
                }

                if (!enemyPlanetFound && planet.isOwned() && planet.getOwner() != myPlayerId) {
                    distanceToEnemyPlanet = ship.getDistanceTo(planet);
                    enemyPlanetFound = true;
                }
            } else if (obj.getValue() instanceof Ship) {
                Ship mapShip = (Ship) obj.getValue();
                if (!enemyShipFound && mapShip.getOwner() != myPlayerId) {
                    distanceEnemyShip = ship.getDistanceTo(mapShip);
                    enemyShipFound = true;
                }
            }

            if (myFreePlanetFound && enemyPlanetFound && myFreePlanetFound && enemyShipFound) {
                break;
            }
        }
    }

    @Override
    public String toString() {
        return myRang + "," + freePlanets + "," + myPlanets + "," + distanceToMyFreePlanet + "," +
               distanceToEnemyPlanet + "," + distanceToFreePlanet + "," + distanceEnemyShip + "\n";
    }

    public float getMyRang() {
        return myRang;
    }

    public int getShipId() {
        return shipId;
    }
}
